<div align="center">

## 🌐 LANGUAGE / IDIOMA

### 🇧🇷 **[ PORTUGUÊS ](README.md)** &nbsp;&nbsp;&nbsp; 🇺🇸 **[ ENGLISH ](README_EN.md)**

</div>
<div align="center">

<img src="https://github.com/GrupoTP/Sistema-de-Envio-e-Armazenamento-dos-Projetos-ADS/blob/main/Senac_logo.svg.png" width="140" alt="Logo Senac" />

# Recife Saudável

**Your health in the palm of your hand.**

A healthcare support application for pre-screening and medical appointment scheduling, developed as the Integrative Project for the 3rd semester of the Systems Analysis and Development program at Faculdade Senac Pernambuco.

> **Clinical disclaimer:** Recife Saudável does not provide medical diagnosis and does not replace professional medical care. Information provided during pre-screening is intended to support the organization and preparation of care. In an emergency, seek urgent medical attention immediately.

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
![License](https://img.shields.io/badge/License-Academic-lightgrey?style=for-the-badge)

</div>

<details open>
<summary><h2>📑 Table of Contents</h2></summary>

- [About the project](#about-the-project)
- [Features](#features)
  - [Welcome screen](#welcome-screen)
  - [Registration and account](#registration-and-account)
  - [Profile selection](#profile-selection)
  - [Pre-screening](#pre-screening)
  - [Appointment scheduling](#appointment-scheduling)
  - [Dashboard](#dashboard)
  - [Settings](#settings)
  - [Privacy](#privacy)
  - [Administration](#administration)
- [Required footer](#required-footer)
- [Healthcare and security components](#healthcare-and-security-components)
  - [`ClinicalDisclaimer`](#clinicaldisclaimer)
  - [Privacy and LGPD](#privacy-and-lgpd)
  - [Access control](#access-control)
  - [Audit](#audit)
- [Accessibility](#accessibility)
- [Architecture](#architecture)
- [Technologies](#technologies)
  - [Frontend](#frontend)
  - [Backend](#backend)
  - [Database](#database)
  - [API](#api)
  - [Security](#security)
  - [Quality](#quality)
- [Project structure](#project-structure)
- [Important rules](#important-rules)
- [Main MVP criteria](#main-mvp-criteria)
  - [Registration](#registration)
  - [Pre-screening](#pre-screening-1)
  - [Appointment scheduling](#appointment-scheduling-1)
  - [Settings](#settings-1)
  - [Administration](#administration-1)
  - [Interface](#interface)
- [Academic project](#academic-project)
- [Sources and references](#sources-and-references)
- [🎯 Problem](#-problem)
- [✅ Solution](#-solution)
- [🖼️ Prototypes](#️-prototypes)
- [🌍 ESG Impact](#-esg-impact)
- [📈 Market](#-market)
- [💼 Business Model](#-business-model)
- [🧾 Cost and Feasibility Analysis](#-cost-and-feasibility-analysis)
- [🎯 Target Audience](#-target-audience)
- [🛠️ Technologies Used](#️-technologies-used)
- [🚀 How to Run Locally](#-how-to-run-locally)
  - [Prerequisites](#prerequisites)
  - [Clone](#clone)
  - [Backend](#backend-1)
  - [Frontend](#frontend-1)
- [👥 Team](#-team)
  - [Polyana Fontes](#polyana-fontes)
  - [Thayná Batista da Silva](#thayná-batista-da-silva)
- [📋 Technical Documentation](#-technical-documentation)
  - [1. Problem Definition](#1-problem-definition)
  - [2. Backlog and Requirements Engineering](#2-backlog-and-requirements-engineering)
  - [3. UX/UI for Intelligent Interfaces](#3-uxui-for-intelligent-interfaces)
  - [4. Software Architecture and Development Stack](#4-software-architecture-and-development-stack)
  - [5. Process Report and Prompt Engineering](#5-process-report-and-prompt-engineering)
  - [6. Cost and Feasibility Analysis](#6-cost-and-feasibility-analysis)
  - [7. Future Developments](#7-future-developments)
- [📽️ Demonstrations](#️-demonstrations)
- [📄 License](#-license)
- [📞 Contact](#-contact)
  - [Polyana Fontes](#polyana-fontes-1)
  - [Thayná Batista da Silva](#thayná-batista-da-silva-1)
- [🎓 Academic Information](#-academic-information)

</details>

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

## 🎯 Problem

The project aims to improve the patient's digital journey by centralizing information for informational pre-screening and appointment scheduling in a simple, accessible, and secure experience.

## ✅ Solution

The **Recife Saudável App** is an academic, patient-centered MVP featuring registration, authentication, informational pre-screening, appointment scheduling, appointment management, privacy, security, and PWA support.

The application does not provide diagnosis, prescriptions, or automated clinical decisions.

## 🖼️ Prototypes

<div align="center">

<a href="https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6"><img src="https://img.shields.io/badge/View%20Figma%20Prototype-F24E1E?style=for-the-badge&logo=figma&logoColor=white" alt="View Figma Prototype"></a>

<a href="https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2"><img src="https://img.shields.io/badge/View%20GitHub%20Repository-181717?style=for-the-badge&logo=github&logoColor=white" alt="View GitHub Repository"></a>

</div>

## 🌍 ESG Impact

- **Social:** a simpler and more accessible digital journey for patients.
- **Governance:** privacy, security, auditing, access control, and LGPD.
- **Environmental:** digitization of administrative processes and reduced dependence on physical documents.

## 📈 Market

The proposal is positioned within healthcare service digitalization, focusing on patient journey organization, scheduling, and structured information collection before care.

This is an academic project and does not represent commercial or clinical product validation.

## 💼 Business Model

As a product concept, the solution could be adapted for clinics and healthcare institutions interested in digital appointment management, informational pre-screening, data organization, communications, and governance.

Commercial deployment would require legal, regulatory, technical, security, and market validation.

## 🧾 Cost and Feasibility Analysis

The MVP uses widely adopted open-source technologies, reducing initial development costs. Potential production costs include infrastructure, database, observability, security, domain, certificates, maintenance, support, compliance, and external services.

## 🎯 Target Audience

**Primary audience:** patients who need to organize information and schedule appointments digitally.

**Institutional audience:** clinics, healthcare institutions, and administrative teams.

## 🛠️ Technologies Used

| Layer | Technologies |
|---|---|
| Frontend | React Native, TypeScript/JavaScript, React Native Web |
| PWA | Web App Manifest, Service Worker, HTTPS |
| Backend | Java, Spring Boot, Spring Security |
| Persistence | Spring Data JPA, Hibernate |
| Database | PostgreSQL |
| API | REST, JSON, OpenAPI/Swagger |
| Testing | JUnit, Mockito, integration and accessibility testing |
| Design | Figma |
| Governance | LGPD, RBAC, auditing, and security |

## 🚀 How to Run Locally

### Prerequisites

- Node.js and npm.
- Java JDK compatible with the backend.
- PostgreSQL.
- Git.

### Clone

```bash
git clone https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2.git
cd App-Recife-Saudavel-Senac-PE-2026.2
```

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

Use `.env.example` as a reference and never commit real credentials. Commands may vary according to the final project configuration.

## 👥 Team

### Polyana Fontes

**Full Stack Developer · Database Modeling · Technical Documentation**

Systems Analysis and Development student  
Faculdade Senac Pernambuco, Recife-PE, Brazil

### Thayná Batista da Silva

**Manager / Requirements Analyst · Scrum Master**  
**Product Designer · High-Fidelity Prototyping (Figma)**

Systems Analysis and Development student  
Faculdade Senac Pernambuco, Recife-PE, Brazil  
Class of 2025 · Expected graduation: 2027

## 📋 Technical Documentation

### 1. Problem Definition
Project context, problem, objectives, scope, and constraints.

### 2. Backlog and Requirements Engineering
Functional and non-functional requirements, business rules, acceptance criteria, use cases, and backlog.

### 3. UX/UI for Intelligent Interfaces
Information architecture, flows, prototyping, accessibility, responsiveness, and patient experience.

### 4. Software Architecture and Development Stack
Client-server architecture, REST API, Spring Boot, PostgreSQL, security, PWA, and code organization.

### 5. Process Report and Prompt Engineering
Development process, technical decisions, and responsible use of AI tools.

### 6. Cost and Feasibility Analysis
Estimates, assumptions, infrastructure, and technical and economic feasibility.

### 7. Future Developments
Potential developments include institutional integrations, advanced notifications, observability, expanded accessibility, and healthcare system integrations after technical and regulatory validation.

## 📽️ Demonstrations

- [🎨 View Figma Prototype](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6)
- [💻 View GitHub Repository](https://github.com/GrupoTP/App-Recife-Saudavel-Senac-PE-2026.2)

## 📄 License

This project was developed for academic purposes. The final license must be defined in the repository's `LICENSE` file.

## 📞 Contact

### Polyana Fontes

Full Stack Developer, Database Modeling, and Technical Documentation.

### Thayná Batista da Silva

**Scrum Master · Product Designer · Requirements Analyst**  
Senac Recife-PE, Systems Analysis and Development.

📧 **Email:** [thaynabdstec@gmail.com](mailto:thaynabdstec@gmail.com)  
📱 **Phone:** +55 (81) 97912-6121

## 🎓 Academic Information

**App Name:** 🏫 Recife Saudável App  
**Project started:** August 2026  
**Institution:** Faculdade Senac Pernambuco, Recife-PE, Brazil  
**Program:** Systems Analysis and Development  
**Project:** Integrative Project, 3rd semester  
**Integrative Project faculty advisor:** Prof. Ícaro Santos Ferreira  
**Students:** Thayná Batista da Silva and Polyana Fontes
