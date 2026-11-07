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
- O adaptador mantém o modelo carregado em memória após a primeira carga; a importação de um novo arquivo descarrega o modelo anterior. A chamada deve ocorrer em thread de trabalho.
- Importação do GGUF pela ação **MODELO** na interface; o arquivo é copiado para o armazenamento privado do app com nome fixo.
- A interface informa se a biblioteca nativa está disponível e se o arquivo GGUF foi encontrado. Isso ainda não comprova inferência: é necessário testar uma resposta gerada pelo modelo.
- Sem streaming, histórico de contexto ou cancelamento.
- Nunca executar comandos ou código gerado. Ferramentas futuras exigem allowlist e confirmação para ações sensíveis.

## Referências

- https://github.com/ggml-org/llama.cpp/blob/v0.9.0/docs/android.md
- https://github.com/ggml-org/llama.cpp/blob/v0.9.0/include/llama.h
- https://developer.android.com/ndk/guides/abis


## Instalar o modelo no aparelho

1. Obtenha um modelo **Qwen3 0.6B Instruct** em formato **GGUF**, quantização Q4_K_M, de uma fonte confiável.
2. No Puppy, toque em **MODELO** no cabeçalho.
3. Selecione o arquivo `.gguf` no seletor de arquivos do Android e aguarde a cópia.
4. O status deve mudar para **Qwen3 GGUF encontrado · ainda não testado**.
5. Envie uma pergunta aberta, por exemplo: "Explique em duas frases por que o céu é azul". Uma resposta não relacionada às Skills é o primeiro sinal de inferência, mas a confirmação técnica exige logs nativos e validação de saída.

O aplicativo não baixa modelos automaticamente. O arquivo é grande e a importação exige espaço livre adicional temporário. Em aparelho com 2 GB de RAM, carregamento pode falhar por falta de memória; mantenha apps em segundo plano fechados durante o primeiro teste.
