# Recife Saudável

App Kotlin Multiplatform do Recife Saudável. A interface é escrita só em Kotlin, com React Native, e o mesmo código roda como PWA no navegador e como app Android. O shell Expo em `recife-saudavel-client` só registra o componente Kotlin; no navegador o Metro troca `react-native` por `react-native-web`.

Na web, o `sw.js` inicia o bundle Kotlin do worker, que guarda a interface em cache e o cadastro autenticado no IndexedDB. No Android não existe service worker: o cadastro autenticado fica em SQLite (expo-sqlite), pelo mesmo gateway Kotlin.

## Módulos

- `recife-saudavel-common`: código compartilhado, com kotlinx-serialization. Alvos JVM e JavaScript.
- `recife-saudavel-server`: backend Kotlin/JVM com Ktor, na porta 8080.
- `recife-saudavel-app-core`: casos de uso do cliente, sem React. Os testes rodam em Node.
- `recife-saudavel-app`: interface Kotlin/JS com React Native (wrappers do `grupotp-kotlin-react-native`) e gateways de cada plataforma.
- `recife-saudavel-worker`: service worker em Kotlin/JS, carregado por `sw.js`.
- `recife-saudavel-client`: shell Expo (Metro) que empacota o bundle Kotlin para a web e para o Android.
- `grupotp-kotlin-react-native`: submódulo com o fork da biblioteca de wrappers, branch `modern`, incluído via `includeBuild`.

## Pré-requisitos

- JDK 21
- Node.js 24
- Android: Android SDK com CMake 4.1.2 ou mais novo (ver [Android no Windows](#android-no-windows))

Depois de clonar:

```bash
git submodule update --init
cd recife-saudavel-client && npm install
```

## Comandos

Na pasta `recife-saudavel`:

```bash
./gradlew :recife-saudavel-common:allTests
./gradlew :recife-saudavel-server:test
./gradlew :recife-saudavel-app-core:jsNodeTest
./gradlew :recife-saudavel-worker:jsNodeTest
./gradlew :recife-saudavel-server:run
```

O servidor responde em `http://localhost:8080/healthz` e `http://localhost:8080/readyz`.

O banco padrão é um arquivo H2 em `data/recife-saudavel.mv.db`, na pasta de execução do servidor. Para apontar outro lugar, defina `DATABASE_JDBC_URL`, `DATABASE_DRIVER`, `DATABASE_USER` e `DATABASE_PASSWORD`.

### Cliente

O Metro lê o bundle Kotlin já compilado. Gere o bundle da interface e o do service worker antes de abrir o cliente, e de novo depois de mudar código Kotlin:

```bash
./gradlew :recife-saudavel-app:buildClientBundle
```

PWA, na pasta `recife-saudavel-client`:

```bash
npx expo start --web
```

O cliente abre em `http://localhost:8081`, a única origem que o CORS do servidor aceita. Para testar a PWA instalável e o modo offline, gere o export e sirva o `dist` na mesma porta:

```bash
npx expo export --platform web
npx expo serve --port 8081
```

Android, com o aparelho conectado por USB e o servidor rodando:

```bash
adb reverse tcp:8080 tcp:8080
npx expo run:android --variant release
```

O app chama `http://localhost:8080`, que o `adb reverse` leva ao computador. Por isso o `app.json` libera HTTP sem TLS (`usesCleartextTraffic`, via `expo-build-properties`). Isso é só para desenvolvimento; em produção a API deve usar HTTPS e essa opção sai.

O plugin Gradle do React Native não percebe mudanças no bundle Kotlin, que fica fora da pasta do cliente. Depois de rodar `buildClientBundle`, apague `android/app/build/generated/assets/react` antes de compilar o Android, ou o APK sai com o bundle antigo.

### Android no Windows

O código C++ gerado pelo React Native para `react-native-svg` e `react-native-safe-area-context` produz caminhos de arquivo acima de 260 caracteres. O ninja do CMake 3.22.1, que o Android Gradle Plugin usa por padrão, não aceita caminhos assim, e o build falha com `Filename longer than 260 characters` ou `build.ninja still dirty after 100 tries`. Para compilar:

1. Ative caminhos longos no Windows (PowerShell como administrador, depois reinicie):

   ```powershell
   New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force
   ```

2. Instale o CMake 4.1.2 ou mais novo pelo SDK Manager do Android Studio (SDK Tools, Show Package Details). Ele traz o ninja 1.12, que aceita caminhos longos.
3. Aponte o CMake novo em `recife-saudavel-client/android/local.properties`. A pasta `android/` é gerada pelo `expo prebuild`, que apaga esse arquivo, então recrie-o depois de cada prebuild:

   ```properties
   cmake.dir=C\:/Users/<usuario>/AppData/Local/Android/Sdk/cmake/4.1.2
   ```

4. Prefira clonar o repositório num caminho curto, fora de pastas sincronizadas como Dropbox ou OneDrive (por exemplo, `C:\30-codigo-fonte`).
