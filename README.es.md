<div align="center">

## Idioma

[Português](README.md) · [English](README.en.md) · [Español](README.es.md)

<img src="medcall/client/expo/assets/brand/medcall-logo.svg" width="360" alt="MedCall" />

<img src="https://github.com/GrupoTP/Sistema-de-Envio-e-Armazenamento-dos-Projetos-ADS/blob/main/Senac_logo.svg.png" width="100" alt="Logo Senac" />

# MedCall

**Tu salud en la palma de la mano.**

Aplicación de pretriaje informativo y de reserva de consultas. Proyecto integrador del 3.er período de Análisis y Desarrollo de Sistemas de la Faculdade Senac Pernambuco.

Antes se llamaba Recife Saudável. El nombre del producto es MedCall.

Todos los derechos reservados. El uso de este código queda limitado a Polyana Fontes y Thayná Batista da Silva. El texto está en [LICENSE](LICENSE).

![Suite](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml/badge.svg)
<!-- test-badge:start -->
![Pruebas](https://img.shields.io/badge/pruebas-132%20ok-1E6B5E)
<!-- test-badge:end -->

> MedCall no hace diagnóstico médico y no sustituye la atención profesional. En una emergencia, llama al 192 (SAMU). Ese texto está en el componente `ClinicalDisclaimer`.

[Sistema en línea](https://medcall.grupotp.polyclub.com.br) · [Requisitos V8](https://grupotp.youtrack.cloud/articles/SPM-A-141) · [Prototipo en Figma](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6) · [Pitch](https://www.canva.com/design/DAHb31ads88/bK-cDvsyY1i6zaBu8-vvZA/edit)

</div>

## Cuenta de prueba

Esta cuenta existe en el [sistema en línea](https://medcall.grupotp.polyclub.com.br). No guarda datos reales de un paciente.

- Correo: `teste@medcall.grupotp.polyclub.com.br`
- Contraseña: `MedCall2026`

## Para qué sirve

MedCall es la aplicación del paciente. Sirve para que la persona cuente, antes de la consulta, lo que siente y para reservar un horario con un profesional. El relato queda en la cuenta. La consulta reservada también.

La aplicación no dice el nombre de una enfermedad y no sustituye al profesional de salud. En una emergencia, el aviso de la pantalla pide llamar al 192 (SAMU).

## Cómo usarla

1. Abre el [sistema en línea](https://medcall.grupotp.polyclub.com.br) en el navegador, o la aplicación Android.
2. Crea una cuenta, o entra con la cuenta que ya existe. En Olvidé mi contraseña, el servidor manda un código por correo cuando `MAIL_HOST` y `MAIL_FROM` están definidos. El código vale 30 minutos, sirve una sola vez, y la respuesta es la misma si el correo no existe.
3. En el pretriaje, describe lo que sientes. El borrador se puede retomar. Ese relato no es un diagnóstico.
4. Toca reservar. Elige especialidad y profesional, después fecha y horario, y confirma. El servidor vuelve a comprobar el cupo.
5. Abre las consultas ya reservadas. Sin internet, esa lista sigue en la pantalla. Reservar otra consulta solo funciona con conexión.
6. En la cuenta, actualiza el perfil, el consentimiento de notificaciones, la contraseña, o pide la eliminación de los datos.
7. El botón de accesibilidad (AccessAbility Widget) se abre con un clic o con Alt+A en la web, y con el mismo botón en Android y en iOS. La versión es la del archivo `medcall/client/accessability-widget.version`. Cambiar ese número sin actualizar la web, Android e iOS hace fallar la prueba de GitHub. En Android y en iOS, el contraste, la fuente y la lectura de ese panel valen para el documento del widget. La interfaz en Kotlin sigue el tema de la aplicación.

## Recursos de la interfaz

La interfaz es Kotlin con componentes de React Native. El shell Expo en `medcall/client/expo` solo registra ese componente. No es el sitio antiguo dentro de una WebView.

| Recurso | Dónde | Función |
|---|---|---|
| Navegación entre pantallas | `AppScreen` en `App.kt` | Bienvenida, registro, acceso, inicio, pretriaje, reserva, consultas, cuenta y política. El estado está en `useState`. |
| Menú | `FloatingMenu.kt` | Se abre sobre la pantalla con sesión y va a inicio, pretriaje, reserva, consultas y cuenta. |
| ScrollView | Registro, reserva, consultas, cuenta y política | Desplazamiento del contenido. |
| TextInput | `TextField.kt` | Campos de registro, acceso y reserva. |
| Pressable | Botones y enlaces | Toque. |
| Modal | `ConfirmDialog.kt` | Confirmación de acciones de la cuenta. |
| Iconos | `ui/icon/Icons.kt` | Menú, consultas, cuenta y salida. |

## Con y sin internet

En la PWA, la interfaz vuelve a abrirse sin red después de la primera visita con el service worker. Enviar datos nuevos exige el servidor.

| Pantalla | Sin red |
|---|---|
| Bienvenida, acceso y registro | La pantalla se abre. Entrar o crear una cuenta exige el servidor. |
| Inicio y consultas | La lista ya recibida sigue. |
| Reserva y pretriaje | Exigen red. La aplicación avisa y no confirma lo que el servidor no guardó. |
| Cuenta | El perfil ya guardado se puede leer. Guardar perfil, contraseña o eliminación exige red. |

## Datos en el aparato

En la web, IndexedDB se llama `medcall`, versión 2, con las stores `accounts` y `appointments`. En Android los mismos datos están en SQLite. La contraseña no se guarda en el aparato. Fuente: `LocalAccountDatabase.kt`.

| Dato | Dónde | Cuándo sale |
|---|---|---|
| Perfil de la cuenta | clave `signed-in` | Salida, o cuando el servidor rechaza la sesión. |
| Consultas de la última cuenta | store `appointments`, clave del id de la cuenta | La lista nueva sustituye la anterior. La salida borra la store. |
| Cola de salida hecha sin red | clave `pending-sign-outs` | Sigue después de la limpieza y se envía cuando vuelve la red. |

No hay un plazo aparte en el aparato. El servidor cierra la sesión después de `SESSION_IDLE_DAYS` sin una petición. El valor por defecto es 180 días.

## Pruebas ejecutadas

<!-- test-results:start -->
Última ejecución el 25 de noviembre de 2026, con el comando de la sección [Cómo ejecutarlo](#cómo-ejecutarlo).

| Resultado | Cantidad |
|---|---|
| Pruebas ejecutadas | 132 |
| Fallos | 0 |
| Errores | 0 |
| Omitidas | 0 |

Los números salen de los informes XML de Gradle, en las carpetas de `common`, `server`, `client/app` y `client/worker`. Las pruebas de `common` entran dos veces cuando corren en la JVM y en JavaScript. El workflow [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) escribe esta tabla y una línea en [TESTES.md](TESTES.md) después de cada ejecución en main, con o sin fallos. Registro: https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45657081839.
<!-- test-results:end -->

## Qué hay en este repositorio

La interfaz es Kotlin con React Native. El mismo código corre como PWA en el navegador y como app Android. El shell Expo en `medcall/client/expo` solo registra el componente Kotlin. En el navegador, Metro cambia `react-native` por `react-native-web`.

El servidor es Kotlin/JVM 21, con Ktor, Koin y Exposed. En desarrollo y en las pruebas la base es H2. En producción es MySQL. La publicación es GitOps: imágenes en ghcr.io, Kubernetes en el namespace `medcall`, ArgoCD y Traefik.

El detalle de módulos, modo sin conexión, zona horaria de la clínica y variables está en [medcall/README.md](medcall/README.md). Este archivo es la portada del repositorio.

## Qué hace la aplicación hoy

- Bienvenida, registro, inicio de sesión y cambio de contraseña con un código de un solo uso.
- Pretriaje informativo, sin diagnóstico.
- Reserva en tres pasos: especialidad y profesional, fecha y horario, confirmación.
- Consultas ya reservadas visibles sin internet. Reservar exige conexión. El servidor vuelve a comprobar el cupo, y un índice único impide dos reservas en el mismo horario.
- Cuenta: perfil, consentimiento de notificaciones, cambio de contraseña y pedido de eliminación.
- Pantalla de política de privacidad.

El preanálisis de riesgo del panel de la clínica está especificado y todavía no está en este código. Solo la clínica y el profesional deben ver el nivel. El paciente no. La decisión es del profesional. Fuente de la especificación: [SPM-A-139](https://grupotp.youtrack.cloud/articles/SPM-A-139/Pre-analise-de-risco-Feita-por-AI). Las normas del Ministerio de Salud y del CFM exigen acogida con clasificación de riesgo y no imponen un protocolo único. Manchester entra como referencia. [Publicación del Ministerio de Salud](https://bvsms.saude.gov.br/bvs/publicacoes/acolhimento_classificaao_risco_servico_urgencia.pdf).

## API

21 rutas en `/api/v1`. Las del paciente exigen `Authorization: Bearer`. El token es un JWT HS256 (`JWT_SECRET`, como mínimo 32 bytes). El servidor guarda solo el id de la sesión y lo revoca al cerrar sesión. El cambio con código también cierra las sesiones de la cuenta.

| Área | Rutas |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/password-recovery`, `POST /auth/password-recovery/confirm`, `GET /auth/me`, `POST /auth/logout` |
| Cuenta | `GET /account`, `PUT /account`, `PUT /account/consents`, `POST /account/password`, `POST /account/deletion-request` |
| Pretriaje | `GET /pre-triages/draft`, `GET /pre-triages/pending`, `POST /pre-triages` |
| Agenda | `GET /specialties`, `GET /professionals`, `GET /clinics`, `GET /availability`, `GET /appointments`, `POST /appointments`, `POST /appointments/{id}/cancel` |

Fuera de la API: `GET /healthz` (proceso en línea, sin consultar la base) y `GET /readyz` (la base responde).

Contraseña con PBKDF2-HMAC-SHA256, 600.000 iteraciones. Fuente del formato: [medcall/README.md](medcall/README.md).

## Arquitectura

```text
Cliente Kotlin/JS + React Native (PWA y Android)
        |
        | REST /api/v1
        v
Servidor Ktor
  controller  ->  service  ->  repository (Exposed)
        |
        v
H2 en desarrollo, MySQL en producción
```

La regla de negocio queda en el service. El controller recibe la petición y devuelve la respuesta.

## Dónde está el código

```text
medcall/
  common/          compartido, JVM y JavaScript
  server/          Ktor, puerto 8080
  client/app/      interfaz y casos de uso
  client/worker/   service worker
  client/expo/     shell Expo
  docker/          imágenes
k8s/               namespace medcall, ingress, MySQL
argocd/            Application de GitOps
.github/workflows/ publicación de las imágenes en ghcr.io
```

Producción: [https://medcall.grupotp.polyclub.com.br](https://medcall.grupotp.polyclub.com.br). El host antiguo `recifesaudavel.grupotp.polyclub.com.br` sigue sirviendo la API. Los demás caminos redirigen al host nuevo. Ver `k8s/ingress.yaml`.

## Cómo ejecutarlo

Requisitos, según [medcall/README.md](medcall/README.md): JDK 21 y Node.js 24. El primer `./gradlew` inicializa el submódulo.

```bash
git clone --recurse-submodules https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2.git
cd App-MedCall-Senac-PE-2026.2/medcall
./gradlew :server:run
./gradlew :client:expo:startWeb
```

El cliente de desarrollo abre en `http://localhost:8081`. El servidor responde en `http://localhost:8080/healthz`. No versiones `JWT_SECRET`, la contraseña de la base, la contraseña del correo ni `.env`. El código de recuperación sale por SMTP (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Sin ese envío, la ruta responde que el código no se puede enviar. Las pruebas usan `MAIL_MODE=capture` y no abren una conexión de correo.

Pruebas:

```bash
./gradlew :common:allTests :server:test :client:app:jsNodeTest :client:worker:jsNodeTest
```

## Equipo

Faculdade Senac Pernambuco, Recife-PE. Curso de Análisis y Desarrollo de Sistemas. Proyecto integrador, agosto a diciembre de 2026. Profesor responsable: Ícaro Santos Ferreira.

- Thayná Batista da Silva: Scrum Master, requisitos, prototipado y desarrollo.
- Polyana Fontes: modelado de base de datos, documentación y desarrollo.

Contacto de Thayná: [thaynabdstec@gmail.com](mailto:thaynabdstec@gmail.com).

## Fuentes

- Código y comandos: [medcall/README.md](medcall/README.md), controllers en `medcall/server/src/main/kotlin/grupotp/medcall/server/controller/`.
- Requisitos vigentes: [SPM-A-141](https://grupotp.youtrack.cloud/articles/SPM-A-141). La V7 está marcada como superada en [SPM-A-133](https://grupotp.youtrack.cloud/articles/SPM-A-133).
- [LGPD, Ley n.º 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm).
- [Ktor, development mode](https://ktor.io/docs/server-development-mode.html).
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/), requisito de accesibilidad del proyecto. La cobertura de pruebas con lectores de pantalla no está terminada en este repositorio.
- [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html): código de un solo uso, la misma respuesta si el correo no existe y sesiones cerradas después del cambio.
