# Recife Saudável

Esqueleto Kotlin Multiplatform do app. Esta etapa só sobe os três módulos. As telas do protótipo, o manifesto, o service worker e o banco ficam para as tarefas seguintes.

## Módulos

- `recife-saudavel-common`: código compartilhado, com kotlinx-serialization. Alvos JVM e JavaScript.
- `recife-saudavel-server`: backend Kotlin/JVM com Ktor, na porta 8080.
- `recife-saudavel-app`: cliente Kotlin/JS com Kotlin React, na porta 8081.

## Pré-requisitos

- JDK 21

## Comandos

Na pasta `recife-saudavel`:

```bash
./gradlew :recife-saudavel-common:allTests
./gradlew :recife-saudavel-server:test
./gradlew :recife-saudavel-server:run
```

O servidor responde em `http://localhost:8080/healthz` e `http://localhost:8080/readyz`.

```bash
./gradlew :recife-saudavel-app:jsBrowserDevelopmentRun
```

O cliente abre em `http://localhost:8081`.
