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

## Decisão técnica

- Modelo principal planejado: família Qwen3.
- Execução pretendida: inteiramente local no Android, sem API proprietária obrigatória.
- Runtime inicial a avaliar: MNN-LLM (Apache-2.0), que possui suporte a Qwen3 e integração Android.
- Runtime alternativo futuro: llama.cpp, caso os testes de compatibilidade, desempenho, tamanho ou manutenção indiquem vantagem.
- O modelo deve ser baixado sob demanda; não embutir pesos grandes no APK.
- O primeiro candidato para avaliação é Qwen3-0.6B, sujeito à verificação da licença e do formato/quantização exatos disponíveis para o runtime escolhido.

## Pesquisa técnica inicial do MNN-LLM

Consulta à documentação oficial do repositório MNN, especialmente o README do aplicativo Android MnnLlmChat:

- A integração Android oficial constrói bibliotecas nativas com Android NDK; não é correto presumir que basta adicionar uma dependência Java/Gradle.
- O README oficial consultado indica NDK `27.2.12479018` para o aplicativo de exemplo e mostra a compilação da biblioteca MNN antes da construção do app.
- O exemplo de compilação consultado está orientado a `arm64-v8a`; precisamos verificar separadamente o suporte e a distribuição de bibliotecas para `armeabi-v7a` antes de decidir quais ABIs o Puppy vai empacotar.
- A documentação oficial alerta que o aplicativo MNN Chat foi testado em aparelhos topo de linha (OnePlus 13 e Xiaomi 14 Ultra) e que aparelhos modestos podem ficar lentos, instáveis ou não conseguir carregar modelos.
- O README registra suporte a Qwen3 no aplicativo MNN Chat a partir da versão 0.4.0. Isso comprova suporte da aplicação/ecossistema, mas ainda não comprova que um checkpoint específico Qwen3-0.6B em formato específico funciona no aparelho do usuário.
- Ainda falta inspecionar e fixar uma versão/release do MNN e confirmar o caminho real de integração Java/JNI e o formato do modelo. Nenhuma API Java foi assumida ou implementada nesta etapa.

Referências oficiais consultadas:
- MNN Android LLM app: https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/README.md
- MNN Android build workflow / NDK: https://github.com/alibaba/MNN/blob/master/.github/workflows/mnn_release.yml
- MNN engine Android notes: https://github.com/alibaba/MNN/wiki/engine

## Restrição do aparelho de desenvolvimento

O aparelho de referência é um Samsung Galaxy A01 (Android 12, ARM, hardware de entrada). A documentação do aplicativo Android do MNN alerta que seus testes foram feitos em aparelhos topo de linha e que dispositivos modestos podem apresentar lentidão, instabilidade ou falha ao carregar modelos.

Portanto, não presumir que qualquer Qwen3 rode no A01. Começar com o menor checkpoint Qwen3 disponível que tenha licença explicitamente compatível, preferir quantização suportada pelo runtime e medir memória, tempo de carregamento, velocidade de geração, temperatura e estabilidade. Se o modelo não couber ou ficar inutilizável, manter o Puppy funcional sem LLM e oferecer a execução local em aparelho mais potente como opção futura. Não prometer desempenho antes de testar no aparelho real.

## Arquitetura planejada

1. **AssistantEngine**: interface independente do fornecedor/runtime para enviar contexto e receber texto/ações estruturadas.
2. **QwenMnnEngine**: implementação inicial que integra Qwen3 via MNN-LLM. Deve poder ser substituída sem reescrever a interface, memória ou ferramentas.
3. **AssistantOrchestrator**: organiza contexto, histórico, instruções, chamadas de ferramentas e validação dos resultados.
4. **ToolRegistry**: catálogo explícito de ferramentas disponíveis (clima, notícias, Wikipédia, calculadora, arquivos e ações Android autorizadas).
5. **MemoryStore**: memória local, com controles de visualização, limpeza e retenção.
6. **SafetyGate**: valida cada ação antes de executá-la. O modelo nunca recebe execução direta de shell, acesso irrestrito à rede ou permissão para alterar permissões do Android.

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

- [ ] Fixar uma versão/release do MNN para evitar integração instável com a branch principal em movimento.
- [ ] Inspecionar os exemplos e o código oficial do MNN-LLM para Android/Java/JNI; confirmar APIs reais antes de escrever o adaptador.
- [ ] Confirmar requisitos de NDK, CMake, bibliotecas nativas, ABIs e versão mínima do Android.
- [ ] Identificar as ABIs realmente disponíveis no Galaxy A01 e escolher a estratégia de empacotamento sem remover suporte existente sem justificativa.
- [ ] Verificar a licença do checkpoint exato e a compatibilidade de formato/quantização com a versão do MNN.
- [ ] Fazer um protótipo mínimo isolado para carregar o modelo e medir consumo de memória e desempenho no A01, antes de alterar o fluxo principal do chat.

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

- [ ] APK compila para as ABIs selecionadas.
- [ ] App abre e continua funcionando sem modelo baixado.
- [ ] Inferência não bloqueia a interface.
- [ ] Falha de carregamento não fecha o app.
- [ ] Ferramentas rejeitam argumentos inválidos e ações não autorizadas.
- [ ] Downloads do modelo têm verificação de integridade e tratamento de falhas.
- [ ] Teste de desempenho real no Galaxy A01, sem afirmar suporte se não passar.
- [ ] Licenças do runtime, dependências e checkpoint ficam documentadas.
- [ ] Build e testes automatizados executados e resultados registrados.

## Observação sobre status

As caixas de seleção acima representam trabalho pendente. Os itens da seção “Já implementado” descrevem alterações presentes no histórico do repositório, mas não substituem uma compilação e testes no aparelho. A próxima atividade técnica é fixar uma versão do MNN e construir um protótipo isolado para validar as bibliotecas nativas e o carregamento do modelo.
