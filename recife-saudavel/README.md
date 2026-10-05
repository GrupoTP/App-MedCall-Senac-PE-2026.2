# Recife Saudável

Esqueleto Kotlin Multiplatform do app. As telas do protótipo, o manifesto e o service worker ficam para as tarefas seguintes.

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

O banco padrão é um arquivo H2 em `data/recife-saudavel.mv.db`, na pasta de execução do servidor. Para apontar outro lugar, defina `DATABASE_JDBC_URL`, `DATABASE_DRIVER`, `DATABASE_USER` e `DATABASE_PASSWORD`.

```bash
./gradlew :recife-saudavel-app:jsBrowserDevelopmentRun
```

O cliente abre em `http://localhost:8081`.
