# Validação de compatibilidade — MNN-LLM no Galaxy A01

**Status:** pesquisa documental concluída; ABI de 32 bits do sistema confirmada por Termux; compilação nativa e inferência ainda não validadas.

## Resumo executivo

O Galaxy A01 SM-A015M é um aparelho de entrada com Snapdragon 439 e cerca de 2 GB de RAM. O usuário executou os comandos no Termux e informou estes resultados: `ro.product.cpu.abilist=armeabi-v7a,armeabi`; `ro.product.cpu.abilist32=armeabi-v7a,armeabi`; `ro.product.cpu.abilist64` vazio/não informado; `ro.zygote=zygote32`; `ro.product.cpu.abi=armeabi-v7a`; `ro.build.version.sdk=32` (Android 12). Portanto, para este firmware, devemos tratar o Galaxy A01 como ambiente Android de 32 bits e exigir que o Puppy continue funcionando em `armeabi-v7a`. Não contar com a instalação de bibliotecas `arm64-v8a` neste aparelho.

O exemplo oficial MNN Chat para Android **não pode ser copiado diretamente para o Puppy**:
- O `app/build.gradle` atual do exemplo fixa `abiFilters "arm64-v8a"`.
- O exemplo exige Android NDK `27.2.12479018`, CMake `3.22.1`, e compila uma biblioteca JNI própria.
- O `CMakeLists.txt` do exemplo pressupõe uma árvore completa do repositório MNN e liga contra uma biblioteca MNN compilada em `project/android/build_64`.
- O projeto do Puppy é Java 17, minSdk 26 e atualmente não possui NDK/CMake nem bibliotecas nativas.

O repositório MNN possui `project/android/build_32.sh`, e foi inspecionado também o script unificado oficial `build_lib.sh`. Esse script configura explicitamente uma build `armeabi-v7a` com `-DMNN_BUILD_LLM=ON`, `-DMNN_LOW_MEMORY=ON` e `-DMNN_ARM82=OFF`; em seguida configura uma segunda build `arm64-v8a`, também com `-DMNN_BUILD_LLM=ON`. Isso melhora substancialmente a viabilidade técnica para ambas as ABIs. Ainda é necessário executar as builds, inspecionar os `.so` gerados, compilar a camada JNI do Puppy para cada ABI e validar inferência no dispositivo.

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

Resultados recebidos do usuário:

```text
ro.product.cpu.abilist     = armeabi-v7a,armeabi
ro.product.cpu.abilist32  = armeabi-v7a,armeabi
ro.product.cpu.abilist64  = (vazio/não informado)
ro.zygote                 = zygote32
ro.product.cpu.abi        = armeabi-v7a
ro.build.version.sdk      = 32
```

Interpretação:
- O sistema instalado anuncia somente ABIs ARM de 32 bits e usa `zygote32`.
- O APK atual do Puppy não inclui bibliotecas nativas, então seu código Java permanece independente de ABI.
- A arquitetura do produto passa a exigir suporte tanto a `armeabi-v7a` quanto a `arm64-v8a`, em builds nativas separadas. A versão 32-bit é obrigatória e deve ser testada no Galaxy A01; a versão 64-bit será testada em outro dispositivo Android compatível.
- Não colocar bibliotecas 64-bit na variante 32-bit nem configurar `abiFilters` que excluam `armeabi-v7a`.

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

- Build das bibliotecas MNN-LLM para `armeabi-v7a` concluída e artefatos inspecionados.
- Build das bibliotecas MNN-LLM para `arm64-v8a` concluída e artefatos inspecionados.
- Camada JNI e APK compilados para cada ABI, sem misturar bibliotecas 32/64-bit.
- APK de diagnóstico instala e inicia sem afetar o app de referência.
- Modelo carrega sem crash ou OOM.
- Geração de texto funciona por várias interações consecutivas.
- Memória e temperatura permanecem aceitáveis durante uso real.
- Fechar/reabrir a tela e liberar o modelo não deixa processo ou memória presos.
- O app permanece funcional quando o modelo não foi baixado, foi removido ou falhou ao carregar.

## Limitações atuais

A ABI do sistema foi informada pelo usuário via Termux. A inspeção do script unificado upstream `build_lib.sh` encontrou configurações explícitas de `MNN_BUILD_LLM=ON` para `armeabi-v7a` e `arm64-v8a`, mas ainda não houve compilação NDK/CMake, instalação de APK nativo, inferência real nem medição de memória/temperatura. Nenhum arquivo nativo ou dependência MNN foi adicionado ao app nesta fase. O suporte de 32 bits é requisito obrigatório, mas ainda precisa ser provado para o runtime LLM; se a inferência não for viável no A01, o app continuará funcional em 32 bits com as Skills existentes. O suporte de 64 bits será entregue em variante própria e não substituirá o caminho de 32 bits. Não declarar Qwen funcional até passar nos critérios acima.
