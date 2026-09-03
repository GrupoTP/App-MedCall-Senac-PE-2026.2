<div align="center">

## 🌐 IDIOMA / LANGUAGE

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>

# Recife Saudável

**Sua saúde na palma da mão.**

Aplicativo acadêmico centrado no paciente para apoiar pré-triagem informativa e agendamento de consultas, desenvolvido no Projeto Integrador do 3º período de Análise e Desenvolvimento de Sistemas da Faculdade Senac Pernambuco.

> **Aviso clínico:** O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. As informações da pré-triagem servem para auxiliar a organização e preparação do atendimento. Em caso de emergência, procure imediatamente um serviço de atendimento de urgência.

## Visão geral

O Recife Saudável é uma plataforma de apoio ao atendimento, com foco no paciente e em uma jornada digital simples, acessível e segura.

O MVP contempla cadastro, autenticação, pré-triagem em quatro etapas, agendamento em três etapas, gerenciamento de consultas, configurações da conta, privacidade e PWA.

O aplicativo do usuário final **não possui seleção de perfil entre paciente, médico, enfermeiro e administrador**. O fluxo é centrado no paciente. Perfis privilegiados, quando necessários, pertencem ao backend ou a módulos administrativos externos.

## Funcionalidades

### Boas-vindas
- Logo e identidade Recife Saudável.
- Tagline **"Sua saúde na palma da mão."**
- Apresentação das funcionalidades.
- Botões **Começar** e **Acessar minha conta**.

### Cadastro e autenticação
- Cadastro e login do paciente.
- Nome completo e nome social.
- Até dois telefones.
- Indicador individual de WhatsApp.
- Contato de emergência.
- Recuperação segura de acesso.
- Sessão e logout.
- Senhas protegidas por hash no backend.

### Consentimentos
O cadastro possui três consentimentos independentes:
1. Ciência da Política de Privacidade.
2. Tratamento necessário para realização do agendamento.
3. Comunicações e lembretes opcionais.

A base legal deve ser definida para cada finalidade concreta.

## Pré-triagem

A pré-triagem possui quatro etapas e é exclusivamente informativa. Ela não diagnostica, prescreve ou altera tratamentos.

### Etapa 1, dados pessoais
- Nome completo.
- Nome social.
- Data de nascimento.
- Telefone principal.
- Telefone secundário.
- Flag de WhatsApp por telefone.
- Nome e telefone do contato de emergência.

### Etapa 2, queixa principal
- Motivo da consulta.
- Sintomas atuais.
- Quando começaram.
- Duração.
- Intensidade de 1 a 10, opcional.
- Observações adicionais.

### Etapa 3, histórico médico
- Doenças anteriores.
- Cirurgias.
- Alergias conhecidas.
- Tratamento atual.
- Observações relevantes.

### Etapa 4, medicamentos
- Medicamentos regulares.
- Medicamentos de uso contínuo.
- Suplementos.
- Outros medicamentos atuais.
- Observações.

Informações de saúde são tratadas como dados pessoais sensíveis.

## Agendamento

Fluxo atual:

**Especialidade + Profissional → Data/Horário → Dados do paciente → Confirmação**

O sistema reutiliza dados da conta e da pré-triagem, permite edição dos campos autorizados e consulta disponibilidade no backend.

O backend deve impedir:
- Duplo agendamento.
- Horário indisponível.
- Profissional inativo.
- Agendamento fora do período configurado.
- Alterações não autorizadas.
- Manipulação da disponibilidade diretamente pelo frontend.

## Dashboard

O dashboard do paciente apresenta:
- Saudação personalizada.
- Ações rápidas.
- Pré-triagem.
- Agendamento.
- Próxima consulta.
- Minhas consultas.
- Configurações.
- Aviso clínico contextual.

## Consultas

O paciente pode visualizar suas consultas, consultar detalhes e cancelar conforme as regras da clínica. O reagendamento é permitido quando disponível e autorizado.

## Configurações

