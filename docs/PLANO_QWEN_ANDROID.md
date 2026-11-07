# Arquitetura de IA local do Puppy — Qwen

> Documento de acompanhamento: distingue o que já existe no repositório, o que foi decidido e o que ainda será implementado. Uma decisão ou plano não significa que o recurso já esteja funcionando.

## Estado atual do projeto

### Já implementado no repositório

- **Interface do Puppy:** tema escuro tecnológico, cabeçalho, indicador de cérebro local, painel do mascote, campo de mensagem e controles de interação. Commit de referência: `5d66767c77dbfcb7e538102cf2b3daa7f4d465dc`.
- **Manifesto Android reforçado:** backup desativado e tráfego HTTP em texto claro desativado; a permissão de Internet foi mantida para os recursos de consulta. Commit: `47a32de84e9e20c0bab9b0ea3ae9c397f5e9ae7b`.
- **Validação das entradas das Skills:** limite de 200 caracteres, validação de comandos e limites para cidade/assunto, tratamento de entrada nula e mensagens de erro genéricas. Commit: `01f62416758452a5c3e29c6b39e0c018726764d8`.
- **Consultas externas restringidas:** HTTPS, hosts permitidos, sem portas ou credenciais arbitrárias, redirecionamentos bloqueados, timeouts, limite de resposta e limite de itens RSS. Commit: `0356052751edb04473ee3e54158da17d87701b1e`.
- **Parser XML endurecido:** processamento de declarações de documento externo desativado. Commit: `59fcaf89bebcd07a3540de60a4a6f6c9869fcab8`.
- **Plano de arquitetura Qwen/MNN documentado:** commit inicial do documento: `8391d55d3981143bc37c75031943e5c02a8b5cd3`.

### Funcionamento atual conhecido

- Aplicativo Android nativo em Java 17.
- Android Gradle Plugin 8.7.3, compileSdk 34, minSdk 26 e targetSdk 34.
- A tela envia o texto para as Skills em uma thread de trabalho.
- As Skills atuais são baseadas em regras e oferecem recursos como clima, notícias, Wikipédia e diário; ainda não são um chatbot generativo.
- A classe `Pet` e a rede neural comportamental `Brain` cuidam do comportamento/animação do mascote. Essa rede não é um LLM.
- A integração do Qwen e do MNN **ainda não foi implementada**.
- **Validação manual recente:** o usuário informou que atualizou/instalou o APK atual no Galaxy A01 e que ficou tudo certo. Isso confirma a validação prática relatada para a versão atual, mas não substitui testes automatizados ou medições de desempenho do futuro LLM.
- **Pesquisa de integração MNN-LLM:** o código oficial Android foi inspecionado em nível de README, Gradle, CMake, JNI e wrapper Kotlin. O exemplo oficial atual fixa ABI `arm64-v8a`; o MNN também mantém um script genérico `build_32.sh` para `armeabi-v7a`, mas ainda não foi confirmado que a configuração LLM completa compile e funcione nessa ABI.
- **Compatibilidade real informada pelo usuário (Termux, Galaxy A01):** `ro.product.cpu.abilist=armeabi-v7a,armeabi`, `ro.product.cpu.abilist32=armeabi-v7a,armeabi`, `ro.product.cpu.abilist64` sem valor informado, `ro.zygote=zygote32`, `ro.product.cpu.abi=armeabi-v7a` e `ro.build.version.sdk=32` (Android 12). A evidência disponível caracteriza este sistema instalado como ambiente de 32 bits; não presumir que ele consiga carregar bibliotecas `arm64-v8a`.

## Decisão técnica

- Modelo principal planejado: família Qwen3.
- Execução pretendida: inteiramente local no Android, sem API proprietária obrigatória.
- Runtime inicial a avaliar: MNN-LLM (Apache-2.0), que possui suporte a Qwen3 e integração Android.
- Runtime alternativo: llama.cpp, caso os testes de compatibilidade, desempenho, tamanho ou manutenção indiquem vantagem.
- **Requisito obrigatório de arquitetura:** o Puppy deve continuar instalável e funcional em Android ARM de 32 bits (`armeabi-v7a`). Também deve oferecer suporte a ARM 64 bits (`arm64-v8a`) em aparelhos compatíveis. A implementação nativa será compilada separadamente para cada ABI; nunca incluir uma `.so` de 64 bits no caminho de execução de um processo de 32 bits.
- A ausência ou falha do runtime/modelo em qualquer ABI não pode impedir a inicialização do app nem remover as Skills atuais. Em 32 bits, se a inferência local não for tecnicamente viável, o Puppy permanece funcional sem LLM.
- O modelo deve ser baixado sob demanda; não embutir pesos grandes no APK.
- O primeiro candidato para avaliação é Qwen3-0.6B, sujeito à verificação da licença e do formato/quantização exatos disponíveis para o runtime escolhido.

