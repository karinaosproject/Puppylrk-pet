package br.karina.pet;

import android.content.Context;
import java.io.File;

/** Adaptador opcional: sem biblioteca ou modelo, o app mantém o fallback Skills. */
public final class LlamaRuntime {
    private static final String MODEL_FILE = "qwen3-0.6b-q4_k_m.gguf";
    private static final int CONTEXT_SIZE = 384;
    private static final int THREADS = 2;
    private static boolean libraryLoaded;

    static {
        try {
            System.loadLibrary("puppy_llama");
            libraryLoaded = true;
        } catch (UnsatisfiedLinkError | SecurityException ignored) {
            libraryLoaded = false;
        }
    }

    private LlamaRuntime() {}

    public static synchronized String responder(Context context, String prompt) {
        if (!libraryLoaded || prompt == null || prompt.trim().isEmpty()) return null;
        File model = new File(new File(context.getFilesDir(), "models"), MODEL_FILE);
        if (!model.isFile() || model.length() < 1024 * 1024) return null;
        if (!nativeLoadModel(model.getAbsolutePath(), CONTEXT_SIZE, THREADS)) return null;
        try {
            return nativeGenerate(prompt, 64);
        } catch (OutOfMemoryError | RuntimeException error) {
            return null;
        } finally {
            nativeUnload();
        }
    }

    public static boolean isLibraryAvailable() { return libraryLoaded; }

    private static native boolean nativeLoadModel(String path, int contextSize, int threads);
    private static native String nativeGenerate(String prompt, int maxTokens);
    private static native void nativeUnload();
}
