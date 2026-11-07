# Histórico de implementação e estado técnico do Puppy

> Documento de acompanhamento da branch `feature/llama-armv7`. Atualizado em 7 de novembro de 2026.

## 1. Objetivo do projeto

Puppy é um companheiro virtual Android, com mascote animado, necessidades, emoções, personalidade, diário e habilidades locais. A evolução planejada adiciona um modelo de linguagem GGUF executado localmente, mantendo o funcionamento por regras como fallback.

Dispositivo de referência informado para os testes iniciais: Samsung Galaxy A01, Android 12, arquitetura de processo 32 bits (`armeabi-v7a`), memória RAM de 2 GB.

## 2. Funcionalidades do app base

- Interface Android nativa em Java.
- Mascote desenhado em Canvas e animado por `PetView`.
- Estados e reações do mascote geridos por `Pet`.
- Rede neural pulsada e aprendizado por recompensa em `Brain`.
- Ações de carinho, comida, brincadeira e bronca.
- Persistência de estado e preferências locais.
- Diário local.
- Skills baseadas em regras para ajuda, clima, RSS/notícias e pesquisa na Wikipédia.
- Consultas externas por HTTPS para serviços permitidos; as consultas online precisam de internet.

### Principais classes

| Arquivo | Responsabilidade |
| --- | --- |
| `MainActivity.java` | Interface, conversa, importação de modelo e ciclo de vida |
| `Pet.java` | Estado, emoções, necessidades, personalidade e persistência |
| `PetView.java` | Desenho, animação e interação visual do mascote |
| `Brain.java` | Rede neural pulsada e ajuste por recompensa |
| `Skills.java` | Interpretação de comandos e fallback por regras |
| `Conhecimento.java` | RSS, Wikipédia e armazenamento de conhecimento |
| `Clima.java` | Consulta meteorológica |
| `Diario.java` | Histórico local |

## 3. Segurança aplicada

As seguintes melhorias foram adicionadas e commitadas na história do projeto:

- Manifesto com `allowBackup=false` e tráfego cleartext desativado.
- Validação de entrada de comandos, incluindo limite de tamanho.
- Conexões de conhecimento limitadas a hosts HTTPS autorizados, sem userinfo ou portas arbitrárias.
- Requisições GET com limites de tempo e de tamanho de resposta.
- Redirecionamentos bloqueados na camada de conhecimento.
- Leitura RSS limitada a dez itens.
- Parser XML com processamento de declarações externas desativado (`FEATURE_PROCESS_DOCDECL=false`).
- A integração de IA não executa comandos nem código produzido pelo modelo. Ações futuras precisam de allowlist e confirmação explícita para ações sensíveis.

Commits de segurança identificados:
- `47a32de` — endurecimento do Manifest.
- `01f6241` — validação de comandos.
- `0356052` — restrição e endurecimento de conexões externas.
- `59fcaf8` — endurecimento do parser XML.

## 4. Integração opcional llama.cpp / Qwen3

A branch experimental integra llama.cpp v0.9.0 via CMake e JNI.

### Componentes

- `app/src/main/cpp/CMakeLists.txt`: busca upstream fixado e configura compilação nativa.
- `app/src/main/cpp/llama_jni.cpp`: funções JNI para inicializar GGUF, tokenizar, gerar texto e liberar recursos.
- `LlamaRuntime.java`: ponte Java para a biblioteca nativa; mantém o fallback quando biblioteca/modelo não estão disponíveis.
- `MainActivity.java`: tenta a resposta local primeiro; se não houver saída do modelo, usa Skills.
- `docs/LLAMA_ARMV7_TESTE.md`: instruções e plano de validação.

### Configuração inicial do protótipo

- llama.cpp upstream fixado em v0.9.0.
- Alvo nativo inicial: somente `armeabi-v7a`.
- NDK 27.2.12479018 e CMake 3.22.1 no workflow.
- CPU, sem OpenMP, Vulkan, OpenCL ou KleidiAI.
- Contexto inicial de 384 tokens.
- Duas threads.
- Até 64 tokens por resposta.
- Modelo esperado: Qwen3 0.6B Instruct GGUF Q4_K_M.
- O GGUF não é incorporado ao APK. É selecionado pelo usuário e copiado para armazenamento privado do app.

Os parâmetros são experimentais. A RAM de 2 GB pode ser insuficiente, e o desempenho pode ser baixo.

## 5. Importação e estado do modelo

A interface possui a ação **MODELO** no cabeçalho. Ela abre o seletor de documentos Android e permite escolher um arquivo GGUF.

O app copia o arquivo para `files/models/qwen3-0.6b-q4_k_m.gguf` no armazenamento privado. A cópia usa arquivo temporário, sincronização e substituição; o modelo carregado anteriormente é descarregado antes de substituir o arquivo.