## Pesquisa técnica inicial do MNN-LLM

Consulta à documentação oficial do repositório MNN, especialmente o README do aplicativo Android MnnLlmChat:

- A integração Android oficial constrói bibliotecas nativas com Android NDK; não é correto presumir que basta adicionar uma dependência Java/Gradle.
- O README oficial consultado indica NDK `27.2.12479018` para o aplicativo de exemplo e mostra a compilação da biblioteca MNN antes da construção do app.
- O exemplo oficial MNN Chat consultado fixa `abiFilters "arm64-v8a"` e seu CMake aponta para uma biblioteca MNN compilada em `project/android/build_64`. O repositório MNN possui script de build `build_32.sh` e rotinas de build para ARMv7, mas isso não comprova sozinho que todos os componentes MNN-LLM, JNI e kernels necessários estejam prontos e validados em 32 bits. A ABI `armeabi-v7a` permanece um gate de aprovação obrigatório.
- O release MNN `3.6.2` publica um pacote Android com bibliotecas `armv7` e `armv8`, mas o nome do pacote não comprova que todos os componentes MNN-LLM/JNI usados pelo app estejam incluídos. Não tratar esse pacote genérico como runtime LLM sem inspecionar seus artefatos.
- A documentação oficial alerta que o aplicativo MNN Chat foi testado em aparelhos topo de linha (OnePlus 13 e Xiaomi 14 Ultra) e que aparelhos modestos podem ficar lentos, instáveis ou não conseguir carregar modelos.
- O README registra suporte a Qwen3 no aplicativo MNN Chat a partir da versão 0.4.0. Isso comprova suporte da aplicação/ecossistema, mas ainda não comprova que um checkpoint específico Qwen3-0.6B em formato específico funciona no aparelho do usuário.
- O wrapper oficial observado é Kotlin (`LlmSession.kt`) com métodos `external` JNI, enquanto o Puppy é Java. A integração pode ser feita via Java/JNI ou um pequeno módulo Kotlin, mas deve usar contratos e bibliotecas nativas compatíveis; não é uma dependência Java simples.
- A última release consultada do MNN foi `3.6.2` (29/10/2026), mas ainda não foi escolhida como dependência do Puppy. Precisamos validar os artefatos LLM para a ABI do aparelho antes de fixar a versão.
- Nenhuma API Java foi assumida ou implementada nesta etapa.

Referências oficiais consultadas:
- MNN Android LLM app: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/README.md
- MNN Android build workflow / NDK: https://github.com/alibaba/MNN/blob/master/.github/workflows/mnn_release.yml
- MNN build 32-bit genérico: https://github.com/alibaba/MNN/blob/master/project/android/build_32.sh
- MNN build 64-bit genérico: https://github.com/alibaba/MNN/blob/master/project/android/build_64.sh
- MNN release 3.6.2: https://github.com/alibaba/MNN/releases/tag/3.6.2
- MNN engine Android notes: https://github.com/alibaba/MNN/wiki/engine
- Relatório de compatibilidade do aparelho: `docs/VALIDACAO_MNN_GALAXY_A01.md`

## Requisito de compatibilidade 32/64 bits

O requisito do produto foi confirmado pelo usuário: **o Puppy deve funcionar em 32 bits e também oferecer uma build para 64 bits**. O Galaxy A01 é o aparelho de referência obrigatório para o caminho 32-bit. Os valores enviados pelo usuário no Termux foram:

- `ro.product.cpu.abilist`: `armeabi-v7a,armeabi`
- `ro.product.cpu.abilist32`: `armeabi-v7a,armeabi`
- `ro.product.cpu.abilist64`: vazio/não informado
- `ro.zygote`: `zygote32`
- `ro.product.cpu.abi`: `armeabi-v7a`
- `ro.build.version.sdk`: `32` (Android 12)

