<div align="center">

## 🌐 LANGUAGE / IDIOMA

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>

# Recife Saudável

**Your health in the palm of your hand.**

A healthcare support application for pre-screening and medical appointment scheduling, developed as the Integrative Project for the 3rd semester of the Systems Analysis and Development program at Faculdade Senac Pernambuco.

> **Clinical disclaimer:** Recife Saudável does not provide medical diagnosis and does not replace professional medical care. Information provided during pre-screening is intended to support the organization and preparation of care. In an emergency, seek urgent medical attention immediately.

## About the project

Recife Saudável was designed around a simple, accessible, and secure digital journey for patients, with particular consideration for older users.

The system follows a client-server architecture and is designed for future evolution, with a React Native frontend, PWA compatibility, REST API, and Java backend using Spring Boot.

**Main objective:** provide user registration, informational pre-screening, specialty and professional search, and medical appointment scheduling.

## Features

### Welcome screen

- Recife Saudável logo and visual identity.
- Tagline: **"Your health in the palm of your hand."**
- Presentation of the main features.
- **Get Started** and **Access my account** buttons.

### Registration and account

- User registration.
- Full name and social name.
- Up to two phone numbers.
- Individual WhatsApp identification for each phone.
- Three independent consents:
  - Privacy Policy.
  - Processing required for appointment scheduling.
  - Optional communications.

### Profile selection

The application displays the available profiles, but effective authorization is performed by the server. The interface does not grant privileges by itself.

Available profiles:

- Patient.
- Doctor.
- Nurse.
- Administrator.

### Pre-screening

The pre-screening process is informational and does not perform diagnosis.

**Step 1, personal information**

- Full name.
- Social name.
- Date of birth.
- Primary phone.
- Secondary phone.
- WhatsApp indicator for each phone.
- Emergency contact name.
- Emergency contact phone.

**Step 2, main complaint**

- Reason for appointment.
- Current symptoms.
- When symptoms started.
- Duration.
- Optional intensity from 1 to 10.
- Additional notes.

The project specification also includes stages for medical history and medications.

### Appointment scheduling

The flow was reorganized as:

**Specialty + Professional → Date/Time → Patient Information → Confirmation**

Availability must be determined by the server, preventing scheduling conflicts and appointments outside configured availability.

### Dashboard

The dashboard displays information and actions according to the authorized profile.

For patients, it includes:

- Pre-screening.
- Appointment scheduling.
- Next appointment.
- My appointments.
- Settings.

The clinical disclaimer is also displayed on the dashboard in the appropriate format.

### Settings

The Settings screen contains three tabs:

**Account**
- Personal information.
- Social name.
- Phone numbers.
- Account information.

**Security**
- Change password.
- Logout.
- Request account deletion.

**Privacy**
- Consent management.
- Privacy Policy access.

### Privacy

A dedicated **Privacy and Data Protection** screen was added, containing 9 sections of the data policy, focused on transparency and LGPD compliance.

### Administration

The administration area contains three tabs:

**Administrators**
- Administrator management.
- Minimum of two active administrators.

**Audit**
- Log of relevant administrative operations.
- Traceability of critical actions.

**Recovery**
- Administrative recovery code.
- Security context and guidance.
- The master code must not be exposed in the frontend.

## Required footer

All screens use the `Footer` component with the text defined in the specification:

> Copyright © 2026, Polyana Fontes; Thayná Batista da Silva; App Recife Saudável, Senac PE. All rights reserved.

## Healthcare and security components

### `ClinicalDisclaimer`

Reusable component with two modes:

- `compact`
- `inline`

Used in the pre-screening flow and dashboard.

### Privacy and LGPD

The system considers:

- Purpose limitation.
- Necessity.
- Transparency.
- Security.
- Prevention.
- Non-discrimination.
- Accountability.
- Access control.
- Data minimization.

Health-related information is treated as sensitive personal data under the Brazilian LGPD.

### Access control

The architecture uses RBAC, separating the user's role from their permissions.

Example:

```text
ROLE_PATIENT
ROLE_DOCTOR
ROLE_NURSE
ROLE_ADMIN
```

Critical rules must be enforced by the backend.

