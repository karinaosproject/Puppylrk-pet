#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <mutex>
#include <string>
#include <vector>
#include "llama.h"

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "PuppyLlama", __VA_ARGS__)
namespace {
std::mutex g_mutex;
llama_model *g_model = nullptr;
llama_context *g_context = nullptr;
const llama_vocab *g_vocab = nullptr;

void unload_locked() {
    if (g_context) { llama_free(g_context); g_context = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    g_vocab = nullptr;
}
std::string from_java(JNIEnv *env, jstring value) {
    if (!value) return {};
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (!chars) return {};
    std::string result(chars);
    env->ReleaseStringUTFChars(value, chars);
    return result;
}
void fill_batch(llama_batch_ext *batch, const llama_token *tokens, int count, int position) {
    llama_batch_ext_clear(batch);
    for (int i = 0; i < count; ++i) {
        int index = llama_batch_ext_add_token(batch, 0, tokens[i]);
        if (index < 0) return;
        llama_pos pos = position + i;
        llama_batch_ext_set_pos(batch, index, &pos);
    }
    if (count > 0) llama_batch_ext_set_output_logits(batch, count - 1, true);
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_br_karina_pet_LlamaRuntime_nativeLoadModel(JNIEnv *env, jclass, jstring path, jint contextSize, jint threads) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unload_locked();
    std::string modelPath = from_java(env, path);
    if (modelPath.empty()) return JNI_FALSE;
    llama_backend_init();
    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    g_model = llama_model_load_from_file(modelPath.c_str(), mp);
    if (!g_model) { LOGE("Falha ao carregar GGUF"); return JNI_FALSE; }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = static_cast<uint32_t>(std::max(128, std::min(1024, (int) contextSize)));
    cp.n_batch = 128;
    cp.n_ubatch = 128;
    cp.n_threads = std::max(1, std::min(4, (int) threads));
    cp.n_threads_batch = cp.n_threads;
    g_context = llama_init_from_model(g_model, cp);
    if (!g_context) { unload_locked(); return JNI_FALSE; }
    g_vocab = llama_model_get_vocab(g_model);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_br_karina_pet_LlamaRuntime_nativeGenerate(JNIEnv *env, jclass, jstring promptValue, jint maxTokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (!g_model || !g_context || !g_vocab) return nullptr;
    std::string prompt = from_java(env, promptValue);
    if (prompt.empty()) return env->NewStringUTF("");
    int32_t needed = -llama_tokenize(g_vocab, prompt.c_str(), (int32_t) prompt.size(), nullptr, 0, true, true);
    if (needed <= 0 || needed > 512) return nullptr;
    std::vector<llama_token> tokens((size_t) needed);
    int32_t count = llama_tokenize(g_vocab, prompt.c_str(), (int32_t) prompt.size(), tokens.data(), needed, true, true);
    if (count < 0) return nullptr;
    llama_memory_clear(llama_get_memory(g_context), true);
    llama_sampler *sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (!sampler) return nullptr;
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    llama_batch_ext *batch = llama_batch_ext_init(g_context);
    if (!batch) { llama_sampler_free(sampler); return nullptr; }
    fill_batch(batch, tokens.data(), count, 0);
    bool failed = llama_process(g_context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0;
    int position = count;
    std::string output;
    int limit = std::max(1, std::min(64, (int) maxTokens));
    for (int i = 0; !failed && i < limit; ++i) {
        llama_token token = llama_sampler_sample(sampler, g_context, -1);
        if (llama_vocab_is_eog(g_vocab, token)) break;
        char piece[256];
        int size = llama_token_to_piece(g_vocab, token, piece, sizeof(piece), 0, true);
        if (size < 0) { failed = true; break; }
        output.append(piece, (size_t) size);
        fill_batch(batch, &token, 1, position++);
        failed = llama_process(g_context, LLAMA_PROCESS_TYPE_DECODE, batch) != 0;
    }
    llama_batch_ext_free(batch);
    llama_sampler_free(sampler);
    if (failed) return nullptr;
    return env->NewStringUTF(output.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_br_karina_pet_LlamaRuntime_nativeUnload(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    unload_locked();
}
