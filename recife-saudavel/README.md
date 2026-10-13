# Recife Saudável

App Kotlin Multiplatform do Recife Saudável. A interface é escrita só em Kotlin, com React Native, e o mesmo código roda como PWA no navegador e como app Android. O shell Expo em `client/expo` só registra o componente Kotlin; no navegador o Metro troca `react-native` por `react-native-web`.

Na web, o `sw.js` inicia o bundle Kotlin do worker, que guarda a interface em cache e o cadastro autenticado no IndexedDB. No Android não existe service worker: o cadastro autenticado fica em SQLite (expo-sqlite), pelo mesmo gateway Kotlin.

## Módulos

- `common`: código compartilhado, com kotlinx-serialization. Alvos JVM e JavaScript.
- `server`: backend Kotlin/JVM com Ktor, na porta 8080.
- `client/app`: interface Kotlin/JS com React Native (wrappers do `grupotp-kotlin-react-native`), casos de uso e gateways de cada plataforma. Os testes unitários rodam em Node; o `test-support/native-module-stubs.js` troca os módulos do React Native e do Expo por stubs, porque eles só existem no `client/expo`.
- `client/worker`: service worker em Kotlin/JS, carregado por `sw.js`.
- `client/expo`: shell Expo (Metro) que empacota o bundle Kotlin para a web e para o Android.
- `grupotp-kotlin-react-native`: submódulo com o fork da biblioteca de wrappers, branch `modern`, incluído via `includeBuild`.

## Pré-requisitos

- JDK 21
- Node.js 24
- Android: Android SDK com CMake 4.1.2 ou mais novo (ver [Android no Windows](#android-no-windows))

Depois de clonar não há passo manual. O primeiro `./gradlew` baixa o submódulo (`git submodule update --init`), pelo `settings.gradle.kts`: o build incluído precisa existir antes de qualquer task rodar. As dependências npm do `client/expo` ficam com a task `:client:expo:npmInstall` (`npm ci`), que as tasks do cliente chamam sozinhas. Ela só roda quando `package.json` ou `package-lock.json` mudam ou a pasta `node_modules` some; fora isso fica `UP-TO-DATE`.

No Windows, use `gradlew` no lugar de `./gradlew`.

## Comandos

Na pasta `recife-saudavel`:

```bash
./gradlew :common:allTests
./gradlew :server:test
./gradlew :client:app:jsNodeTest
./gradlew :client:worker:jsNodeTest
./gradlew :server:run
```

O servidor responde em `http://localhost:8080/healthz` e `http://localhost:8080/readyz`.

O banco padrão é um arquivo H2 em `data/recife-saudavel.mv.db`, na pasta de execução do servidor. Para apontar outro lugar, defina `DATABASE_JDBC_URL`, `DATABASE_DRIVER`, `DATABASE_USER` e `DATABASE_PASSWORD`.

### Cliente

O Metro lê o bundle Kotlin já compilado. As tasks do `:client:expo` geram antes o bundle da interface e o do service worker (`:client:app:buildClientBundle`) e instalam as dependências npm. Se o Metro já estiver aberto, rode `buildClientBundle` de novo depois de mudar código Kotlin.

PWA (`npx expo start --web`):

```bash
./gradlew :client:expo:startWeb
```

O cliente abre em `http://localhost:8081`, a única origem que o CORS do servidor aceita. Para testar a PWA instalável e o modo offline, gere o export e sirva o `dist` na mesma porta (`npx expo export --platform web` e `npx expo serve --port 8081`):

```bash
./gradlew :client:expo:serveWeb
```

Android, com o aparelho conectado por USB e o servidor rodando (`adb reverse tcp:8080 tcp:8080` e `npx expo run:android --variant release`; o `adb` precisa estar no `PATH`):

```bash
./gradlew :client:expo:runAndroid
```

O app chama `http://localhost:8080`, que o `adb reverse` leva ao computador. Por isso o `app.json` libera HTTP sem TLS (`usesCleartextTraffic`, via `expo-build-properties`). Isso é só para desenvolvimento; em produção a API deve usar HTTPS e essa opção sai.

A pasta `android/` é gerada pelo `expo prebuild` e não vai para o git. Ajustes nativos ficam no config plugin `plugins/withKotlinAndroidBuild.js`, registrado no `app.json`. Ele faz a task de bundle do React Native acompanhar o bundle Kotlin, que fica fora da pasta do cliente: depois de rodar `buildClientBundle`, o próximo build Android já empacota o bundle novo.

### Android no Windows

O código C++ gerado pelo React Native para `react-native-svg` e `react-native-safe-area-context` produz caminhos de arquivo acima de 260 caracteres. O ninja do CMake 3.22.1, que o Android Gradle Plugin usa por padrão, não aceita caminhos assim, e o build falha com `Filename longer than 260 characters` ou `build.ninja still dirty after 100 tries`. Para compilar:

1. Ative caminhos longos no Windows (PowerShell como administrador, depois reinicie):

   ```powershell
   New-ItemProperty -Path "HKLM:\SYSTEM\CurrentControlSet\Control\FileSystem" -Name "LongPathsEnabled" -Value 1 -PropertyType DWORD -Force
   ```

2. Instale o CMake 4.1.2 ou mais novo pelo SDK Manager do Android Studio (SDK Tools, Show Package Details). Ele traz o ninja 1.12, que aceita caminhos longos. No Windows, o `expo prebuild` grava em `android/local.properties` o `cmake.dir` do CMake mais novo (4.1.2 ou acima) que encontrar em `ANDROID_HOME/cmake`. Se o arquivo já tiver um `cmake.dir`, ele é mantido.
3. Se o clone estiver numa pasta do Dropbox, marque as saídas de build (`build/`, `.gradle/`, `.kotlin/`, `android/`, `.cxx/`) para o Dropbox ignorar. Assim ele não sincroniza gigabytes de arquivos gerados nem trava arquivos no meio do build. O script cria as pastas que faltam e grava o atributo `com.dropbox.ignored`. O `expo prebuild` recria `android/` e o config plugin marca a pasta de novo; já o `gradlew clean` apaga as pastas `build/`, então rode o script de novo depois dele:

   ```powershell
   powershell -ExecutionPolicy Bypass -File scripts\dropbox-ignore.ps1
   ```