A tela de Settings possui três abas.

**Conta**
- Dados pessoais.
- Nome social.
- Telefones.
- Data de criação.
- Último login.

**Segurança**
- Alterar senha.
- Logout.
- Solicitar exclusão.

**Privacidade**
- Gerenciar consentimentos aplicáveis.
- Acessar a Política de Privacidade.
- Consultar tratamento de dados.
- Consultar direitos do titular.

## Privacidade e LGPD

Existe uma tela dedicada de **Privacidade e Proteção de Dados**, organizada em 9 seções.

O projeto considera finalidade, adequação, necessidade, livre acesso, qualidade, transparência, segurança, prevenção, não discriminação, responsabilização e minimização.

O sistema deve prever mecanismos para acesso, correção, informações sobre tratamento e compartilhamento e eliminação ou revogação quando juridicamente aplicáveis.

Para uso real, devem ser definidos controlador, operadores, bases legais, retenção, compartilhamentos, segurança, resposta a incidentes e eventual RIPD.

## Administração

Os módulos administrativos são externos ao fluxo do paciente.

### Administradores
- Gerenciamento de administradores.
- Mínimo de dois administradores ativos.
- Não permitir operação que deixe menos de dois administradores ativos.
- Confirmação de operações críticas.

### Auditoria
Registro de operações relevantes, incluindo autenticação, alterações de usuários e permissões, acesso a dados sensíveis, operações de consultas e administração.

Não registrar senhas, tokens ou dados sensíveis desnecessários.

### Recuperação
- Recuperação administrativa.
- Autenticação forte.
- Código previamente configurado.
- Registro em auditoria.
- Código mestre fora do frontend.
- Código não armazenado em texto puro.

## Footer obrigatório

O componente `Footer` aparece em todas as telas:

> Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. Todos os direitos reservados.

## ClinicalDisclaimer

O componente `ClinicalDisclaimer` possui os modos:
- `compact`
- `inline`

É utilizado na pré-triagem e no dashboard.

## Segurança

Controles previstos:
- HTTPS/TLS.
- Autenticação.
- Autorização no backend.
- RBAC.
- Validação de entrada.
- Rate limiting.
- CORS restritivo.
- Proteção contra IDOR/BOLA.
- Proteção contra SQL Injection e XSS.
- Gestão segura de segredos.
- Hash de senhas.
- Expiração e invalidação de sessão.
- Auditoria.
- Erros sem exposição de informações internas.

Papéis internos:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_NURSE
ROLE_ADMIN
```

Esses papéis não são selecionáveis pelo paciente no aplicativo.

## Acessibilidade e UX

A referência é **WCAG 2.2 nível AA**.

O projeto considera:
- Contraste mínimo de 4,5:1 para texto normal e 3:1 para texto grande.
- Redimensionamento.
- Foco visível.
- Navegação por teclado no PWA.
- Labels acessíveis.
- Ordem lógica de foco.
- TalkBack e VoiceOver.
- Áreas de toque adequadas.
- Linguagem simples.
- Mensagens de erro claras.
- Não depender somente de cores.

O design considera especialmente usuários idosos.

## PWA

A versão web deverá ser instalável quando suportada, utilizando:
- Web App Manifest.
- Ícones.
- Service Worker.
- HTTPS.
- Cache seguro.
- Atualização controlada.

Dados clínicos privados não devem ser armazenados em cache público.

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

### Stack
- React Native.
- JavaScript/TypeScript.
- React Native Web.
- Java.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- PostgreSQL.
- REST/JSON.
- OpenAPI/Swagger.
- JUnit e Mockito.

## Estrutura

```text
recife-saudavel/
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── screens/
│   │   ├── navigation/
│   │   ├── services/
│   │   ├── hooks/
│   │   ├── contexts/
│   │   ├── validators/
│   │   ├── utils/
│   │   ├── types/
│   │   └── assets/
│   ├── public/
│   └── service-worker
├── backend/
│   └── src/main/java/br/edu/senac/recifesaudavel/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── mapper/
│       ├── security/
│       ├── validation/
│       ├── exception/
│       ├── audit/
│       └── config/
├── database/
│   └── migrations/
├── docs/
│   ├── requisitos/
│   ├── uml/
│   ├── arquitetura/
│   ├── lgpd/
│   ├── seguranca/
│   ├── acessibilidade/
│   ├── governanca/
│   └── api/
├── .env.example
├── .gitignore
├── README.md
├── README_EN.md
└── LICENSE
```

## API de referência

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/password-recovery

GET  /api/v1/patients/me
PUT  /api/v1/patients/me

POST /api/v1/pre-triagens
GET  /api/v1/pre-triagens/{id}

GET  /api/v1/specialties
GET  /api/v1/professionals
GET  /api/v1/availability

POST  /api/v1/appointments
GET   /api/v1/appointments
PATCH /api/v1/appointments/{id}/cancel

GET /api/v1/settings
```

