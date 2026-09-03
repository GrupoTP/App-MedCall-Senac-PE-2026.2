<div align="center">

## 🌐 IDIOMA / LANGUAGE

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>

# Recife Saudável

**Sua saúde na palma da mão.**

Aplicativo de apoio à pré-triagem e ao agendamento de consultas, desenvolvido como Projeto Integrador do 3º período do curso de Análise e Desenvolvimento de Sistemas da Faculdade Senac Pernambuco.

> **Aviso clínico:** O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. As informações fornecidas na pré-triagem servem para auxiliar a organização e preparação do atendimento. Em caso de emergência, procure imediatamente um serviço de atendimento de urgência.

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