A implementação deve manter o núcleo Java independente de ABI e compilar qualquer biblioteca JNI separadamente para `armeabi-v7a` e `arm64-v8a`. O app deve escolher/carregar apenas a biblioteca compatível com o processo instalado. Preferir APKs separados por ABI ou App Bundle/splits para reduzir tamanho; um APK universal só será usado se todos os artefatos nativos de ambas as ABIs estiverem presentes e forem compatíveis. Não forçar `arm64-v8a` como ABI única.

O NDK do Android documenta suporte a `armeabi-v7a` e `arm64-v8a`, e o CMake recebe uma ABI por configuração de build. Isso viabiliza builds separadas do app, mas **não garante** que cada runtime de LLM tenha suporte funcional em ambas. (Referência: https://developer.android.com/ndk/guides/abis)

## Restrição do aparelho de desenvolvimento

O aparelho de referência é um Samsung Galaxy A01 (Android 12, ARM de 32 bits no sistema instalado, hardware de entrada). A documentação do aplicativo Android do MNN alerta que seus testes foram feitos em aparelhos topo de linha e que dispositivos modestos podem apresentar lentidão, instabilidade ou falha ao carregar modelos.

Portanto, não presumir que qualquer Qwen3 rode no A01. Começar com o menor checkpoint Qwen3 disponível que tenha licença explicitamente compatível, preferir quantização suportada pelo runtime e medir memória, tempo de carregamento, velocidade de geração, temperatura e estabilidade. Se o modelo não couber ou ficar inutilizável em 32 bits, manter o Puppy funcional sem LLM nessa ABI; a versão 64 bits poderá oferecer inferência local quando o dispositivo tiver recursos suficientes. Não prometer desempenho antes de testar no aparelho real.

## Arquitetura planejada

1. **AssistantEngine**: interface independente do fornecedor/runtime para enviar contexto e receber texto/ações estruturadas.
2. **RuntimeSelector**: detecta a ABI do processo e seleciona somente uma implementação nativa compatível; se nenhuma estiver disponível, retorna modo sem LLM sem interromper o app.
3. **QwenMnnEngine**: implementação inicial que integra Qwen3 via MNN-LLM. Deve poder ser substituída sem reescrever a interface, memória ou ferramentas.
4. **AssistantOrchestrator**: organiza contexto, histórico, instruções, chamadas de ferramentas e validação dos resultados.
5. **ToolRegistry**: catálogo explícito de ferramentas disponíveis (clima, notícias, Wikipédia, calculadora, arquivos e ações Android autorizadas).
6. **MemoryStore**: memória local, com controles de visualização, limpeza e retenção.
7. **SafetyGate**: valida cada ação antes de executá-la. O modelo nunca recebe execução direta de shell, acesso irrestrito à rede ou permissão para alterar permissões do Android.

## Regras de segurança

- O modelo apenas propõe uma ferramenta e argumentos em formato estruturado; o aplicativo valida nome, tipos, tamanho e limites.
- Ferramentas são allowlist: somente ações implementadas e autorizadas pelo usuário.
- Ações sensíveis (excluir, compartilhar, enviar, instalar, alterar configurações) exigem confirmação explícita.
- Consultas de rede continuam limitadas a destinos e operações explicitamente autorizados; não criar navegador ou cliente HTTP arbitrário a partir de texto gerado pelo modelo.
- Saídas do modelo são dados não confiáveis: nunca executar código, comandos ou links automaticamente.
- A memória é local e não deve armazenar senhas, tokens ou segredos.
- A rede neural comportamental atual do pet permanece separada do LLM: ela controla personalidade/animação, não permissões nem ferramentas.
- Não prometer segurança absoluta: cada nova ferramenta e integração precisa de revisão e testes próprios.

## Próximas mudanças planejadas

### Etapa 1 — Base e compatibilidade

- [x] Inspecionar README, Gradle, CMake, JNI e wrapper Kotlin do exemplo oficial MNN Chat.
- [x] Identificar os requisitos documentados do exemplo: NDK `27.2.12479018`, CMake `3.22.1`, build nativa e ABI `arm64-v8a` no Gradle atual.
- [x] Identificar a existência de um script genérico MNN para `armeabi-v7a`; a build LLM nessa ABI continua sem validação.
- [x] Obter do aparelho a lista real de ABIs e propriedades Zygote pelo Termux. Resultado: sistema do Galaxy A01 opera como `zygote32`, ABI primária `armeabi-v7a`, sem ABI 64-bit anunciada.
- [ ] Manter o build Java do Puppy compatível com 32 e 64 bits; não introduzir filtro nativo que exclua `armeabi-v7a`.
- [ ] Fixar uma versão/release do MNN somente depois de confirmar artefatos LLM para cada ABI suportada.
- [ ] Executar e validar a build oficial `MNN_BUILD_LLM=ON` para `armeabi-v7a` e `arm64-v8a` separadamente, incluindo inspeção dos artefatos gerados. A build 32-bit é obrigatória para o requisito do produto; a 64-bit não pode substituir o suporte 32-bit.
- [ ] Verificar a licença do checkpoint exato e a compatibilidade de formato/quantização com a versão do MNN.
- [ ] Fazer protótipos mínimos isolados para as ABIs 32 e 64 bits, carregando o modelo e medindo memória/desempenho antes de alterar o fluxo principal do chat.
- [ ] Testar no Galaxy A01 que o APK 32-bit instala, abre, conversa pelas Skills e continua utilizável sem bibliotecas/modelo LLM.

### Etapa 2 — Integração do motor local

- [ ] Criar a interface `AssistantEngine` e contratos de mensagens/respostas.
- [ ] Implementar `QwenMnnEngine` somente depois de confirmar as APIs reais do runtime.
- [ ] Permitir baixar, verificar, remover e atualizar o modelo sob demanda, com origem e integridade verificáveis.
- [ ] Exibir estado do modelo: não instalado, baixando, pronto, carregando ou indisponível.
- [ ] Executar inferência fora da thread de UI, com cancelamento e limites de tokens/tempo.
- [ ] Manter as Skills atuais como fallback se o modelo não estiver disponível ou falhar.

### Etapa 3 — Ferramentas e agentes

- [ ] Criar `ToolRegistry` com contratos tipados e allowlist.
- [ ] Converter as Skills existentes em ferramentas sem perder suas validações.
- [ ] Permitir que Qwen proponha chamadas de ferramenta, mas executar apenas após validação local.
- [ ] Implementar planejamento em etapas e verificação de resultados.
- [ ] Adicionar ações Android gradualmente, com permissões mínimas e confirmação para operações sensíveis.

### Etapa 4 — Memória e autonomia limitada

- [ ] Criar `MemoryStore` local, com opções para consultar e apagar dados.
- [ ] Implementar histórico resumido e contexto por tarefa.
- [ ] Não guardar credenciais, tokens ou segredos.
- [ ] Exigir confirmação humana para ações sensíveis e informar claramente o que será executado.

## Critérios mínimos antes de considerar concluído

- [ ] APK 32-bit (`armeabi-v7a`) compila, instala e abre no Galaxy A01.
- [ ] APK 64-bit (`arm64-v8a`) compila e abre em um dispositivo Android 64-bit de teste.
- [ ] App abre e continua funcionando sem modelo baixado, em ambas as variantes.
- [ ] Inferência não bloqueia a interface.
- [ ] Falha de carregamento não fecha o app.
- [ ] Ferramentas rejeitam argumentos inválidos e ações não autorizadas.
- [ ] Downloads do modelo têm verificação de integridade e tratamento de falhas.
- [ ] Teste de desempenho real no Galaxy A01, sem afirmar suporte se não passar.
- [ ] Licenças do runtime, dependências e checkpoint ficam documentadas.
- [ ] Build e testes automatizados executados e resultados registrados.

## Observação sobre status

As caixas de seleção acima representam trabalho pendente. O usuário confirmou por Termux que o Galaxy A01 usa ambiente de 32 bits (`zygote32`, `armeabi-v7a`) e solicitou suporte de produto para 32 e 64 bits, com 32 bits como requisito essencial. A pesquisa documental e inspeção do código oficial estão concluídas, mas não houve build NDK/CMake, instalação de APK nativo ou inferência real. O próximo gate é provar builds nativas separadas e comportamento de fallback. Consulte `docs/VALIDACAO_MNN_GALAXY_A01.md` para o diagnóstico.
