# Recife Saudável

App Kotlin Multiplatform do Recife Saudável. A interface é escrita só em Kotlin, com React Native, e o mesmo código roda como PWA no navegador e como app Android. O shell Expo em `client/expo` só registra o componente Kotlin; no navegador o Metro troca `react-native` por `react-native-web`.

Na web, o `sw.js` inicia o bundle Kotlin do worker, que guarda a interface em cache e, no IndexedDB, o cadastro autenticado e as consultas dele. No Android não existe service worker: os mesmos dados ficam em SQLite (expo-sqlite), pelo mesmo gateway Kotlin.

## Agendamento e modo offline

O botão Agendar abre o fluxo do protótipo em três etapas (especialidade e profissional, data e horário, confirmação). Especialidades, profissionais e horários livres vêm sempre do servidor, que confere tudo de novo ao confirmar; um índice único por profissional, data e horário impede que a mesma vaga seja marcada duas vezes. Os horários são os dias úteis dos próximos 14 dias.

Cada profissional atende numa clínica, e cada clínica tem o seu fuso (IANA, como `America/Manaus`): datas e horários das consultas são o relógio da clínica, nunca o do servidor ou do aparelho. Quando o aparelho está em outro fuso, o app mostra o horário da clínica com o equivalente no horário do paciente (`09:00 em Manaus · 10:00 no seu horário`). Se a especialidade escolhida é oferecida em clínicas de fusos diferentes, o paciente escolhe a clínica antes do horário. Profissionais e disponibilidade citam a clínica pelo id (`clinicId`), e a lista de clínicas vem uma vez só de `/api/v1/clinics`. O catálogo de exemplo põe Psiquiatria em Recife e em Manaus para exercitar isso.

O agendamento só funciona com internet. Sem conexão, o cartão Agendar aparece como indisponível e o fluxo nunca confirma uma consulta que o servidor não gravou. As consultas já marcadas continuam visíveis offline, na tela inicial e em Minhas consultas: cada lista recebida do servidor substitui a cópia guardada no aparelho, que só guarda as consultas da última conta conectada.

As rotas de paciente (`/api/v1/specialties`, `/professionals`, `/clinics`, `/availability` e `/appointments`) exigem `Authorization: Bearer <token>`. O login e o cadastro devolvem esse token em `sessionToken`, junto com a conta. Ele é um JWT assinado com HS256 pela chave `JWT_SECRET`, com o usuário em `sub` e o id da sessão em `jti`; o servidor guarda só esse id e confere em toda requisição, pelo plugin de autenticação do Ktor, se a sessão continua aberta. Por isso o JWT não tem `exp`: logout e revogação valem na hora, e a sessão não expira enquanto o app é usado: ela só cai depois de `SESSION_IDLE_DAYS` dias sem nenhuma requisição (padrão 180; `0` desliga a expiração). Trocar a senha pode encerrar as sessões dos outros aparelhos (o app pergunta), e pedir a exclusão da conta encerra todas. Se o servidor recusar o token, o app pede para entrar de novo e apaga o banco local do aparelho. Só fica a fila de logouts feitos offline, enviada ao servidor quando a conexão volta.

As senhas são guardadas com PBKDF2-HMAC-SHA256 (600.000 iterações), no formato `pbkdf2-sha256$<iterações>$<salt>$<hash>`. Bancos criados antes desse formato precisam ser apagados (`server/data`), porque as contas antigas não conseguem mais entrar.

## Módulos

- `common`: código compartilhado, com kotlinx-serialization. Alvos JVM e JavaScript.
- `server`: backend Kotlin/JVM com Ktor, na porta 8080.
- `client/app`: interface Kotlin/JS com React Native (wrappers do `grupotp-kotlin-react-native`), casos de uso e gateways de cada plataforma. Os testes unitários rodam em Node; o `test-support/native-module-stubs.js` troca os módulos do React Native e do Expo por stubs, porque eles só existem no `client/expo`.
- `client/worker`: service worker em Kotlin/JS, carregado por `sw.js`.
- `client/expo`: shell Expo (Metro) que empacota o bundle Kotlin para a web e para o Android.
- `build-logic`: plugins Gradle do próprio repositório, incluídos via `pluginManagement`. `recifesaudavel.dropbox-ignore` (settings) tira as pastas geradas do Dropbox; `recifesaudavel.ktor-dev-mode` adiciona a task `:server:runDev`.
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

O servidor responde em `http://localhost:8080/healthz` (processo no ar) e `http://localhost:8080/readyz` (pronto para atender, o que inclui o banco responder). O `/healthz` não consulta o banco, para que uma queda do banco não faça o servidor ser reiniciado à toa.

