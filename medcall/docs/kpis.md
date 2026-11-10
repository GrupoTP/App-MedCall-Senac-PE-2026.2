# Catálogo de KPIs do MedCall

Fonte única dos indicadores do **Portal da Clínica** e do **Painel de Controle MedCall**. O código, os gráficos e a apresentação de Data Science usam os códigos daqui (`C01`, `M03`...) e as mesmas fórmulas. Mudou uma fórmula? Mude aqui primeiro.

Card: [SPM-82](https://grupotp.youtrack.cloud/issue/SPM-82) · Marco: [SPM-77](https://grupotp.youtrack.cloud/issue/SPM-77)

## Regras que valem para todos os KPIs

- **Fuso.** Datas e horas de consulta são o relógio da clínica (`clinics.time_zone`). Instantes (`created_at` e eventos) são convertidos para o fuso da clínica antes de agrupar por dia. No Painel MedCall, que mistura clínicas, cada agendamento é agrupado pelo fuso da própria clínica.
- **Duas datas de referência.** Não confundir:
  - *Data do agendamento*: quando o paciente marcou (`appointments.created_at`). Mede **demanda**.
  - *Data da consulta*: quando o paciente vai ser atendido (`appointment_date` + `appointment_time`). Mede **agenda**.
  Cada KPI diz qual das duas usa; o filtro de período do painel aplica a data indicada no KPI.
- **Agendamento ativo.** Status `pending` ou `confirmed` (hoje com `slot_key` preenchido). Os status planejados em [SPM-83](https://grupotp.youtrack.cloud/issue/SPM-83) se encaixam assim: `confirmed`, `rescheduled`, `attended` e `no_show` contam como ocupados; `declined` e `cancelled` liberam o horário.
- **Horários ofertados.** Até a grade por profissional ([SPM-94](https://grupotp.youtrack.cloud/issue/SPM-94)) existir, cada profissional oferece a grade fixa de `SchedulingService.OPENING_TIMES`: **16 horários por dia útil** (08:00–11:30 e 14:00–17:30, de 30 em 30 min), sem sábado e domingo.
- **Exclusões.** Contas com exclusão pedida (`users.deletion_requested_at` preenchido) ficam fora das contagens de pacientes, mas os agendamentos delas continuam contando para a agenda da clínica.
- **Mascaramento (LGPD).** No Painel MedCall, qualquer grupo com menos de 5 ocorrências aparece como `< 5` ([SPM-96](https://grupotp.youtrack.cloud/issue/SPM-96)). No Portal da Clínica não há mascaramento, porque a clínica já vê os agendamentos dela.
- **Divisão por zero.** Taxa sem denominador aparece como `—` (sem dado), nunca como 0 %.
- **Mediana, não média,** para tempos: poucos casos muito demorados distorcem a média.

## Situação das fontes

| Sinal | Significado |
|---|---|
| ✅ | A fonte já existia no banco |
| 🟡 | Vem dos eventos de agendamento (`appointment_events`, [SPM-83](https://grupotp.youtrack.cloud/issue/SPM-83)), já implementados |
| 🔴 | Precisa de uma fonte nova, fora do escopo atual |

Onde está o código: `server/.../service/KpiService.kt` (fórmulas, cada uma comentada com o código daqui) e `server/.../repository/KpiRepository.kt` (leitura dos dados). As telas: Portal da Clínica › Indicadores e Painel MedCall › Visão geral.

## Resumo

### Portal da Clínica (público: administrador da clínica)

| Código | KPI | Data de referência | Fonte | Fase |
|---|---|---|---|---|
| C01 | Agendamentos recebidos | agendamento | ✅ | **MVP** |
| C02 | Taxa de ocupação da agenda | consulta | ✅ | **MVP** |
| C03 | Taxa de confirmação | agendamento | 🟡 | **MVP** |
| C04 | Tempo até a confirmação (mediana) | agendamento | 🟡 | **MVP** |
| C05 | Pendentes há mais de 24 h | agora | ✅ | **MVP** |
| C06 | Taxa de cancelamento pelo paciente | agendamento | 🟡 | Na API, fora da tela |
| C07 | Taxa de faltas (no-show) | consulta | 🟡 | **MVP** |
| C08 | Antecedência do agendamento (mediana) | agendamento | ✅ | Na API, fora da tela |
| C09 | Mapa de pico (dia da semana × hora) | consulta | ✅ | **MVP** |

### Painel MedCall (público: equipe interna)

| Código | KPI | Data de referência | Fonte | Fase |
|---|---|---|---|---|
| M01 | Novos cadastros de pacientes | cadastro | ✅ | **MVP** |
| M02 | Pacientes que agendaram | agendamento | ✅ | **MVP** |
| M03 | Agendamentos por clínica (ranking) | agendamento | ✅ | **MVP** |
| M04 | Clínicas ativas | agendamento | ✅ | **MVP** |
| M05 | Tempo até a confirmação por clínica | agendamento | 🟡 | **MVP** |
| M06 | Uso da pré-triagem | agendamento | ✅ | **MVP** |
| M07 | Clínicas por modo de integração | agora | ✅ `webhooks` | **MVP** |
| M08 | Saúde das integrações (webhooks) | entrega | ✅ `webhook_deliveries` | **MVP** |
| M09 | Pacientes ativos (semana/mês) | atividade | 🔴 | Futuro |
| M10 | Conversão do fluxo de agendamento | sessão do app | 🔴 | Futuro |

---

## Fichas do Portal da Clínica

### C01 · Agendamentos recebidos

- **Definição:** quantos agendamentos os pacientes fizeram para a clínica pelo MedCall no período.
- **Fórmula:** `count(appointments)` com `created_at` no período, de profissionais da clínica, em qualquer status.
- **Unidade:** número inteiro. Na tela: total e variação contra o período anterior de mesmo tamanho.
- **Fonte:** `appointments` ⨝ `professionals` (`clinic_id`).
- **Recortes:** período, especialidade, profissional; série por dia.
- **Pergunta:** quanto de demanda o MedCall traz para a clínica?
- **Decisão:** manter ou ampliar a parceria; liberar mais horários no app.
- **Limite:** conta só o que chegou pelo MedCall, não a demanda total da clínica (telefone, balcão, outros apps).

### C02 · Taxa de ocupação da agenda

- **Definição:** que parte dos horários oferecidos foi tomada.
- **Fórmula:** `horários ocupados ÷ horários ofertados`, onde
  - ocupados = agendamentos ativos com data da consulta no período;
  - ofertados = `16 × dias úteis do período × profissionais` (ver "Horários ofertados").
- **Unidade:** % com uma casa decimal.
- **Fonte:** `appointments`, `professionals`, `specialties`; grade de `SchedulingService`.
- **Recortes:** período, especialidade, profissional; matriz **especialidade × dia** (gráfico principal do dashboard).
- **Pergunta:** quais especialidades lotam primeiro, e em que dias? (ex. do professor: ortopedistas com agenda cheia no dia 5.)
- **Decisão:** abrir horários extras, chamar outro profissional, redistribuir a agenda.
- **Limite:** mede a demanda **atendida**, não a reprimida: o paciente que não achou vaga e desistiu não deixa registro. Enquanto a grade for fixa, um profissional que atende meio período aparece com ocupação mais baixa do que a real.

### C03 · Taxa de confirmação

- **Definição:** dos agendamentos que a clínica já respondeu, quantos ela aceitou.
- **Fórmula:** `confirmados ÷ (confirmados + recusados)`, considerando a **primeira** decisão da clínica para cada agendamento criado no período. Remarcação conta como aceite.
- **Unidade:** %.
- **Fonte:** 🟡 eventos `CONFIRMADO`, `REMARCADO` e `RECUSADO` ([SPM-83](https://grupotp.youtrack.cloud/issue/SPM-83)).
- **Recortes:** período, especialidade, profissional.
- **Pergunta:** a clínica está aceitando o que chega pelo app?
- **Decisão:** se baixa, ajustar os horários que o app oferece (estão oferecendo horário que a clínica não tem).
- **Limite:** pendentes ficam fora do denominador; ler sempre junto com C05.

### C04 · Tempo até a confirmação (mediana)

- **Definição:** quanto tempo o paciente espera até a clínica responder.
- **Fórmula:** mediana de `primeira decisão.ocorrido_em − CRIADO.ocorrido_em`, em horas, para agendamentos criados no período e já respondidos.
- **Unidade:** horas com uma casa (abaixo de 1 h, em minutos).
- **Fonte:** 🟡 eventos ([SPM-83](https://grupotp.youtrack.cloud/issue/SPM-83)).
- **Recortes:** período, profissional, origem da decisão (portal × API).
- **Pergunta:** a recepção responde a tempo?
- **Decisão:** reforçar a recepção em certos horários ou integrar por webhook.
- **Limite:** conta horas corridas, inclusive noite e fim de semana. Pendentes ficam fora, então um tempo baixo com muitos pendentes engana; ler junto com C05.

### C05 · Pendentes há mais de 24 h

- **Definição:** agendamentos ainda "Aguardando" criados há mais de 24 horas.
- **Fórmula:** `count(appointments)` com `status = pending` e `created_at < agora − 24 h`, só com consulta futura.
- **Unidade:** número inteiro (alerta vermelho se > 0).
- **Fonte:** ✅ `appointments`.
- **Recortes:** profissional, especialidade. Não usa filtro de período: é uma foto de agora.
- **Pergunta:** tem paciente esperando resposta?
- **Decisão:** agir hoje; abrir a agenda filtrada por pendentes.
- **Limite:** nenhum relevante; é uma contagem direta.

### C06 · Taxa de cancelamento pelo paciente

- **Definição:** quantos agendamentos o próprio paciente cancelou.
- **Fórmula:** `cancelados pelo paciente ÷ agendamentos criados no período`.
- **Unidade:** %.
- **Fonte:** 🟡 eventos com origem `APP_PACIENTE`. Hoje `cancelled` não diz quem cancelou nem quando.
- **Pergunta / decisão:** os pacientes desistem muito? Lembretes, revisão da antecedência oferecida.
- **Limite:** não sabe o motivo do cancelamento.

### C07 · Taxa de faltas (no-show)

- **Definição:** das consultas confirmadas que já passaram e têm registro de comparecimento, quantas o paciente faltou.
- **Fórmula:** `faltas ÷ (faltas + comparecimentos)`, por data da consulta no período.
- **Unidade:** %.
- **Fonte:** 🟡 eventos `COMPARECEU` e `FALTOU` ([SPM-90](https://grupotp.youtrack.cloud/issue/SPM-90)).
- **Recortes:** período, profissional, especialidade, dia da semana, turno.
- **Pergunta:** onde a clínica perde horários?
- **Decisão:** confirmação na véspera, lembrete, encaixe controlado nos turnos com mais falta.
- **Limite:** depende de a recepção registrar o comparecimento; consultas sem registro ficam fora. Mostrar ao lado a **cobertura** (`com registro ÷ consultas passadas confirmadas`).

### C08 · Antecedência do agendamento (mediana)

- **Definição:** com quantos dias de antecedência os pacientes marcam.
- **Fórmula:** mediana de `(data e hora da consulta no fuso da clínica) − created_at`, em dias.
- **Fonte:** ✅ `appointments` + `clinics.time_zone`.
- **Pergunta / decisão:** a janela de 14 dias do app (`BOOKING_WINDOW_DAYS`) é suficiente?
- **Limite:** a própria janela limita o valor máximo; não mostra quem queria marcar mais longe.

### C09 · Mapa de pico

- **Definição:** em que dias da semana e horários se concentram as consultas.
- **Fórmula:** `count(agendamentos ativos)` por (dia da semana, horário da consulta).
- **Fonte:** ✅ `appointments`.
- **Pergunta / decisão:** escala de profissionais e da recepção.
- **Limite:** com a grade fixa, mostra preferência de horário só dentro do que é oferecido.

---

## Fichas do Painel MedCall

### M01 · Novos cadastros de pacientes

- **Fórmula:** `count(users)` com `created_at` no período e `deletion_requested_at` vazio.
- **Fonte:** ✅ `users`.
- **Pergunta / decisão:** a divulgação está trazendo gente? Onde investir.
- **Limite:** cadastro não é uso; ler junto com M02.

### M02 · Pacientes que agendaram

- **Definição:** pacientes distintos com pelo menos um agendamento criado no período. É o melhor sinal de uso que o banco permite hoje (ver M09).
- **Fórmula:** `count(distinct appointments.patient_id)` com `created_at` no período.
- **Fonte:** ✅ `appointments`.
- **Recortes:** período, clínica.
- **Limite:** não conta quem só consultou a agenda ou usou outra função.

### M03 · Agendamentos por clínica (ranking)

- **Fórmula:** C01 agrupado por clínica, ordenado.
- **Fonte:** ✅ `appointments` ⨝ `professionals` ⨝ `clinics`.
- **Pergunta / decisão:** onde está a demanda; que clínica precisa de mais profissionais no app.
- **Limite:** clínica com mais profissionais tende a ter mais agendamentos; mostrar também C02 por clínica.

### M04 · Clínicas ativas

- **Definição:** clínicas com pelo menos um agendamento criado no período.
- **Fonte:** ✅. Depois de [SPM-93](https://grupotp.youtrack.cloud/issue/SPM-93), mostrar também as cadastradas (`Ativa`) sem movimento.
- **Decisão:** acionar clínica parada.

### M05 · Tempo até a confirmação por clínica

- **Fórmula:** C04 agrupado por clínica, em ranking; destacar acima de 24 h.
- **Fonte:** 🟡 eventos.
- **Decisão:** que clínica precisa de suporte ou de integração por webhook.

### M06 · Uso da pré-triagem

- **Definição:** que parte dos agendamentos vem com pré-triagem preenchida.
- **Fórmula:** `agendamentos com pre_triages.appointment_id ÷ agendamentos criados no período`.
- **Fonte:** ✅ `pre_triages`, `appointments`. Só a contagem: o painel nunca lê o conteúdo (`form`), que é dado de saúde.
- **Decisão:** vale investir na pré-triagem? Onde divulgar a função.
- **Limite:** não mede se a clínica usou a pré-triagem.

### M07 a M10 · Futuros

- **M07 Clínicas por modo de integração:** clínicas ativas com webhook ativo × manuais ([SPM-97](https://grupotp.youtrack.cloud/issue/SPM-97)).
- **M08 Saúde das integrações:** `entregas com 2xx ÷ entregas criadas no período` e quantas foram para a fila de mortas ([SPM-98](https://grupotp.youtrack.cloud/issue/SPM-98)).
- **M09 Pacientes ativos (semana/mês):** precisa de uma tabela de atividade diária (`user_id`, `dia`). A tabela `sessions` não serve: a linha é apagada no logout e depois de `SESSION_IDLE_DAYS` sem uso, e guarda só o último acesso. `users.last_login_at` também guarda só o último login.
- **M10 Conversão do fluxo de agendamento:** quem abriu o fluxo × quem confirmou; precisa de telemetria no app.

---

## As 3 perguntas da apresentação (2ª VA de Data Science)

O professor pede para usar o painel, aplicar o filtro e responder 3 perguntas, explicando **a decisão e o limite** ([ADS-33](https://grupotp.youtrack.cloud/issue/ADS-33)). Todas ficam no dashboard da clínica ([SPM-87](https://grupotp.youtrack.cloud/issue/SPM-87)), na voz do administrador da clínica:

| # | Pergunta | KPI e gráfico | Filtro usado | Decisão | Limite |
|---|---|---|---|---|---|
| 1 | **Quais especialidades lotam a agenda primeiro, e em que dias?** | C02 na matriz especialidade × dia | especialidade + período | Abrir horários ou chamar mais um profissional na especialidade que lota | Mede demanda atendida, não reprimida |
| 2 | **A recepção está respondendo os agendamentos a tempo?** | C04 + C05 (+ C03) | período (7 × 30 dias) e profissional | Reforçar a recepção ou integrar por webhook | Horas corridas; pendentes fora da mediana |
| 3 | **Em que dias e turnos mais perdemos horários com faltas?** | C07 por dia da semana e turno, com a cobertura | profissional + período | Confirmação na véspera e encaixe nos turnos críticos | Depende do registro de comparecimento |

A base sintética ([SPM-84](https://grupotp.youtrack.cloud/issue/SPM-84)) deve trazer de propósito um padrão para cada pergunta (uma especialidade que lota no começo do mês, uma clínica que confirma devagar, mais faltas na segunda de manhã), para que a resposta apareça ao aplicar o filtro.

## Como conferir um número

Para cada KPI do MVP, a apresentação mostra um valor do painel recalculado à mão na planilha exportada da base. O roteiro e o dicionário das colunas estão em [base-sintetica.md](base-sintetica.md).
