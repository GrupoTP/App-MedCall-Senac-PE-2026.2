<div align="center">

## 🌐 IDIOMA / LANGUAGE

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>

# Recife Saudável

**Sua saúde na palma da mão.**

Aplicativo de apoio à pré-triagem e ao agendamento de consultas, desenvolvido como Projeto Integrador do 3º período do curso de Análise e Desenvolvimento de Sistemas da Faculdade Senac Pernambuco.

> **Aviso clínico:** O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. As informações fornecidas na pré-triagem servem para auxiliar a organização e preparação do atendimento. Em caso de emergência, procure imediatamente um serviço de atendimento de urgência.

<div align="center">

![React Native](https://img.shields.io/badge/React_Native-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![PWA](https://img.shields.io/badge/PWA-5A0FC8?style=for-the-badge&logo=pwa&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![REST API](https://img.shields.io/badge/REST_API-005571?style=for-the-badge&logo=swagger&logoColor=white)
![Figma](https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white)
![LGPD](https://img.shields.io/badge/LGPD-Compliant-2E7D32?style=for-the-badge)
![License](https://img.shields.io/badge/Licença-Acadêmica-lightgrey?style=for-the-badge)

</div>

<details open>
<summary><h2>📑 Índice</h2></summary>

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
  - [Tela de boas-vindas](#tela-de-boas-vindas)
  - [Cadastro e conta](#cadastro-e-conta)
  - [Seleção de perfil](#seleção-de-perfil)
  - [Pré-triagem](#pré-triagem)
  - [Agendamento](#agendamento)
  - [Dashboard](#dashboard)
  - [Configurações](#configurações)
  - [Privacidade](#privacidade)
  - [Administração](#administração)
- [Rodapé obrigatório](#rodapé-obrigatório)
- [Componentes de segurança e saúde](#componentes-de-segurança-e-saúde)
  - [`ClinicalDisclaimer`](#clinicaldisclaimer)
  - [Privacidade e LGPD](#privacidade-e-lgpd)
  - [Controle de acesso](#controle-de-acesso)
  - [Auditoria](#auditoria)
- [Acessibilidade](#acessibilidade)
- [Arquitetura](#arquitetura)
- [Tecnologias](#tecnologias)
  - [Frontend](#frontend)
  - [Backend](#backend)
  - [Banco de dados](#banco-de-dados)
  - [API](#api)
  - [Segurança](#segurança)
  - [Qualidade](#qualidade)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Regras importantes](#regras-importantes)
- [Critérios principais do MVP](#critérios-principais-do-mvp)
  - [Cadastro](#cadastro)
  - [Pré-triagem](#pré-triagem-1)
  - [Agendamento](#agendamento-1)
  - [Configurações](#configurações-1)
  - [Administração](#administração-1)
  - [Interface](#interface)
- [Projeto acadêmico](#projeto-acadêmico)
- [Fontes e referências](#fontes-e-referências)
- [🎯 Problema](#-problema)
- [✅ A Solução](#-a-solução)
- [🖼️ Protótipos](#️-protótipos)
- [🌍 Impacto ESG](#-impacto-esg)
- [📈 Mercado](#-mercado)
- [💼 Modelo de Negócio](#-modelo-de-negócio)
- [🧾 Análise de Custo e Viabilidade](#-análise-de-custo-e-viabilidade)
- [🎯 Público-Alvo](#-público-alvo)
- [🛠️ Tecnologias Utilizadas](#️-tecnologias-utilizadas)
- [🚀 Como Executar Localmente](#-como-executar-localmente)
  - [Pré-requisitos](#pré-requisitos)
  - [Clonar](#clonar)
  - [Backend](#backend-1)
  - [Frontend](#frontend-1)
- [👥 Equipe](#-equipe)
  - [Polyana Fontes](#polyana-fontes)
  - [Thayná Batista da Silva](#thayná-batista-da-silva)
- [📋 Documentação Técnica](#-documentação-técnica)
  - [1. Definição do Problema](#1-definição-do-problema)
  - [2. Backlog e Engenharia de Requisitos](#2-backlog-e-engenharia-de-requisitos)
  - [3. UX/UI para Interfaces Inteligentes](#3-uxui-para-interfaces-inteligentes)
  - [4. Arquitetura de Software e Stack de Desenvolvimento](#4-arquitetura-de-software-e-stack-de-desenvolvimento)
  - [5. Relato do Processo e Engenharia de Prompt](#5-relato-do-processo-e-engenharia-de-prompt)
  - [6. Análise de Custos e Viabilidade](#6-análise-de-custos-e-viabilidade)
  - [7. Evoluções Futuras](#7-evoluções-futuras)
- [📽️ Demonstrações](#️-demonstrações)
- [📄 Licença](#-licença)
- [📞 Contato](#-contato)
  - [Polyana Fontes](#polyana-fontes-1)
  - [Thayná Batista da Silva](#thayná-batista-da-silva-1)
- [🎓 Informações Acadêmicas](#-informações-acadêmicas)

</details>

## Sobre o projeto

O Recife Saudável foi desenvolvido com foco em uma jornada digital simples, acessível e segura para pacientes, especialmente considerando usuários idosos.

O sistema foi estruturado segundo uma arquitetura cliente servidor e preparado para evolução futura, com frontend em React Native, compatibilidade com PWA, API REST e backend Java com Spring Boot.

**Objetivo principal:** permitir cadastro, pré-triagem informativa, consulta de especialidades e profissionais e agendamento de consultas.

## Funcionalidades

### Tela de boas-vindas

- Logo e identidade do Recife Saudável.
- Tagline: **"Sua saúde na palma da mão."**
- Apresentação das principais funcionalidades.
- Botões **Começar** e **Acessar minha conta**.

### Cadastro e conta

- Cadastro de usuário.
- Nome completo e nome social.
- Até dois telefones.
- Identificação individual de WhatsApp por telefone.
- Três consentimentos independentes:
  - Política de Privacidade.
  - Tratamento necessário para agendamento.
  - Comunicações opcionais.

### Seleção de perfil

O aplicativo apresenta os perfis disponíveis, mas a autorização efetiva é realizada pelo servidor. A interface não concede privilégios por conta própria.

Perfis previstos:

- Paciente.
- Médico(a).
- Enfermeiro(a).
- Administrador(a).

### Pré-triagem

A pré-triagem é informativa e não realiza diagnóstico.

**Etapa 1, dados pessoais**

- Nome completo.
- Nome social.
- Data de nascimento.
- Telefone principal.
- Telefone secundário.
- Indicador de WhatsApp para cada telefone.
- Nome do contato de emergência.
- Telefone do contato de emergência.

**Etapa 2, queixa principal**

- Motivo da consulta.
- Sintomas atuais.
- Quando começaram.
- Duração.
- Intensidade de 1 a 10, opcional.
- Observações adicionais.

O projeto também prevê etapas para histórico médico e medicamentos, conforme a especificação funcional.

### Agendamento

O fluxo foi reorganizado para:

**Especialidade + Profissional → Data/Horário → Dados do paciente → Confirmação**

A disponibilidade deve ser determinada pelo servidor, evitando conflitos de horários e agendamentos em períodos indisponíveis.

### Dashboard

O dashboard apresenta informações e ações de acordo com o perfil autorizado.

Para pacientes, inclui:

- Pré-triagem.
- Agendamento.
- Próxima consulta.
- Minhas consultas.
- Configurações.

O dashboard também apresenta o aviso clínico em formato apropriado.

### Configurações

A tela de Settings possui três abas:

**Conta**
- Dados pessoais.
- Nome social.
- Telefones.
- Informações da conta.

**Segurança**
- Alteração de senha.
- Logout.
- Solicitação de exclusão.

**Privacidade**
- Gerenciamento de consentimentos.
- Acesso à Política de Privacidade.

### Privacidade

Foi criada uma tela dedicada de **Privacidade e Proteção de Dados**, contendo 9 seções da política de dados, com foco em transparência e conformidade com a LGPD.

### Administração

O ambiente administrativo possui três abas:

**Administradores**
- Gerenciamento de administradores.
- Regra de mínimo de dois administradores ativos.

**Auditoria**
- Registro de operações administrativas relevantes.
- Rastreabilidade de ações críticas.

**Recuperação**
- Código de recuperação administrativa.
- Contexto e orientações de segurança.
- O código mestre não deve ficar exposto no frontend.

## Rodapé obrigatório

Todas as telas utilizam o componente `Footer` com o texto definido na especificação:

> Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. Todos os direitos reservados.

## Componentes de segurança e saúde

### `ClinicalDisclaimer`

Componente reutilizável com dois modos:

- `compact`
- `inline`

Utilizado na pré-triagem e no dashboard.

### Privacidade e LGPD

O sistema considera:

- Finalidade.
- Necessidade.
- Transparência.
- Segurança.
- Prevenção.
- Não discriminação.
- Responsabilização.
- Controle de acesso.
- Minimização de dados.

Dados referentes à saúde são tratados como dados pessoais sensíveis segundo a LGPD.

### Controle de acesso

A arquitetura prevê RBAC, com separação entre perfil do usuário e permissões.

Exemplo:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_NURSE
ROLE_ADMIN
```

As regras críticas devem ser verificadas no backend.

### Auditoria

Operações relevantes devem ser registradas, incluindo ações administrativas, autenticação, alterações de permissões e acesso a informações sensíveis quando aplicável.

Senhas, tokens e dados sensíveis desnecessários não devem ser armazenados nos logs.

## Acessibilidade

O projeto considera **WCAG 2.2 nível AA** e recursos de acessibilidade do React Native.

Principais cuidados:

- Contraste adequado.
- Textos legíveis.
- Botões com áreas de toque apropriadas.
- Labels acessíveis.
- Mensagens de erro claras.
- Navegação lógica.
- Compatibilidade com TalkBack e VoiceOver.
- Navegação por teclado no PWA.
- Não utilizar apenas cores para transmitir informações.

## Arquitetura

```text
React Native
      ↓
React Native Web / PWA
      ↓
REST API
      ↓
Spring Boot
      ↓
Spring Security
      ↓
JPA / Hibernate
      ↓
PostgreSQL
```

## Tecnologias

### Frontend

- React Native.
- JavaScript / TypeScript.
- React Native Web.
- PWA.
- HTML.
- CSS.

### Backend

- Java.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- Bean Validation.
- OpenAPI / Swagger.

### Banco de dados

- PostgreSQL.

### API

- REST.
- JSON.
- HTTPS.
- Versionamento previsto em `/api/v1/`.

### Segurança

- Autenticação.
- Autorização baseada em papéis.
- Hash de senha.
- Controle de sessão.
- Auditoria.
- Validação de entrada.
- Rate limiting.
- Proteção de segredos.

### Qualidade

- JUnit.
- Mockito.
- Testes de integração.
- Testes de API.
- Testes de acessibilidade.

## Estrutura do projeto

```text
recife-saudavel/
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── screens/
│   │   ├── navigation/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── contexts/
│   │   ├── validators/
│   │   └── utils/
│   ├── public/
│   │   ├── manifest.json
│   │   └── icons/
│   └── service-worker
│
├── backend/
│   └── src/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── security/
│       ├── validation/
│       ├── audit/
│       └── config/
│
├── database/
│   └── migrations/
│
├── docs/
│   ├── requisitos/
│   ├── uml/
│   ├── arquitetura/
│   ├── lgpd/
│   ├── seguranca/
│   ├── acessibilidade/
│   ├── governanca/
│   └── api/
│
├── .env.example
├── .gitignore
├── README.md
├── README_EN.md
└── LICENSE
```

## Regras importantes

- A pré-triagem não diagnostica.
- A autorização de perfis privilegiados vem do backend.
- A disponibilidade de horários vem do servidor.
- O backend valida regras críticas de negócio.
- Deve existir no mínimo dois administradores ativos.
- O sistema não deve permitir que a operação administrativa reduza o número de administradores abaixo do mínimo definido.
- O código mestre de recuperação não deve ser exposto no frontend.
- Senhas, tokens, chaves e credenciais não devem ser enviados ao GitHub.
- O ambiente acadêmico deve utilizar dados fictícios.
- Dados clínicos não devem ser armazenados indiscriminadamente em cache público do Service Worker.
- O projeto não deve ser utilizado para armazenar prontuários ou dados reais sem as devidas bases legais, controles e autorizações institucionais.

## Critérios principais do MVP

### Cadastro
- [x] Nome social.
- [x] Dois telefones.
- [x] Indicador de WhatsApp por telefone.
- [x] Consentimentos independentes.

### Pré-triagem
- [x] Dados pessoais.
- [x] Contato de emergência.
- [x] Motivo da consulta.
- [x] Sintomas.
- [x] Início e duração.
- [x] Intensidade opcional.
- [x] Observações.

### Agendamento
- [x] Especialidade.
- [x] Profissional.
- [x] Data e horário.
- [x] Dados do paciente.
- [x] Confirmação.

### Configurações
- [x] Conta.
- [x] Segurança.
- [x] Privacidade.

### Administração
- [x] Administradores.
- [x] Auditoria.
- [x] Recuperação.

### Interface
- [x] Footer em todas as telas.
- [x] Aviso clínico.
- [x] Tela de boas-vindas atualizada.
- [x] Tela dedicada de privacidade.
- [x] Seleção de perfil com autorização controlada pelo servidor.

## Projeto acadêmico

**Instituição:** Faculdade Senac Pernambuco, Recife, PE  
**Curso:** Análise e Desenvolvimento de Sistemas  
**Projeto:** Projeto Integrador, 3º período  
**Período:** Agosto a dezembro de 2026  
**Professor focal:** Prof. Ícaro Santos Ferreira  
**Alunas:** Thayná Batista da Silva e Poliana Fontes

## Fontes e referências

- [LGPD, Lei nº 13.709/2018, Planalto](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [ANPD, Direitos dos Titulares](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares)
- [ANPD, Relatório de Impacto à Proteção de Dados, RIPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd)
- [ANVISA, Software como Dispositivo Médico](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/software-como-dispositivo-medico-perguntas-e-respostas)
- [W3C, WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- [React Native, Accessibility](https://reactnative.dev/docs/accessibility)
- [Spring Security, Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [MDN, Progressive Web Apps](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Reference)
- [Ministério da Saúde, Segurança do Paciente](https://www.gov.br/saude/pt-br/composicao/saes/seguranca-do-paciente)

---

**Recife Saudável**, um MVP acadêmico de saúde digital com foco em UX, Mobile, arquitetura cliente servidor, acessibilidade, segurança, privacidade e governança.

## 🎯 Problema

O projeto busca melhorar a jornada digital do paciente, centralizando informações para pré-triagem informativa e agendamento de consultas em uma experiência simples, acessível e segura.

## ✅ A Solução

O **App Recife Saudável** é um MVP acadêmico centrado no paciente, com cadastro, autenticação, pré-triagem informativa, agendamento, gerenciamento de consultas, privacidade, segurança e PWA.

O aplicativo não realiza diagnóstico, prescrição ou decisão clínica automatizada.

## 🖼️ Protótipos

<div align="center">

<a href="https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6"><img src="https://img.shields.io/badge/Ver%20Prot%C3%B3tipo%20no%20Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white" alt="Ver Protótipo no Figma"></a>

<a href="https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2"><img src="https://img.shields.io/badge/Ver%20Reposit%C3%B3rio%20no%20GitHub-181717?style=for-the-badge&logo=github&logoColor=white" alt="Ver Repositório no GitHub"></a>

</div>

## 🌍 Impacto ESG

- **Social:** jornada digital mais acessível e simples para o paciente.
- **Governança:** privacidade, segurança, auditoria, controle de acesso e LGPD.
- **Ambiental:** digitalização de etapas administrativas e redução de documentos físicos.

## 📈 Mercado

A proposta está inserida na digitalização de serviços de saúde, com foco na organização da jornada do paciente, agendamento e coleta estruturada de informações antes do atendimento.

Este é um projeto acadêmico e não representa validação comercial ou clínica do produto.

## 💼 Modelo de Negócio

Como conceito de produto, a solução pode ser adaptada para clínicas e instituições de saúde interessadas em gestão digital da jornada de agendamento, pré-triagem informativa, organização de dados, comunicações e governança.

Qualquer exploração comercial exigiria validação jurídica, regulatória, técnica, de segurança e de mercado.

## 🧾 Análise de Custo e Viabilidade

O MVP utiliza tecnologias amplamente adotadas e de código aberto, reduzindo custos iniciais de desenvolvimento. Em produção, os principais custos potenciais incluem infraestrutura, banco de dados, observabilidade, segurança, domínio, certificados, manutenção, suporte, conformidade e serviços externos.

## 🎯 Público-Alvo

**Público primário:** pacientes que precisam organizar informações e realizar agendamentos de forma digital.

**Público institucional:** clínicas, instituições de saúde e equipes administrativas.

## 🛠️ Tecnologias Utilizadas

| Camada | Tecnologias |
|---|---|
| Frontend | React Native, TypeScript/JavaScript, React Native Web |
| PWA | Web App Manifest, Service Worker, HTTPS |
| Backend | Java, Spring Boot, Spring Security |
| Persistência | Spring Data JPA, Hibernate |
| Banco | PostgreSQL |
| API | REST, JSON, OpenAPI/Swagger |
| Testes | JUnit, Mockito, integração e acessibilidade |
| Design | Figma |
| Governança | LGPD, RBAC, auditoria e segurança |

## 🚀 Como Executar Localmente

### Pré-requisitos

- Node.js e npm.
- Java JDK compatível com o backend.
- PostgreSQL.
- Git.

### Clonar

```bash
git clone https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2.git
cd App-Recife-Saudavel-Senac-PE-2026.2
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

No Windows:

```powershell
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Use `.env.example` como referência e nunca versione credenciais reais. Os comandos podem variar conforme a configuração definitiva do projeto.

## 👥 Equipe

### Polyana Fontes

**Desenvolvedora Full Stack · Modelagem de Banco de Dados · Documentação Técnica**

Aluna de Análise e Desenvolvimento de Sistemas  
Faculdade Senac Pernambuco, Recife-PE

### Thayná Batista da Silva

**Gerente / Analista de Requisitos · Scrum Master**  
**Product Designer · Prototipação de Alta Fidelidade (Figma)**

Aluna de Análise e Desenvolvimento de Sistemas  
Faculdade Senac Pernambuco, Recife-PE  
Turma 2025 · Formação prevista: 2027

## 📋 Documentação Técnica

### 1. Definição do Problema
Contexto, problema, objetivos, escopo e restrições do projeto.

### 2. Backlog e Engenharia de Requisitos
Requisitos funcionais e não funcionais, regras de negócio, critérios de aceitação, casos de uso e backlog.

### 3. UX/UI para Interfaces Inteligentes
Arquitetura de informação, fluxos, prototipação, acessibilidade, responsividade e experiência do paciente.

### 4. Arquitetura de Software e Stack de Desenvolvimento
Arquitetura cliente-servidor, API REST, Spring Boot, PostgreSQL, segurança, PWA e organização do código.

### 5. Relato do Processo e Engenharia de Prompt
Processo de desenvolvimento, decisões técnicas e uso responsável de ferramentas de IA.

### 6. Análise de Custos e Viabilidade
Estimativas, premissas, infraestrutura e viabilidade técnica e econômica.

### 7. Evoluções Futuras
Possíveis evoluções incluem integrações institucionais, notificações avançadas, observabilidade, acessibilidade ampliada e integrações com sistemas de saúde, após validação técnica e regulatória.

## 📽️ Demonstrações

- [🎨 Ver Protótipo no Figma](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6)
- [💻 Ver Repositório no GitHub](https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2)

## 📄 Licença

Projeto desenvolvido para fins acadêmicos. A licença definitiva deve ser definida no arquivo `LICENSE` do repositório.

## 📞 Contato

### Polyana Fontes

Desenvolvedora Full Stack, Modelagem de Banco de Dados e Documentação Técnica.

### Thayná Batista da Silva

**Scrum Master · Product Designer · Requirements Analyst**  
Senac Recife-PE, Análise e Desenvolvimento de Sistemas.

📧 **Email:** [thaynabdstec@gmail.com](mailto:thaynabdstec@gmail.com)  
📱 **Telefone:** +55 (81) 97912-6121

## 🎓 Informações Acadêmicas

**Nome do App:** 🏫 App Recife Saudável  
**Projeto iniciado em:** Agosto de 2026  
**Instituição:** Faculdade Senac Pernambuco, Recife-PE, Brasil  
**Curso:** Análise e Desenvolvimento de Sistemas  
**Projeto:** Projeto Integrador, 3º período  
**Professor focal do Projeto Integrador:** Prof. Ícaro Santos Ferreira  
**Alunas:** Thayná Batista da Silva e Polyana Fontes