Estados mostrados na interface:
- Biblioteca nativa indisponível.
- Biblioteca pronta · modelo não instalado.
- Qwen3 GGUF encontrado · ainda não testado.

**Encontrar o arquivo não é o mesmo que validar inferência.** A cópia não prova que o arquivo é um GGUF válido, que o modelo cabe na memória ou que a API nativa consegue gerar uma resposta.

## 6. Build ARMv7 confirmado

GitHub Actions run [#72](https://github.com/karinaosproject/Puppylrk-pet/actions/runs/42327960594):

- Resultado do job: sucesso.
- Compilação Gradle/NDK/CMake: sucesso.
- Verificação do APK: sucesso.
- Biblioteca encontrada: `lib/armeabi-v7a/libpuppy_llama.so`.
- Verificação de ABIs inesperadas: passou.
- Artifact: `puppy-armv7-llama-debug`.
- Artifact ID: `13555419511`.
- Tamanho reportado: 3.540.094 bytes.
- SHA-256: `dba5737874cdd24fd9ad3d99a02ddef727f306137e3cd0f9e205f73fde480f84`.

Este resultado confirma compilação e empacotamento. Não é um teste no Galaxy A01 e não confirma inferência real.

## 7. Evolução visual: glass futurista

A tela principal recebeu superfícies translúcidas simuladas e uma paleta tecnológica:

- Fundo profundo em azul-marinho/preto com gradientes.
- Painel central do mascote com camadas azuladas e violeta.
- Bordas finas em azul claro.
- Indicador do cérebro local em verde-água.
- Ciano/azul para o botão de envio e ações primárias.
- Detalhes coral, âmbar, azul e violeta nas ações do mascote.
- Campos de conversa e resposta com transparência simulada, cantos arredondados e elevação.

A imagem conceitual está em [PUPPY_INTERFACE_GLASS.svg](PUPPY_INTERFACE_GLASS.svg) e é exibida no README.

A implementação usa transparência, gradientes, contornos e elevação. Não usa blur real de vidro, pois a prioridade é compatibilidade e desempenho no aparelho de entrada.

## 8. Automação de build

Workflow: `.github/workflows/apk.yml`.

- Dispara em push e manualmente.
- Prepara Java 17.
- Instala Android NDK e CMake.
- Configura Gradle 8.13.
- Executa `gradle assembleDebug --no-daemon --stacktrace`.
- Verifica a presença da biblioteca ARMv7 no APK.
- Publica o APK como artifact.

O usuário não precisa executar Gradle localmente para obter o artifact de uma execução bem-sucedida.

## 9. Limitações e validações ainda pendentes

1. Instalar a versão glass no Galaxy A01 e verificar abertura, layout e fluidez.
2. Testar as quatro ações do mascote, diário, teclado e rolagem.
3. Testar funcionamento por Skills sem GGUF.
4. Importar um GGUF Qwen3 compatível e validar carregamento real.
5. Confirmar que a resposta vem do modelo e não do fallback de regras.
6. Medir tempo de carregamento, tokens por segundo, pico de RAM, temperatura e estabilidade.
7. Testar prompts em português, conversas repetidas e retomada após sair/voltar.
8. Avaliar redução de contexto/tokens ou modelo menor caso a memória seja insuficiente.
9. Revisar acessibilidade, contraste, tamanhos de fonte e desempenho em tela pequena.
10. Criar testes automatizados e uma build release assinada de forma segura.

## 10. Observações de segurança e distribuição

O repositório tem um arquivo histórico chamado `debug-signing.keystore.b64`. Uma chave de debug publicada não deve ser considerada secreta nem reutilizada para assinatura de produção. A distribuição release deve usar uma nova chave privada, mantida fora do Git e protegida por armazenamento seguro/secrets.

O projeto ainda precisa de uma licença explícita antes de ser descrito formalmente como open source. O modelo GGUF escolhido também deve ter licença compatível com o uso pretendido.

## 11. Histórico recente de commits

- `795e24e` — importador privado GGUF e status do modelo.
- `d76c74a` — seletor de modelo e estado na interface.
- `3e26b53` — documentação do importador e validação.
- `b8d07ea` — descarregar modelo ativo antes de substituir GGUF.
- `f3629d9` — corrigir documentação do ciclo de vida do modelo.
- `b65d239` — atualizar superfícies glass da interface.
- `fd60d08` — adicionar prévia visual SVG.
- `4ef0d1b` — documentar interface glass e integração ARMv7.
- `d337ba4` / `b5576d3` — registrar e revisar status do build e testes pendentes.

---

**Regra de comunicação do projeto:** nunca declarar o Qwen3 funcional no aparelho antes de observar uma inferência real. Build verde, biblioteca empacotada, modelo importado e inferência validada são etapas distintas.
