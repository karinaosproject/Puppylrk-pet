# llama.cpp ARMv7 — protótipo experimental

## Objetivo e estado

Branch de protótipo para testar llama.cpp no Galaxy A01 informado pelo usuário: Android 12, processo de 32 bits, ABI armeabi-v7a.

**Ainda não validado por compilação nem em aparelho.** A documentação Android upstream mostra o exemplo NDK com arm64-v8a; o alvo ARMv7 aqui é experimental e só poderá ser declarado compatível após build e inferência reais.

## Integração

- CMake baixa a versão upstream fixada v0.9.0.
- JNI carrega GGUF, tokeniza, gera com sampler greedy e libera contexto.
- LlamaRuntime.java é adaptador opcional; se a biblioteca ou o modelo faltarem, retorna null para manter o fallback.
- O modelo não é embutido no APK. Arquivo esperado no armazenamento privado: files/models/qwen3-0.6b-q4_k_m.gguf.

## Configuração inicial

- Contexto: 384 tokens.
- Threads: 2.
- Máximo: 64 tokens por resposta.
- CPU; sem OpenMP, Vulkan, OpenCL ou KleidiAI.
- Esses valores são apenas um ponto inicial e não garantem que o modelo caiba na RAM do Galaxy A01.

## Build

O build usa Android NDK/CMake pelo Android Gradle Plugin. Esta branch deve gerar somente armeabi-v7a. O primeiro build precisa de internet para baixar o upstream.

## Validação obrigatória

1. Confirmar lib/armeabi-v7a/libpuppy_llama.so no APK.
2. Confirmar ausência de bibliotecas ELF de 64 bits.
3. Instalar no Galaxy A01 e confirmar abertura.
4. Testar sem GGUF: Skills deve continuar funcionando.
5. Testar GGUF Qwen3 compatível e prompt curto.
6. Medir carregamento, tokens/s, pico de RAM, temperatura e estabilidade.
7. Testar 10 prompts e retomada após sair/voltar.

## Limitações

- API nativa fixada em v0.9.0.
- ARMv7 ainda não foi comprovado; podem ser necessárias correções no upstream ou no NDK.
- O adaptador é experimental e recarrega/libera o modelo por chamada. A chamada deve ocorrer em thread de trabalho.
- Sem streaming, histórico de contexto, cancelamento, importação de modelos ou template específico Qwen3.
- Nunca executar comandos ou código gerado. Ferramentas futuras exigem allowlist e confirmação para ações sensíveis.

## Referências

- https://github.com/ggml-org/llama.cpp/blob/v0.9.0/docs/android.md
- https://github.com/ggml-org/llama.cpp/blob/v0.9.0/include/llama.h
- https://developer.android.com/ndk/guides/abis
