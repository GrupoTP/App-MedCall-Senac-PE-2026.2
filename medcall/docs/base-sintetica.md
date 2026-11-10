# Base sintética de demonstração

Dados fictícios para os dashboards e para a 2ª VA de Data Science ("base utilizada"). Card: [SPM-84](https://grupotp.youtrack.cloud/issue/SPM-84). Indicadores: [kpis.md](kpis.md).

## Como gerar

O servidor gera a base quando sobe com `DEMO_SEED=true` e o banco **ainda não tem nenhum agendamento**. As tasks `:server:run` e `:server:runDev` ligam isso por padrão; em produção a variável não existe e nada é gerado.

```bash
./gradlew :server:run
```

Para gerar de novo, pare o servidor e apague o banco local (`server/data/medcall.mv.db`).

O gerador (`server/.../database/DemoSeed.kt`) usa uma **semente fixa**: no mesmo dia, a base sai sempre igual. As datas são relativas ao dia em que o servidor sobe (90 dias para trás e 14 para frente). Por isso, para a apresentação, gere a base no dia e use um período fixo nos filtros.

## O que a base tem

| Item | Quantidade |
|---|---|
| Clínicas | 3 (Clínica Boa Vista e Policlínica Derby em Recife, Teleconsulta Manaus) |
| Profissionais | 30, em 11 especialidades |
| Pacientes | 420, com cadastros crescendo nos últimos 6 meses |
| Agendamentos | cerca de 20 mil, com os eventos de cada um (criado, confirmado, recusado, cancelado, compareceu, faltou) |
| Pré-triagens | cerca de 40% dos agendamentos |
| Contas de equipe | `medcall@`, `admin.boavista@`, `recepcao.boavista@`, `admin.derby@`, `recepcao.derby@`, `admin.manaus@`, `recepcao.manaus@` (domínio `demo.medcall.dev`), senha `DEMO_STAFF_PASSWORD` (`medcall-demo` nas tasks locais) |

Nomes e telefones são gerados; nenhum dado é real.

## Padrões inseridos de propósito

Cada pergunta da apresentação tem um padrão para o filtro revelar:

1. **Ortopedia lota no começo do mês.** Nos dias 1 a 10, cerca de 94% dos horários de Ortopedia são ocupados, contra cerca de 58% no resto do mês. Veja no mapa "especialidade × dia" filtrando por Ortopedia.
2. **A recepção da Clínica Boa Vista ficou lenta.** Até três semanas atrás, a mediana até a primeira resposta era de cerca de 3 h; nas últimas três semanas, cerca de 16 h. Compare "Últimos 7 dias" com "Últimos 90 dias" no gráfico 2. A Teleconsulta Manaus é lenta o tempo todo (mediana de cerca de 30 h), o que aparece no ranking do Painel MedCall.
3. **Mais faltas na segunda de manhã.** A chance de falta é de 27% na segunda de manhã, 15% na sexta à tarde e 8% nos outros turnos.

Também há: 7% de cancelamentos pelo paciente, 5% de recusas pela clínica e cobertura do registro de comparecimento de 93% (Boa Vista), 86% (Derby) e 72% (Manaus).

## Exportação (CSV)

No Portal da Clínica, **Indicadores › Exportar base (CSV)** baixa os agendamentos da clínica no período escolhido. O arquivo é UTF-8 com BOM, separado por `;` e com vírgula decimal, para o Excel em português abrir direto. Não há dados de pacientes.

| Coluna | Tipo | Significado |
|---|---|---|
| `agendamento_id` | inteiro | Identificador do agendamento |
| `especialidade` | texto | Especialidade do profissional |
| `profissional` | texto | Nome do profissional |
| `data_consulta` | data (AAAA-MM-DD) | Dia da consulta, no relógio da clínica |
| `hora_consulta` | hora (HH:MM) | Horário da consulta, no relógio da clínica |
| `dia_semana` | texto | segunda … domingo |
| `turno` | texto | `manha` (antes das 12 h) ou `tarde` |
| `situacao` | texto | `aguardando`, `confirmada`, `recusada`, `cancelada`, `compareceu`, `faltou` |
| `agendado_em` | data e hora | Quando o paciente agendou, no relógio da clínica |
| `primeira_resposta` | texto | `confirmou`, `remarcou`, `recusou` ou vazio |
| `primeira_resposta_em` | data e hora | Quando a clínica respondeu pela primeira vez |
| `horas_ate_resposta` | decimal | Horas entre o agendamento e a primeira resposta (base do C04) |
| `origem_resposta` | texto | `portal` ou `api` |
| `cancelado_pelo_paciente` | `sim`/`nao` | Se o paciente cancelou (base do C06) |
| `tem_pre_triagem` | `sim`/`nao` | Se o agendamento veio com pré-triagem (base do M06) |
| `antecedencia_dias` | decimal | Dias entre o agendamento e a consulta (base do C08) |

O arquivo traz os agendamentos com **consulta ou agendamento** no período, então tem um pouco mais de linhas que o C01.

## Roteiro de conferência

Para mostrar que o painel bate com a base (critério da avaliação):

1. **C01, agendamentos:** filtre `agendado_em` pelo período do painel e conte as linhas.
2. **C07, faltas:** filtre `data_consulta` pelo período e `situacao` em `compareceu` ou `faltou`; divida as `faltou` pelo total.
3. **C04, tempo até a resposta:** filtre `agendado_em` pelo período e calcule a mediana de `horas_ate_resposta` (ignorando vazios).

O C02 (ocupação) não sai só da planilha: o denominador são os horários oferecidos (16 por dia útil por profissional ativo), que não estão no arquivo.