O banco padrão é um arquivo H2 em `data/recife-saudavel.mv.db`, na pasta de execução do servidor. Para apontar outro lugar, defina `DATABASE_JDBC_URL`, `DATABASE_DRIVER`, `DATABASE_USER` e `DATABASE_PASSWORD`.

O servidor não sobe sem `JWT_SECRET`, a chave que assina os tokens de sessão (no mínimo 32 bytes; gere uma com `openssl rand -base64 48`). As tasks `:server:run`, `:server:runDev` e `:server:test` usam uma chave pública de desenvolvimento quando a variável não está definida. Trocar a chave invalida todos os tokens, e as pessoas precisam entrar de novo.

O navegador só pode chamar a API a partir das origens em `CORS_ALLOWED_ORIGINS`, separadas por vírgula (padrão: `http://localhost:8081`). O app Android não envia origem e não depende dessa lista.

O cliente usa `API_BASE_URL` como endereço do servidor, lida pelo `client/expo/app.config.js` quando o Metro sobe ou quando o app é exportado. Sem ela, o cliente chama a porta 8080 da máquina que serve o bundle. Exemplo de build para outro ambiente:

```bash
API_BASE_URL=https://api.exemplo.com ./gradlew :client:expo:exportWeb
```

#### Servidor em desenvolvimento

```bash
./gradlew :server:runDev
```

Sobe o servidor no [modo de desenvolvimento do Ktor](https://ktor.io/docs/server-development-mode.html) e, ao lado, um segundo build Gradle contínuo (`gradlew -t :server:classes`) que recompila `server` e `common` a cada arquivo salvo. As linhas desse build aparecem com o prefixo `[watch]`, inclusive os erros de compilação. O Ktor recarrega as classes das pastas listadas em `ktor.deployment.watch` (`application.yaml`) na primeira requisição depois da mudança, sem reiniciar o processo. Por isso a `runDev` põe no classpath as classes compiladas de `common` no lugar do `common-jvm.jar`: o Ktor só recarrega classes que vêm de pastas. Mudanças no `application.yaml` ou nas dependências ainda pedem reiniciar a task, porque a configuração só é lida quando o servidor sobe. Para parar, use Ctrl+C: o build contínuo para junto. Os dois builds usam um daemon Gradle cada.

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

Gera a versão de produção em `client/expo/dist` (`expo export --platform web`) e a serve em `http://localhost:8081` (`expo serve`). Não tem recarga automática: depois de mudar o código, rode a task de novo. Para testar offline, abra a página (ou a PWA instalada) uma vez com a task rodando, para o service worker guardar a interface, e então pare a task. Para ver as consultas offline, entre na conta e abra a tela inicial com o `:server:run` rodando, para o app guardar a lista; depois pare o servidor também. O painel de navegador embutido do app Claude não registra service workers: teste no Chrome ou no Edge.

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

### Dropbox

Se o clone estiver dentro de uma pasta do Dropbox, o Gradle marca as pastas geradas para o Dropbox ignorar, a cada execução e antes de qualquer task: `build/`, `.gradle/` e `.kotlin/` de cada build, `client/expo/node_modules`, `.expo`, `dist` e `android/`, e `server/data` (o banco H2 de desenvolvimento). Assim o código não commitado continua sincronizado entre computadores, mas cada um gera e instala as próprias saídas, e o Dropbox não sincroniza gigabytes de arquivos gerados, não trava arquivos no meio de um build nem copia o banco enquanto o servidor escreve. As pastas que somem com `clean`, `npm ci` ou `expo export` são marcadas de novo ao fim dessas tasks. A marcação é o atributo `com.dropbox.ignored` (stream NTFS no Windows, atributo estendido no Linux e no macOS), feita em `gradle/dropbox-ignore.settings.gradle.kts`.

No Windows, a marcação impede o envio, mas o Dropbox continua transformando cada arquivo criado dentro dessas pastas em arquivo de nuvem (ponto de nova análise com a marca `0x9000201a`). Como as pastas marcadas não são arquivos de nuvem, o Windows não esconde essa marca de quem lista o conteúdo delas, e o Node informa cada item de `node_modules` e `.expo` como link simbólico. O crawler do Metro pula links, por isso o build falhava com `Unable to resolve module` ou `EINVAL: readlink`. O `metro.config.js` corrige o tipo desses itens: um arquivo de nuvem não é link (o `readlink` falha com `EINVAL`), então vale o tipo que o `lstat` informa. Além disso, o `exportWeb` apaga `dist` pelo Gradle antes do `expo export`, porque o Expo tenta apagar a pasta uma vez só e falha com `EBUSY` enquanto o Dropbox ainda segura os arquivos recém-apagados. O Gradle tenta de novo até conseguir.
