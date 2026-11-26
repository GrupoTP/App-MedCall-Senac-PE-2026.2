# Registro de testes

Cada execução na main acrescenta uma linha. A tabela do README mostra a mais recente. Os XML ficam no artefato `test-reports` da mesma execução. O commit é o `GITHUB_SHA` que disparou o workflow.

O comando da suíte é `bash ./gradlew :common:allTests :server:test :client:app:jsNodeTest :client:worker:jsNodeTest --continue`, no diretório `medcall`. Os testes de `common` entram duas vezes quando rodam na JVM e no JavaScript. O passo que confere a mesma versão do widget na web, no Android e no iOS não entra nesta contagem. O resultado dele está no log da mesma execução.

| Quando | Onde | Commit | Executados | Falhas | Erros | Ignorados |
|---|---|---|---:|---:|---:|---:|
| 25 de novembro de 2026 | máquina local | sem commit publicado | 128 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45549075023 | `9de9043a95905f7efa9ab5115dd56e15b94c3155` | 128 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45655652705 | `ccd50c96d9c4ef6bb722ad19af4075fab623d768` | 132 | 1 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45657081839 | `a395a1cdd625859ed5d79ebd51d2a0338a4ed6a7` | 132 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45665242462 | `01bfb3ddde31b1b73e0208acf0a40e974ee9ce90` | 132 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45719764437 | `c6d180bc6116b7509b82a8299dc2ae1eefd1135e` | 132 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45721384720 | `36a6e6a77cf1d0ce3af9c1694274097c0402a34e` | 132 | 0 | 0 | 0 |
| 25 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45724975177 | `4fb00c24de9016432786a88b2b91a8cbe84e2d9d` | 132 | 0 | 0 | 0 |
| 26 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45882583467 | `2426c5c06e7496e85fc2f11463c3d0d0599430b3` | 143 | 0 | 0 | 0 |
| 26 de novembro de 2026 | https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45889295798 | `a3a1bc46241d2854768a2a440a037d07258f7b85` | 143 | 0 | 0 | 0 |
<!-- rows:end -->

## Falhas nomeadas

O nome e a mensagem saem do XML do artefato `test-reports`. A execução seguinte, no commit `a395a1cdd625859ed5d79ebd51d2a0338a4ed6a7`, registrou 0 falhas.

Execução https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/runs/45655652705, commit `ccd50c96d9c4ef6bb722ad19af4075fab623d768`:

- `grupotp.medcall.server.AuthRouteTest.passwordRecoveryReplacesThePasswordAndEndsOpenSessions(): java.lang.IllegalStateException: recovery did not keep a code. status=200 OK body={"accepted":true}`