O contrato definitivo deve ser documentado em OpenAPI. IDs enviados pelo frontend nunca devem determinar autorização por si só.

## Dados

Entidades principais:

```text
Usuario
Paciente
Profissional
Administrador
Perfil
Permissao
Telefone
ContatoEmergencia
PreTriagem
HistoricoMedico
Medicamento
Consulta
Especialidade
Clinica
HorarioDisponivel
Auditoria
Consentimento
Notificacao
Sessao
```

## Testes

- Validação de dados.
- Disponibilidade.
- Conflito de horários.
- Cancelamento.
- Permissões.
- Autenticação.
- Acesso indevido a outro paciente.
- Manipulação de IDs.
- Concorrência no agendamento.
- Exposição de dados em logs.
- Exposição de segredos no frontend.
- Integração cliente, API e banco.
- TalkBack.
- VoiceOver.
- Teclado no PWA.
- Contraste.
- Foco e ordem de leitura.

## Escopo do MVP

### Incluído
- [x] Cadastro e login.
- [x] Nome social.
- [x] Até dois telefones.
- [x] WhatsApp por telefone.
- [x] Contato de emergência.
- [x] Pré-triagem em quatro etapas.
- [x] Agendamento em três etapas.
- [x] Reutilização de dados no agendamento.
- [x] Gerenciamento de consultas.
- [x] Settings com Conta, Segurança e Privacidade.
- [x] Tela dedicada de Privacidade.
- [x] Footer em todas as telas.
- [x] ClinicalDisclaimer.
- [x] PWA.
- [x] API REST.

### Fora do escopo
- Diagnóstico automatizado.
- Prescrição.
- Ajuste de medicamentos.
- Teleconsulta.
- Chat clínico em tempo real.
- Integração com prontuários reais no MVP.
- Integração com RNDS no MVP.
- Pagamentos reais.
- Dados reais de pacientes no ambiente acadêmico.
- Seleção pública de perfis privilegiados.

## Projeto acadêmico

**Instituição:** Faculdade Senac Pernambuco, Recife, PE, Brasil  
**Curso:** Análise e Desenvolvimento de Sistemas  
**Projeto:** Projeto Integrador, 3º período  
**Período:** Agosto a dezembro de 2026  
**Professor focal:** Prof. Ícaro Santos Ferreira  
**Alunas:** Thayná Batista da Silva e Poliana Fontes

## Links

**GitHub:** https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2

**Figma:** https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6

## Referências

- [LGPD, Planalto](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [ANPD, Direitos dos Titulares](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares)
- [ANPD, RIPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd)
- [ANVISA, Software como Dispositivo Médico](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/software-como-dispositivo-medico-perguntas-e-respostas)
- [W3C, WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- [React Native, Accessibility](https://reactnative.dev/docs/accessibility)
- [Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [MDN, Progressive Web Apps](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Reference)
- [Ministério da Saúde, Segurança do Paciente](https://www.gov.br/saude/pt-br/composicao/saes/seguranca-do-paciente)