### Audit

Relevant operations should be recorded, including administrative actions, authentication events, permission changes, and access to sensitive information when applicable.

Passwords, tokens, and unnecessary sensitive information must not be stored in logs.

## Accessibility

The project targets **WCAG 2.2 Level AA** and React Native accessibility features.

Main considerations:

- Adequate contrast.
- Readable text.
- Appropriate touch targets.
- Accessible labels.
- Clear error messages.
- Logical navigation.
- TalkBack and VoiceOver compatibility.
- Keyboard navigation in the PWA.
- Information must not be conveyed through color alone.

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

## Technologies

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

### Database

- PostgreSQL.

### API

- REST.
- JSON.
- HTTPS.
- Planned versioning through `/api/v1/`.

### Security

- Authentication.
- Role-based authorization.
- Password hashing.
- Session management.
- Auditing.
- Input validation.
- Rate limiting.
- Secret protection.

### Quality

- JUnit.
- Mockito.
- Integration tests.
- API tests.
- Accessibility tests.

## Project structure

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

## Important rules

- Pre-screening must not diagnose the patient.
- Authorization for privileged profiles comes from the backend.
- Appointment availability comes from the server.
- Critical business rules are validated by the backend.
- At least two active administrators must exist.
- Administrative operations must not reduce the number of administrators below the defined minimum.
- The master recovery code must not be exposed in the frontend.
- Passwords, tokens, keys, and credentials must never be committed to GitHub.
- The academic environment must use fictitious data.
- Clinical data must not be indiscriminately stored in the public Service Worker cache.
- The project must not be used to store real patient records or health data without the appropriate legal basis, controls, and institutional authorization.

## Main MVP criteria

### Registration
- [x] Social name.
- [x] Two phone numbers.
- [x] WhatsApp indicator per phone.
- [x] Independent consents.

### Pre-screening
- [x] Personal information.
- [x] Emergency contact.
- [x] Reason for appointment.
- [x] Symptoms.
- [x] Start date and duration.
- [x] Optional intensity.
- [x] Additional notes.

### Appointment scheduling
- [x] Specialty.
- [x] Professional.
- [x] Date and time.
- [x] Patient information.
- [x] Confirmation.

### Settings
- [x] Account.
- [x] Security.
- [x] Privacy.

### Administration
- [x] Administrators.
- [x] Audit.
- [x] Recovery.

### Interface
- [x] Footer on all screens.
- [x] Clinical disclaimer.
- [x] Updated welcome screen.
- [x] Dedicated privacy screen.
- [x] Profile selection with server-controlled authorization.

## Academic project

**Institution:** Faculdade Senac Pernambuco, Recife, PE  
**Program:** Systems Analysis and Development  
**Project:** Integrative Project, 3rd semester  
**Period:** August to December 2026  
**Faculty advisor:** Prof. Ícaro Santos Ferreira  
**Students:** Thayná Batista da Silva and Poliana Fontes

## Sources and references

- [LGPD, Law No. 13,709/2018, Brazilian Federal Government](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)
- [ANPD, Data Subject Rights](https://www.gov.br/anpd/pt-br/assuntos/titular-de-dados-1/direito-dos-titulares)
- [ANPD, Data Protection Impact Report, RIPD](https://www.gov.br/anpd/pt-br/canais_atendimento/agente-de-tratamento/relatorio-de-impacto-a-protecao-de-dados-pessoais-ripd)
- [ANVISA, Software as a Medical Device](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2022/software-como-dispositivo-medico-perguntas-e-respostas)
- [W3C, WCAG 2.2](https://www.w3.org/TR/WCAG22/)
- [React Native, Accessibility](https://reactnative.dev/docs/accessibility)
- [Spring Security, Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [MDN, Progressive Web Apps](https://developer.mozilla.org/en-US/docs/Web/Progressive_web_apps/Reference)
- [Brazilian Ministry of Health, Patient Safety](https://www.gov.br/saude/pt-br/composicao/saes/seguranca-do-paciente)

---

**Recife Saudável**, an academic digital health MVP focused on UX, Mobile, client-server architecture, accessibility, security, privacy, and governance.
