# Validação de compatibilidade — MNN-LLM no Galaxy A01

**Status:** pesquisa documental concluída; compilação nativa, ABI real do aparelho e inferência ainda não validadas.

## Resumo executivo

O Galaxy A01 SM-A015M é um aparelho de entrada com Snapdragon 439 e cerca de 2 GB de RAM. A ficha consultada informa CPU ARMv8-A, mas uma base de catálogo de dispositivos Android indica `armeabi-v7a` como ABI primária para a família Galaxy A01. Isso não basta para determinar a ABI do processo Android em que o Puppy está rodando: é necessário ler as propriedades do aparelho ou instrumentar o próprio app.

O exemplo oficial MNN Chat para Android **não pode ser copiado diretamente para o Puppy**:
- O `app/build.gradle` atual do exemplo fixa `abiFilters "arm64-v8a"`.
- O exemplo exige Android NDK `27.2.12479018`, CMake `3.22.1`, e compila uma biblioteca JNI própria.
- O `CMakeLists.txt` do exemplo pressupõe uma árvore completa do repositório MNN e liga contra uma biblioteca MNN compilada em `project/android/build_64`.
- O projeto do Puppy é Java 17, minSdk 26 e atualmente não possui NDK/CMake nem bibliotecas nativas.

O repositório MNN possui também `project/android/build_32.sh`, que seleciona `armeabi-v7a`, mas o script genérico não demonstra por si só que a configuração completa de LLM está habilitada e testada nessa ABI. Precisamos compilar com as opções LLM adequadas, confirmar os artefatos gerados e executar inferência real.

## Evidências oficiais e referências

- Aplicativo Android MNN Chat e instruções de build: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/README.md
- Gradle do MNN Chat (NDK 27.2.12479018, CMake 3.22.1, ABI arm64-v8a): https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/app/build.gradle
- CMake/JNI do MNN Chat: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/app/src/main/cpp/CMakeLists.txt
- Wrapper Kotlin/JNI do LLM: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/app/src/main/java/com/alibaba/mnnllm/android/llm/LlmSession.kt
- Implementação JNI e sessão C++: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/app/src/main/cpp/llm_mnn_jni.cpp
- Build 32-bit genérico: https://github.com/alibaba/MNN/blob/master/project/android/build_32.sh
- Build 64-bit genérico: https://github.com/alibaba/MNN/blob/master/project/android/build_64.sh
- Último release consultado do MNN: https://github.com/alibaba/MNN/releases/tag/3.6.2
- Ficha do Galaxy A01 SM-A015M: https://www.maiscelular.com.br/fichas-tecnicas/samsung/galaxy-a01/sm-a015m/
- Catálogo de ABI do Galaxy A01: https://soft9.us/devices/samsung/a01q

As referências do aparelho são fontes secundárias e podem variar por variante/região/firmware. As propriedades do aparelho real são a fonte decisiva.

## Diagnóstico de ABI necessário

Executar no Termux no próprio telefone (ou em um terminal ADB conectado ao telefone):

```sh
getprop ro.product.cpu.abilist
getprop ro.product.cpu.abilist32
getprop ro.product.cpu.abilist64
getprop ro.zygote
getprop ro.build.version.sdk
```

Interpretação:
- Se `abilist` incluir `arm64-v8a`, o sistema suporta aplicações ARM64.
- Se o app estiver executando como processo de 32 bits, ele precisará de bibliotecas `armeabi-v7a`, mesmo que a CPU seja capaz de ARM64.
- O APK do Puppy atual não inclui bibliotecas nativas, então ainda pode instalar em dispositivos 32/64 bits.
- Não alterar `abiFilters` até conhecer a ABI de execução pretendida e termos bibliotecas compatíveis para ela.

Para diagnosticar a ABI do processo do Puppy com precisão numa futura build de diagnóstico, registrar `System.getProperty("os.arch")` e `android.os.Process.is64Bit()` (API 23+), sem enviar identificadores pessoais.

## Plano técnico de protótipo

1. Fixar uma versão/release do MNN e não depender de `master` em produção.
2. Preparar uma compilação mínima do MNN-LLM para a ABI realmente usada pelo Puppy.
3. Incluir somente o núcleo de inferência de texto necessário; não incluir áudio, visão, difusão, câmera, Firebase ou bibliotecas não usadas.
4. Implementar uma camada JNI pequena e própria, baseada nos contratos confirmados do runtime, com ciclo de vida explícito (load, generate, cancel/reset, release).
5. Fazer a inferência em executor de trabalho, nunca na UI thread.
6. Começar com um checkpoint Qwen3 pequeno e quantizado que tenha conversão MNN documentada, licença verificada e tamanho/arquivos conhecidos.
7. Medir RAM livre antes/depois do carregamento, tempo de load, tokens/s, temperatura, estabilidade e espaço em disco.
8. Só depois ligar o motor ao chat do Puppy; manter as Skills como fallback quando o modelo não estiver instalado ou não carregar.

## Critérios de aprovação no Galaxy A01

- Build de bibliotecas nativas para a ABI real do app concluído.
- APK de diagnóstico instala e inicia sem afetar o app de referência.
- Modelo carrega sem crash ou OOM.
- Geração de texto funciona por várias interações consecutivas.
- Memória e temperatura permanecem aceitáveis durante uso real.
- Fechar/reabrir a tela e liberar o modelo não deixa processo ou memória presos.
- O app permanece funcional quando o modelo não foi baixado, foi removido ou falhou ao carregar.

## Limitações atuais

Não foi possível executar compilação NDK/CMake, instalar APK, consultar propriedades ADB ou medir memória/temperatura do telefone a partir da conexão GitHub. Nenhum arquivo nativo ou dependência MNN foi adicionado ao app nesta fase. A próxima decisão depende do diagnóstico de ABI e de uma build nativa reproduzível; não declarar Qwen funcional até passar nos critérios acima.
