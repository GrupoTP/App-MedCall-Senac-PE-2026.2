# Recife Saudável

App Kotlin Multiplatform do Recife Saudável. O manifesto e o `sw.js` fazem parte da PWA. O `sw.js` só inicia o bundle Kotlin do worker, que guarda a interface em cache e o cadastro autenticado no banco local.

## Módulos

- `recife-saudavel-common`: código compartilhado, com kotlinx-serialization. Alvos JVM e JavaScript.
- `recife-saudavel-server`: backend Kotlin/JVM com Ktor, na porta 8080.
- `recife-saudavel-app`: cliente Kotlin/JS com Kotlin React, na porta 8081.
- `recife-saudavel-worker`: service worker em Kotlin/JS, carregado por `sw.js`.

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
