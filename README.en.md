<div align="center">

## Language

[Português](README.md) · [English](README.en.md)

<img src="https://github.com/GrupoTP/Sistema-de-Envio-e-Armazenamento-dos-Projetos-ADS/blob/main/Senac_logo.svg.png" width="140" alt="Senac logo" />

# MedCall

**Your health in the palm of your hand.**

Informational pre-screening and appointment scheduling. Integrative Project, 3rd semester, Systems Analysis and Development, Faculdade Senac Pernambuco.

The product was previously called Recife Saudável. The name is MedCall.

> MedCall does not diagnose and does not replace professional care. In an emergency, call 192 (SAMU). That text lives in `ClinicalDisclaimer`.

[Live app](https://medcall.grupotp.polyclub.com.br) · [Requirements V8](https://grupotp.youtrack.cloud/articles/SPM-A-141) · [Figma prototype](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6) · [Pitch](https://www.canva.com/design/DAHb31ads88/bK-cDvsyY1i6zaBu8-vvZA/edit)

</div>

## What this repository contains

The interface is Kotlin with React Native. The same code runs as a PWA and as an Android app. The Expo shell in `medcall/client/expo` only registers the Kotlin component. In the browser, Metro swaps `react-native` for `react-native-web`.

The server is Kotlin/JVM 21, with Ktor, Koin, and Exposed. Development and tests use H2. Production uses MySQL. Delivery is GitOps: images on ghcr.io, Kubernetes namespace `medcall`, ArgoCD, and Traefik.

Modules, offline behavior, clinic time zones, and environment variables are in [medcall/README.md](medcall/README.md). This file is the repository front page.

## What the app does now

- Welcome, registration, and login.
- Informational pre-screening. No diagnosis.
- Scheduling in three steps: specialty and professional, date and time, confirmation.
- Booked appointments stay visible offline. Booking needs a connection. The server checks the slot again, and a unique index blocks two bookings of the same slot.
- Account: profile, notification consent, password change, and a deletion request.
- Privacy policy screen.

The clinic dashboard with AI risk pre-analysis is specified and is not in this code. Only the clinic and the professional should see the level. The patient should not. The professional decides. Spec: [SPM-A-139](https://grupotp.youtrack.cloud/articles/SPM-A-139/Pre-analise-de-risco-Feita-por-AI). Brazilian Ministry of Health and CFM rules require risk classification at reception and do not mandate one protocol. Manchester is the reference. [Ministry publication](https://bvsms.saude.gov.br/bvs/publicacoes/acolhimento_classificaao_risco_servico_urgencia.pdf).

## API

19 routes under `/api/v1`. Patient routes require `Authorization: Bearer`. The token is an HS256 JWT (`JWT_SECRET`, at least 32 bytes). The server stores only the session id and revokes it on logout.

| Area | Routes |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout` |
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

The development client opens at `http://localhost:8081`. The server answers at `http://localhost:8080/healthz`. Do not commit `JWT_SECRET`, database passwords, or `.env` files.

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
