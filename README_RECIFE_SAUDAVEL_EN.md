<div align="center">

## 🌐 LANGUAGE / IDIOMA

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>

# Recife Saudável

**Your health in the palm of your hand.**

Academic patient-centered application supporting informational pre-screening and medical appointment scheduling, developed as the 3rd-semester Integrative Project in Systems Analysis and Development at Faculdade Senac Pernambuco.

> **Aviso clínico:** O Recife Saudável não realiza diagnóstico médico e não substitui atendimento profissional. As informações da pré-triagem servem para auxiliar a organização e preparação do atendimento. Em caso de emergência, procure imediatamente um serviço de atendimento de urgência.

## Overview

Recife Saudável is a healthcare support platform focused on the patient and on a simple, accessible, and secure digital journey.

The MVP includes registration, authentication, four-step pre-screening, three-step appointment scheduling, appointment management, account settings, privacy, and PWA support.

The end-user application **does not provide profile selection between patient, doctor, nurse, and administrator**. The journey is patient-centered. Privileged roles, when required, belong to the backend or external administrative modules.

## Features

### Welcome
- Logo e identidade Recife Saudável.
- Tagline **"Your health in the palm of your hand."**
- Apresentação das funcionalidades.
- Botões **Começar** e **Acessar minha conta**.

### Registration and authentication
- Cadastro e login do paciente.
- Nome completo e nome social.
- Até dois telefones.
- Indicador individual de WhatsApp.
- Emergency contact.
- Recuperação segura de acesso.
- Sessão e logout.
- Senhas protegidas por hash no backend.

### Consents
O cadastro possui três consentimentos independentes:
1. Ciência da Política de Privacidade.
2. Tratamento necessário para realização do agendamento.
3. Comunicações e lembretes opcionais.

A base legal deve ser definida para cada finalidade concreta.

## Pre-screening

Pre-screening has four steps and is strictly informational. It does not diagnose, prescribe, or alter treatments.

### Step 1, personal information
- Nome completo.
- Social name.
- Data de nascimento.
- Primary phone.
- Secondary phone.
- Flag de WhatsApp por telefone.
- Nome e telefone do contato de emergência.

### Step 2, main complaint
- Reason for appointment.
- Current symptoms.
- When they started.
- Duration.
- Intensidade de 1 a 10, opcional.
- Additional notes.

### Step 3, medical history
- Previous diseases.
- Surgeries.
- Known allergies.
- Current treatment.
- Relevant notes.

### Step 4, medications
- Regular medications.
- Continuous-use medications.
- Supplements.
- Other current medications.
- Observações.

Health information is treated as sensitive personal data.

## Appointment scheduling

Current flow:

**Especialidade + Profissional → Data/Horário → Dados do paciente → Confirmação**

The system reuses account and pre-screening data, allows editing of permitted fields, and retrieves availability from the backend.

The backend must prevent:
- Double booking.
- Unavailable time slot.
- Inactive professional.
- Scheduling outside the configured period.
- Unauthorized changes.
- Direct manipulation of availability by the frontend.

## Dashboard

O dashboard do paciente apresenta:
- Personalized greeting.
- Quick actions.
- Pré-triagem.
- Agendamento.
- Next appointment.
- My appointments.
- Settings.
- Contextual clinical disclaimer.

## Appointments

O paciente pode view appointments, view appointment details e cancel according to clinic rules. O rescheduling is available when authorized.

## Settings

A tela de Settings possui três abas.

**Conta**
- Personal data.
- Social name.
- Telefones.
- Creation date.
- Last login.

**Segurança**
- Change password.
- Logout.
- Request deletion.

**Privacidade**
- Manage applicable consents.
- Access the Privacy Policy.
- View data processing information.
- View data subject rights.

## Privacy and LGPD

A dedicated **Privacy and Data Protection** screen is organized into 9 sections.

The project considers purpose, adequacy, necessity, free access, data quality, transparency, security, prevention, non-discrimination, accountability, and minimization.

O sistema deve prever mecanismos para acesso, correção, informações sobre tratamento e compartilhamento e eliminação ou revogação quando juridicamente aplicáveis.

For real-world use, the controller, processors, legal bases, retention, sharing, security, incident response, and possible DPIA/RIPD must be defined.

## Administration

Administrative modules are external to the patient journey.

### Administrators
- Gerenciamento de administradores.
- Mínimo de dois administradores ativos.
- Não permitir operação que deixe menos de dois administradores ativos.
- Confirmação de operações críticas.

### Audit
Relevant operations are logged, including authentication, user and permission changes, access to sensitive data, appointment operations, and administration.

Do not log passwords, tokens, or unnecessary sensitive data.

### Recovery
- Recuperação administrativa.
- Autenticação forte.
- Código previamente configurado.
- Registro em auditoria.
- Código mestre fora do frontend.
- Código não armazenado em texto puro.

## Required footer

The `Footer` component appears on every screen:

> Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. All rights reserved.

## ClinicalDisclaimer

The `ClinicalDisclaimer` component has the modes:
- `compact`
- `inline`

It is used in pre-screening and on the dashboard.

## Security

Planned controls:
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

Internal roles:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_NURSE
ROLE_ADMIN
```

These roles cannot be selected by the patient in the application.

## Accessibility and UX

The reference is **WCAG 2.2 Level AA**.

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

The design specifically considers older users.

## PWA

The web version should be installable when supported, using:
- Web App Manifest.
- Ícones.
- Service Worker.
- HTTPS.
- Cache seguro.
- Atualização controlada.

Private clinical data must not be stored in public cache.

## Architecture

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

## Structure

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

## Reference API

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

The definitive contract must be documented in OpenAPI. IDs supplied by the frontend must never determine authorization by themselves.

## Data

Main entities:

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

## Testing

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

## MVP scope

### Included
- [x] Cadastro e login.
- [x] Social name.
- [x] Até dois telefones.
- [x] WhatsApp por telefone.
- [x] Emergency contact.
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

### Out of scope
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

## Academic project

**Institution:** Faculdade Senac Pernambuco, Recife, PE, Brasil  
**Program:** Systems Analysis and Development  
**Project:** Integrative Project, 3rd semester  
**Period:** August to December 2026  
**Faculty advisor:** Prof. Ícaro Santos Ferreira  
**Students:** Thayná Batista da Silva e Poliana Fontes

## Links

**GitHub:** https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2

**Figma:** https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6

## References

- [LGPD, Planalto](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [ANPD, Direitos dos Titulares](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares)
- [ANPD, RIPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd)
- [ANVISA, Software como Dispositivo Médico](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/software-como-dispositivo-medico-perguntas-e-respostas)
- [W3C, WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- [React Native, Accessibility](https://reactnative.dev/docs/accessibility)
- [Spring Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [MDN, Progressive Web Apps](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Reference)
- [Ministério da Saúde, Segurança do Paciente](https://www.gov.br/saude/pt-br/composicao/saes/seguranca-do-paciente)
