<div align="center">

## Idioma

[Português](README.md) · [English](README.en.md) · [Español](README.es.md)

<img src="medcall/client/expo/assets/brand/medcall-logo.svg" width="360" alt="MedCall" />

<img src="https://github.com/GrupoTP/Sistema-de-Envio-e-Armazenamento-dos-Projetos-ADS/blob/main/Senac_logo.svg.png" width="100" alt="Logo Senac" />

# MedCall

**Sua saúde na palma da mão.**

Aplicativo de pré-triagem informativa e agendamento de consultas. Projeto Integrador do 3º período de Análise e Desenvolvimento de Sistemas da Faculdade Senac Pernambuco.

Link do Painel de ADM do Sistema e do Painel do ADM da Clínica: https://medcall.grupotp.polyclub.com.br/admin/

Antes se chamava Recife Saudável. O nome do produto é MedCall.

Todos os direitos reservados. O uso deste código é exclusivo de Polyana Fontes e Thayná Batista da Silva. O texto está em [LICENSE](LICENSE).

![Suíte](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml/badge.svg)
<!-- test-badge:start -->
![Testes](https://img.shields.io/badge/testes-143%20passando-1E6B5E)
<!-- test-badge:end -->

> A MedCall não realiza diagnóstico médico e não substitui atendimento profissional. Em emergência, ligue 192 (SAMU). O texto está no componente `ClinicalDisclaimer`.

[Sistema no ar](https://medcall.grupotp.polyclub.com.br) · [Requisitos V8](https://grupotp.youtrack.cloud/articles/SPM-A-141) · [Protótipo no Figma](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6) · [Pitch](https://www.canva.com/design/DAHb31ads88/bK-cDvsyY1i6zaBu8-vvZA/edit)

</div>

## Conta para teste

Esta conta existe no [sistema no ar](https://medcall.grupotp.polyclub.com.br). Ela não guarda dados reais de paciente.

- E-mail: `teste@medcall.grupotp.polyclub.com.br`
- Senha: `MedCall2026`

## Para que serve

O MedCall é o aplicativo do paciente. Serve para a pessoa contar, antes da consulta, o que está sentindo e para marcar um horário com um profissional. O relato fica na conta. A consulta marcada também.

O aplicativo não diz o nome de uma doença e não substitui o profissional de saúde. Em emergência, o aviso da tela pede para ligar 192 (SAMU).

## Como usar

1. Abra o [sistema no ar](https://medcall.grupotp.polyclub.com.br) no navegador, ou o aplicativo Android.
2. Crie uma conta, ou entre com a conta que já existe. Em Esqueci minha senha, o servidor manda um código para a caixa Mailpit do cluster (`MAIL_HOST=mailpit`, porta 1025, sem usuário e sem senha). O endereço de origem é `medcall@medcall.grupotp.polyclub.com.br`. Essa caixa não fica na internet. O código vale 30 minutos, só serve uma vez, e a resposta é a mesma se o e-mail não existir.
3. Na pré-triagem, descreva o que está sentindo. O rascunho pode ser retomado. Esse relato não é um diagnóstico.
4. Toque em agendar. Escolha especialidade e profissional, depois data e horário, e confirme. O servidor confere a vaga de novo.
5. Abra as consultas já marcadas. Sem internet, essa lista continua na tela. Marcar outra consulta só funciona com conexão.
6. Na conta, atualize o perfil, o consentimento de notificações, a senha, ou peça a exclusão dos dados.
7. O botão de acessibilidade (AccessAbility Widget) abre com clique ou Alt+A na web do paciente, em `/admin/` e pelo mesmo botão no Android e no iOS. A versão é a do arquivo `medcall/client/accessability-widget.version`. Trocar esse número sem atualizar a web, os portais, o Android e o iOS faz o teste do GitHub falhar. No Android e no iOS o contraste, a fonte e a leitura desse painel valem para o documento do widget. Nos portais, que são HTML, esses recursos valem para a página. A interface em Kotlin do app do paciente segue o tema do aplicativo.

## Recursos da interface

A interface é Kotlin com componentes React Native. O shell Expo em `medcall/client/expo` só registra esse componente. Não é o site antigo aberto dentro de uma WebView.

| Recurso | Onde está | Função |
|---|---|---|
| Navegação entre telas | `AppScreen` em `medcall/client/app/src/jsMain/kotlin/grupotp/medcall/app/App.kt` | Troca boas-vindas, cadastro, login, início, pré-triagem, agenda, consultas, conta e política. O estado fica em `useState`. |
| Menu | `FloatingMenu.kt` | Abre por cima da tela logada e leva a início, pré-triagem, agenda, consultas e conta. |
| ScrollView | Cadastro, agenda, consultas, conta e política | Rolagem do conteúdo. |
| TextInput | `TextField.kt` | Campos de cadastro, login e agenda. |
| Pressable | Botões e links das telas | Toque nos botões. |
| Modal | `ConfirmDialog.kt` | Confirmação antes de ações da conta. |
| Ícones | `ui/icon/Icons.kt` | Menu, consultas, conta e saída. |

## Com e sem internet

Na PWA, a interface abre de novo sem rede depois da primeira visita com o service worker. Enviar dados novos exige o servidor.

| Tela | Sem rede |
|---|---|
| Boas-vindas, login e cadastro | A tela abre. Entrar ou criar conta exige o servidor. |
| Início e minhas consultas | A lista já recebida continua. |
| Agendar e pré-triagem | Exigem rede. O app avisa e não confirma o que o servidor não gravou. |
| Conta | O perfil já salvo pode ser lido. Gravar perfil, senha ou exclusão exige rede. |

## Dados no aparelho

Na web, o IndexedDB se chama `medcall`, versão 2, com as stores `accounts` e `appointments`. No Android, os mesmos dados ficam em SQLite. A senha não é gravada no aparelho. Fonte: `LocalAccountDatabase.kt`.

| Dado | Onde fica | Quando sai |
|---|---|---|
| Perfil da conta logada | chave `signed-in` | Saída, ou quando o servidor recusa a sessão. |
| Consultas da última conta | store `appointments`, chave do id da conta | A lista nova substitui a anterior. A saída apaga a store. |
| Fila de saída feita offline | chave `pending-sign-outs` | Continua depois da limpeza e é enviada quando a rede volta. |

Não há prazo separado no aparelho. O servidor encerra a sessão depois de `SESSION_IDLE_DAYS` sem requisição. O padrão é 180 dias.

## Testes executados

<!-- test-results:start -->
Última execução em 26 de novembro de 2026, com o comando da seção [Como executar](#como-executar).

| Resultado | Quantidade |
|---|---|
| Testes executados | 143 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |

Os números saem dos relatórios XML do Gradle, nas pastas de `common`, `server`, `client/app` e `client/worker`. Os testes de `common` entram duas vezes quando rodam na JVM e no JavaScript. O workflow [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) grava esta tabela e uma linha em [TESTES.md](TESTES.md) depois de cada execução na main, com ou sem falha. Registro: https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45907767767. Commit: `0fdacd860906962e3ae89cd67e852d26ba34188c`. Quando há falha, o nome do teste fica em TESTES.md.
<!-- test-results:end -->

## O que está neste repositório

A interface é Kotlin com React Native. O mesmo código roda como PWA no navegador e como app Android. O shell Expo em `medcall/client/expo` só registra o componente Kotlin. No navegador, o Metro troca `react-native` por `react-native-web`.

O servidor é Kotlin/JVM 21, com Ktor, Koin e Exposed. Em desenvolvimento e nos testes o banco é H2. Em produção é MySQL. A publicação é GitOps: imagens no ghcr.io, Kubernetes no namespace `medcall`, ArgoCD e Traefik.

O detalhe de módulos, offline, fuso da clínica e variáveis está em [medcall/README.md](medcall/README.md). Este arquivo é a capa do repositório.

## O que o aplicativo faz hoje

- Boas-vindas, cadastro, login e troca de senha com código de uso único.
- Pré-triagem informativa, sem diagnóstico.
- Agendamento em três etapas: especialidade e profissional, data e horário, confirmação.
- Consultas já marcadas visíveis sem internet. Agendar exige conexão. O servidor confere a vaga de novo, e um índice único impede dois agendamentos no mesmo horário.
- Conta: perfil, consentimento de notificações, troca de senha e pedido de exclusão.
- Tela de política de privacidade.

A pré-análise de risco por IA, no dashboard da clínica, está especificada e ainda não está neste código. Só clínica e profissional devem ver o nível. O paciente não. A decisão é do profissional. Fonte da especificação: [SPM-A-139](https://grupotp.youtrack.cloud/articles/SPM-A-139/Pre-analise-de-risco-Feita-por-AI). As normas do Ministério da Saúde e do CFM exigem acolhimento com classificação de risco e não impõem um protocolo único. Manchester entra como referência. [Publicação do Ministério da Saúde](https://bvsms.saude.gov.br/bvs/publicacoes/acolhimento_classificaao_risco_servico_urgencia.pdf).

## API

21 rotas em `/api/v1`. As de paciente exigem `Authorization: Bearer`. O token é um JWT HS256 (`JWT_SECRET`, no mínimo 32 bytes). O servidor guarda só o id da sessão e a revoga no logout. A troca por código também encerra as sessões da conta.

| Área | Rotas |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/password-recovery`, `POST /auth/password-recovery/confirm`, `GET /auth/me`, `POST /auth/logout` |
| Conta | `GET /account`, `PUT /account`, `PUT /account/consents`, `POST /account/password`, `POST /account/deletion-request` |
| Pré-triagem | `GET /pre-triages/draft`, `GET /pre-triages/pending`, `POST /pre-triages` |
| Agenda | `GET /specialties`, `GET /professionals`, `GET /clinics`, `GET /availability`, `GET /appointments`, `POST /appointments`, `POST /appointments/{id}/cancel` |

Fora da API: `GET /healthz` (processo no ar, sem consultar o banco) e `GET /readyz` (banco respondendo).

Senha com PBKDF2-HMAC-SHA256, 600.000 iterações. Fonte do formato: [medcall/README.md](medcall/README.md).

## Portais da equipe

`/admin/` no mesmo host não é o aplicativo do paciente. São dois portais, no módulo `medcall/client/admin`, publicados na imagem web:

- Portal da Clínica: o administrador vê indicadores, equipe, integrações e o registro de ações. A recepcionista confirma, recusa, remarca e registra comparecimento na agenda.
- Painel de Controle MedCall: indicadores da plataforma e de cada clínica, cadastro de clínicas, profissionais, usuários da clínica, integrações e registro de ações. As rotas desse painel não devolvem dado individual de paciente.

Os dois carregam o mesmo AccessAbility Widget do arquivo `medcall/client/accessability-widget.version`.

Link do Painel de ADM do Sistema e do Painel do ADM da Clínica: https://medcall.grupotp.polyclub.com.br/admin/ . Os dois painéis usam esse endereço.

| Papel | Usuário | Senha |
|---|---|---|
| Equipe MedCall, tasks locais | `admin@medcall.dev` | `medcall-dev-admin` |
| Equipe MedCall, Painel de Controle | `medcall@demo.medcall.dev` | `medcall-demo` |
| Admin da Clínica Boa Vista, Portal da Clínica | `admin.boavista@demo.medcall.dev` | `medcall-demo` |
| Recepção da Boa Vista, Portal da Clínica | `recepcao.boavista@demo.medcall.dev` | `medcall-demo` |

Em 26 de novembro de 2026, `POST /api/v1/staff/auth/login` nesse host aceitou as três contas com senha `medcall-demo`. `medcall@demo.medcall.dev` entrou como equipe MedCall, `admin.boavista@demo.medcall.dev` como administrador da Clínica Boa Vista e `recepcao.boavista@demo.medcall.dev` como recepção. `admin@medcall.dev` com `medcall-dev-admin` é a conta das tasks locais (`MEDCALL_ADMIN_*`). Nesse dia o servidor publicado respondeu `WRONG_CREDENTIALS` para essa senha.

O comando para subir os portais nesta máquina está em [medcall/README.md](medcall/README.md).

## Arquitetura

```text
Cliente Kotlin/JS + React Native (PWA e Android)
        |
        | REST /api/v1
        v
Servidor Ktor
  controller  ->  service  ->  repository (Exposed)
        |
        v
H2 no desenvolvimento, MySQL na produção
```

A regra de negócio fica no service. O controller recebe a requisição e devolve a resposta.

## Onde está o código

```text
medcall/
  common/          compartilhado, JVM e JavaScript
  server/          Ktor, porta 8080
  client/app/      interface e casos de uso
  client/worker/   service worker
  client/expo/     shell Expo
  docker/          imagens
k8s/               namespace medcall, ingress, MySQL
argocd/            Application do GitOps
.github/workflows/ publicação das imagens no ghcr.io
```

Produção: [https://medcall.grupotp.polyclub.com.br](https://medcall.grupotp.polyclub.com.br). O host antigo `recifesaudavel.grupotp.polyclub.com.br` continua servindo a API. Os outros caminhos redirecionam para o host novo. Ver `k8s/ingress.yaml`.

## Como executar

Pré-requisitos, segundo [medcall/README.md](medcall/README.md): JDK 21 e Node.js 24. O primeiro `./gradlew` inicializa o submódulo.

```bash
git clone --recurse-submodules https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2.git
cd App-MedCall-Senac-PE-2026.2/medcall
./gradlew :server:run
./gradlew :client:expo:startWeb
```

O cliente de desenvolvimento abre em `http://localhost:8081`. O servidor responde em `http://localhost:8080/healthz`. Não versione `JWT_SECRET`, senha de banco nem `.env`. Em produção o código de recuperação sai por SMTP para o Mailpit do cluster, com `MAIL_PASSWORD` vazio. A porta 587 liga STARTTLS. A porta 1025, usada pelo Mailpit, fica sem STARTTLS. Sem `MAIL_HOST` e `MAIL_FROM`, a rota responde que o código não pode ser enviado. Os testes usam `MAIL_MODE=capture` e não abrem uma conexão de e-mail.

Testes:

```bash
./gradlew :common:allTests :server:test :client:app:jsNodeTest :client:worker:jsNodeTest
```

## Equipe

Faculdade Senac Pernambuco, Recife-PE. Curso de Análise e Desenvolvimento de Sistemas. Projeto Integrador, agosto a dezembro de 2026. Professor focal: Ícaro Santos Ferreira.

- Thayná Batista da Silva: Scrum Master, requisitos, prototipação e desenvolvimento.
- Polyana Fontes: modelagem de banco, documentação e desenvolvimento.

Contato de Thayná: [thaynabdstec@gmail.com](mailto:thaynabdstec@gmail.com).

## Fontes

- Código e comandos: [medcall/README.md](medcall/README.md), controllers em `medcall/server/src/main/kotlin/grupotp/medcall/server/controller/`.
- Requisitos vigentes: [SPM-A-141](https://grupotp.youtrack.cloud/articles/SPM-A-141). A V7 está marcada como superada em [SPM-A-133](https://grupotp.youtrack.cloud/articles/SPM-A-133).
- [LGPD, Lei nº 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).
- [Ktor, development mode](https://ktor.io/docs/server-development-mode.html).
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), requisito de acessibilidade do projeto. A cobertura de testes com leitores de tela não está concluída neste repositório.
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html): código de uso único, resposta igual para e-mail desconhecido e sessões encerradas depois da troca.
