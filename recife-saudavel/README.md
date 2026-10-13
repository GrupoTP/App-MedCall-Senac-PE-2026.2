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

O Metro só lê o bundle Kotlin já compilado (`:client:app:buildClientBundle`). As tasks abaixo geram esse bundle e instalam as dependências npm antes de abrir o cliente. O `startWeb` e o `startLan` também acompanham o código Kotlin enquanto rodam: ao salvar um arquivo de `common`, `client/app`, `client/worker` ou do submódulo, o bundle é gerado de novo e o Metro recarrega o app. Para parar, use Ctrl+C.

#### PWA em desenvolvimento

```bash
./gradlew :client:expo:startWeb
```

Sobe o Metro com recarga automática (`expo start --web`). O cliente abre em `http://localhost:8081`, a única origem que o CORS do servidor aceita. Esse modo não funciona offline: o bundle de desenvolvimento vem do Metro a cada carga e o service worker não o guarda.

#### PWA instalável e modo offline

```bash
./gradlew :client:expo:serveWeb
```

Gera a versão de produção em `client/expo/dist` (`expo export --platform web`) e a serve em `http://localhost:8081` (`expo serve`). Não tem recarga automática: depois de mudar o código, rode a task de novo. Para testar offline, abra a página (ou a PWA instalada) uma vez com a task rodando, para o service worker guardar a interface, e então pare a task.

#### Celular na mesma rede Wi‑Fi (Expo Go)

```bash
./gradlew :client:expo:startLan
```

Mostra o QR code e a URL `exp://IP:8081` para abrir no Expo Go. O `expo start` só desenha o QR code quando a saída é um terminal, e sob o Gradle não é; por isso o `scripts/start-metro.js` escolhe o IP da rede como o Expo CLI faz e mostra o QR code. Deixe o `:server:run` rodando: no Expo Go o app chama o servidor no mesmo IP do Metro (`hostUri` do expo-constants), na porta 8080. Na primeira vez, o Firewall do Windows pede para liberar o Java na rede privada.

A PWA não funciona aberta pelo IP da rede (`http://IP:8081`): o navegador só libera service worker em HTTPS ou em `localhost`.

#### Android por USB

```bash
./gradlew :client:expo:runAndroid
```

Com o aparelho conectado e o servidor rodando, faz o `adb reverse tcp:8080 tcp:8080` e instala o build release (`expo run:android --variant release`). A task acha o `adb` no Android SDK (`ANDROID_HOME`, `ANDROID_SDK_ROOT`, `sdk.dir` do `local.properties` ou a pasta padrão do Android Studio); se não achar, usa o do `PATH`.

No build release o app chama `http://localhost:8080`, que o `adb reverse` leva ao computador. Por isso o `app.json` libera HTTP sem TLS (`usesCleartextTraffic`, via `expo-build-properties`). Isso é só para desenvolvimento; em produção a API deve usar HTTPS e essa opção sai.

A pasta `android/` é gerada pelo `expo prebuild` e não vai para o git. Ajustes nativos ficam no config plugin `plugins/withKotlinAndroidBuild.js`, registrado no `app.json`. Ele faz a task de bundle do React Native acompanhar o bundle Kotlin, que fica fora da pasta do cliente, para o APK sempre levar o bundle novo.

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
