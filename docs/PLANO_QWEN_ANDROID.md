# Arquitetura de IA local do Puppy — Qwen

## Decisão técnica

- Modelo principal planejado: família Qwen3.
- Execução pretendida: inteiramente local no Android, sem API proprietária obrigatória.
- Runtime inicial a avaliar: MNN-LLM (Apache-2.0), que possui suporte a Qwen3 e integração Android.
- Runtime alternativo futuro: llama.cpp, caso os testes de compatibilidade, desempenho, tamanho ou manutenção indiquem vantagem.
- O modelo deve ser baixado sob demanda; não embutir pesos grandes no APK.

## Restrição do aparelho de desenvolvimento

O aparelho de referência é um Samsung Galaxy A01 (Android 12, ARM, hardware de entrada). A documentação do aplicativo Android do MNN alerta que seus testes foram feitos em aparelhos topo de linha e que dispositivos modestos podem apresentar lentidão, instabilidade ou falha ao carregar modelos.

Portanto, não presumir que qualquer Qwen3 rode no A01. Começar com o menor checkpoint Qwen3 disponível que tenha licença explicitamente compatível (avaliar primeiro Qwen3-0.6B), preferir quantização suportada pelo runtime e medir memória, tempo de carregamento, velocidade de geração, temperatura e estabilidade. Se o modelo não couber ou ficar inutilizável, manter o Puppy funcional sem LLM e oferecer a execução local em aparelho mais potente como opção futura. Não prometer desempenho antes de testar no aparelho real.

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

## Etapas de implementação

### Etapa 1 — Base e compatibilidade
- Criar interface do motor de linguagem e contratos de mensagens/respostas.
- Verificar a licença do checkpoint exato e a compatibilidade com MNN.
- Preparar build Android com suporte nativo apenas para ABIs realmente necessárias.
- Testar um protótipo mínimo antes de integrar ao chat.

### Etapa 2 — Qwen local
- Integrar MNN-LLM e permitir baixar/remover o modelo sob demanda.
- Exibir estado do modelo: não instalado, baixando, pronto, carregando ou indisponível.
- Executar inferência fora da thread de UI, com cancelamento e limites de tokens/tempo.
- Manter as skills atuais como fallback se o modelo não estiver disponível.

### Etapa 3 — Ferramentas e agentes
- Converter skills existentes em ferramentas com contratos tipados.
- Permitir que Qwen proponha chamadas de ferramenta, mas executar apenas após validação local.
- Implementar planejamento em etapas e verificação de resultados.

### Etapa 4 — Memória e autonomia limitada
- Adicionar memória local controlável.
- Implementar histórico resumido e contexto por tarefa.
- Exigir confirmação humana para ações sensíveis.

## Critérios mínimos antes de considerar concluído

- APK compila para as ABIs selecionadas.
- App abre e continua funcionando sem modelo baixado.
- Inferência não bloqueia a interface.
- Falha de carregamento não fecha o app.
- Ferramentas rejeitam argumentos inválidos e ações não autorizadas.
- Teste de desempenho real no Galaxy A01, sem afirmar suporte se não passar.
- Licenças do runtime, dependências e checkpoint ficam documentadas.
