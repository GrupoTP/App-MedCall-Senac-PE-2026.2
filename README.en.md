<div align="center">

## Language

[Português](README.md) · [English](README.en.md) · [Español](README.es.md)

<img src="medcall/client/expo/assets/brand/medcall-logo.svg" width="360" alt="MedCall" />

<img src="https://github.com/GrupoTP/Sistema-de-Envio-e-Armazenamento-dos-Projetos-ADS/blob/main/Senac_logo.svg.png" width="100" alt="Senac logo" />

# MedCall

**Your health in the palm of your hand.**

Informational pre-screening and appointment scheduling. Integrative Project, 3rd semester, Systems Analysis and Development, Faculdade Senac Pernambuco.

The product was previously called Recife Saudável. The name is MedCall.

All rights reserved. Use of this code is limited to Polyana Fontes and Thayná Batista da Silva. The terms are in [LICENSE](LICENSE).

![Suite](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml/badge.svg)
<!-- test-badge:start -->
![Tests](https://img.shields.io/badge/tests-132%20failed-B42318)
<!-- test-badge:end -->

> MedCall does not diagnose and does not replace professional care. In an emergency, call 192 (SAMU). That text lives in `ClinicalDisclaimer`.

[Live app](https://medcall.grupotp.polyclub.com.br) · [Requirements V8](https://grupotp.youtrack.cloud/articles/SPM-A-141) · [Figma prototype](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6) · [Pitch](https://www.canva.com/design/DAHb31ads88/bK-cDvsyY1i6zaBu8-vvZA/edit)

</div>

## Test account

This account exists on the [live app](https://medcall.grupotp.polyclub.com.br). It does not hold real patient data.

- E-mail: `teste@medcall.grupotp.polyclub.com.br`
- Password: `MedCall2026`

## What it is for

MedCall is the patient app. It lets a person describe how they feel before the appointment and book a time with a professional. The report stays on the account. The booked appointment does too.

The app does not name a disease and does not replace a health professional. In an emergency, the on-screen notice says to call 192 (SAMU).

## How to use it

1. Open the [live app](https://medcall.grupotp.polyclub.com.br) in a browser, or open the Android app.
2. Create an account, or sign in with an existing one. Forgot password sends a code by e-mail when `MAIL_HOST` and `MAIL_FROM` are set. The code lasts 30 minutes, works once, and the answer is the same when the e-mail is unknown.
3. In pre-screening, describe how you feel. A draft can be resumed. That report is not a diagnosis.
4. Tap schedule. Choose a specialty and a professional, then a date and a time, and confirm. The server checks the slot again.
5. Open the appointments already booked. Without internet, that list stays on the screen. Booking another appointment needs a connection.
6. In the account, update the profile, notification consent, or password, or request data deletion.
7. The accessibility button (AccessAbility Widget) opens with a click or Alt+A on the web, and with the same button on Android and iOS. The version is the one in `medcall/client/accessability-widget.version`. Changing that number without updating web, Android, and iOS makes the GitHub test fail. On Android and iOS, contrast, type size, and reading from that panel apply to the widget document. The Kotlin interface keeps the app theme.

## Interface resources

The interface is Kotlin using React Native components. The Expo shell in `medcall/client/expo` only registers that component. It is not the old site inside a WebView.

| Resource | Where | Role |
|---|---|---|
| Screen navigation | `AppScreen` in `App.kt` | Welcome, register, login, home, pre-screening, schedule, appointments, account, and privacy. State is `useState`. |
| Menu | `FloatingMenu.kt` | Opens over a signed-in screen and goes to home, pre-screening, schedule, appointments, and account. |
| ScrollView | Register, schedule, appointments, account, and privacy | Scrolls the content. |
| TextInput | `TextField.kt` | Register, login, and schedule fields. |
| Pressable | Buttons and links | Taps. |
| Modal | `ConfirmDialog.kt` | Confirms account actions. |
| Icons | `ui/icon/Icons.kt` | Menu, appointments, account, and sign-out. |

## With and without internet

On the PWA, the interface opens again offline after the first visit that installed the service worker. Sending new data needs the server.

| Screen | Offline |
|---|---|
| Welcome, login, and register | The screen opens. Signing in or creating an account needs the server. |
| Home and appointments | The list already received stays. |
| Schedule and pre-screening | They need the network. The app says so and does not confirm what the server did not store. |
| Account | A saved profile can be read. Saving the profile, the password, or a deletion needs the network. |

## Data on the device

On the web, IndexedDB is named `medcall`, version 2, with stores `accounts` and `appointments`. On Android the same data is in SQLite. The password is not stored on the device. Source: `LocalAccountDatabase.kt`.

| Data | Where | When it leaves |
|---|---|---|
| Signed-in profile | key `signed-in` | Sign-out, or when the server refuses the session. |
| Appointments of the last account | store `appointments`, keyed by account id | A new list replaces the old one. Sign-out clears the store. |
| Offline sign-out queue | key `pending-sign-outs` | It survives the wipe and is sent when the network returns. |

The device has no separate expiry. The server ends the session after `SESSION_IDLE_DAYS` without a request. The default is 180 days.

## Tests run

<!-- test-results:start -->
Last run on 25 November 2026, with the command in [Run](#run).

| Result | Count |
|---|---|
| Tests run | 132 |
| Failures | 1 |
| Errors | 0 |
| Skipped | 0 |

The numbers come from the Gradle XML reports under `common`, `server`, `client/app`, and `client/worker`. `common` tests are counted twice when they run on the JVM and on JavaScript. The [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) workflow writes this table and a row in [TESTES.md](TESTES.md) after every run on main, whether it passes or fails. Record: https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45655652705.
<!-- test-results:end -->

## What this repository contains

The interface is Kotlin with React Native. The same code runs as a PWA and as an Android app. The Expo shell in `medcall/client/expo` only registers the Kotlin component. In the browser, Metro swaps `react-native` for `react-native-web`.

The server is Kotlin/JVM 21, with Ktor, Koin, and Exposed. Development and tests use H2. Production uses MySQL. Delivery is GitOps: images on ghcr.io, Kubernetes namespace `medcall`, ArgoCD, and Traefik.

Modules, offline behavior, clinic time zones, and environment variables are in [medcall/README.md](medcall/README.md). This file is the repository front page.

## What the app does now

- Welcome, registration, login, and a one-time code to set a new password.
- Informational pre-screening. No diagnosis.
- Scheduling in three steps: specialty and professional, date and time, confirmation.
- Booked appointments stay visible offline. Booking needs a connection. The server checks the slot again, and a unique index blocks two bookings of the same slot.
- Account: profile, notification consent, password change, and a deletion request.
- Privacy policy screen.

The clinic dashboard with AI risk pre-analysis is specified and is not in this code. Only the clinic and the professional should see the level. The patient should not. The professional decides. Spec: [SPM-A-139](https://grupotp.youtrack.cloud/articles/SPM-A-139/Pre-analise-de-risco-Feita-por-AI). Brazilian Ministry of Health and CFM rules require risk classification at reception and do not mandate one protocol. Manchester is the reference. [Ministry publication](https://bvsms.saude.gov.br/bvs/publicacoes/acolhimento_classificaao_risco_servico_urgencia.pdf).

## API

21 routes under `/api/v1`. Patient routes require `Authorization: Bearer`. The token is an HS256 JWT (`JWT_SECRET`, at least 32 bytes). The server stores only the session id and revokes it on logout. A reset with the code also ends that account's sessions.

| Area | Routes |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/password-recovery`, `POST /auth/password-recovery/confirm`, `GET /auth/me`, `POST /auth/logout` |
| Account | `GET /account`, `PUT /account`, `PUT /account/consents`, `POST /account/password`, `POST /account/deletion-request` |
| Pre-screening | `GET /pre-triages/draft`, `GET /pre-triages/pending`, `POST /pre-triages` |
| Scheduling | `GET /specialties`, `GET /professionals`, `GET /clinics`, `GET /availability`, `GET /appointments`, `POST /appointments`, `POST /appointments/{id}/cancel` |

Outside the API: `GET /healthz` (process up, database not queried) and `GET /readyz` (database answers).

Passwords use PBKDF2-HMAC-SHA256, 600,000 iterations. Format source: [medcall/README.md](medcall/README.md).

## Architecture

```text
Kotlin/JS + React Native client (PWA and Android)
        |
        | REST /api/v1
        v
Ktor server
  controller  ->  service  ->  repository (Exposed)
        |
        v
H2 in development, MySQL in production
```

Business rules stay in the service. The controller receives the request and returns the response.

## Layout

```text
medcall/
  common/          shared, JVM and JavaScript
  server/          Ktor, port 8080
  client/app/      interface and use cases
  client/worker/   service worker
  client/expo/     Expo shell
  docker/          images
k8s/               medcall namespace, ingress, MySQL
argocd/            GitOps Application
.github/workflows/ image publish to ghcr.io
```

Production: [https://medcall.grupotp.polyclub.com.br](https://medcall.grupotp.polyclub.com.br). The legacy host `recifesaudavel.grupotp.polyclub.com.br` still serves the API. Other paths redirect to the new host. See `k8s/ingress.yaml`.

## Run

Prerequisites, from [medcall/README.md](medcall/README.md): JDK 21 and Node.js 24. The first `./gradlew` initializes the submodule.

```bash
git clone --recurse-submodules https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2.git
cd App-MedCall-Senac-PE-2026.2/medcall
./gradlew :server:run
./gradlew :client:expo:startWeb
```

The development client opens at `http://localhost:8081`. The server answers at `http://localhost:8080/healthz`. Do not commit `JWT_SECRET`, database passwords, mail passwords, or `.env` files. The recovery code is sent by SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Without that, the route says the code cannot be sent. Tests set `MAIL_MODE=capture` and do not open a mail connection.

Tests:

```bash
./gradlew :common:allTests :server:test :client:app:jsNodeTest :client:worker:jsNodeTest
```

## Team

Faculdade Senac Pernambuco, Recife-PE, Brazil. Systems Analysis and Development. Integrative Project, August to December 2026. Faculty advisor: Ícaro Santos Ferreira.

- Thayná Batista da Silva: Scrum Master, requirements, prototyping, and development.
- Polyana Fontes: database modeling, documentation, and development.

Thayná: [thaynabdstec@gmail.com](mailto:thaynabdstec@gmail.com).

## Sources

- Code and commands: [medcall/README.md](medcall/README.md) and `medcall/server/src/main/kotlin/grupotp/medcall/server/controller/`.
- Current requirements: [SPM-A-141](https://grupotp.youtrack.cloud/articles/SPM-A-141). V7 is marked superseded at [SPM-A-133](https://grupotp.youtrack.cloud/articles/SPM-A-133).
- [LGPD, Law No. 13,709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).
- [Ktor development mode](https://ktor.io/docs/server-development-mode.html).
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), the project's accessibility requirement. Screen-reader test coverage is not finished in this repository.
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html): one-time code, the same answer for an unknown e-mail, and sessions ended after the reset.
