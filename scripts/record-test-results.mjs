import { appendFileSync, readFileSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { join } from "node:path";

const repo = new URL("..", import.meta.url).pathname;
const roots = [
  "medcall/common/build",
  "medcall/server/build",
  "medcall/client/app/build",
  "medcall/client/worker/build",
];

function walk(dir, found) {
  let entries;
  try {
    entries = readdirSync(dir);
  } catch {
    return;
  }
  for (const name of entries) {
    const path = join(dir, name);
    let info;
    try {
      info = statSync(path);
    } catch {
      continue;
    }
    if (info.isDirectory()) {
      if (name === "node_modules" || name === ".gradle") continue;
      walk(path, found);
    } else if (name.startsWith("TEST-") && name.endsWith(".xml")) {
      found.push(path);
    }
  }
}

const reports = [];
for (const root of roots) walk(join(repo, root), reports);

let tests = 0;
let failures = 0;
let errors = 0;
let skipped = 0;
const broken = [];
for (const path of reports) {
  const xml = readFileSync(path, "utf8");
  const suite = xml.match(/<testsuite\b[^>]*>/);
  if (!suite) continue;
  const read = (attr) => Number(suite[0].match(new RegExp(`${attr}="(\\d+)"`))?.[1] ?? "0");
  tests += read("tests");
  failures += read("failures");
  errors += read("errors");
  skipped += read("skipped");
  broken.push(...namedFailures(xml));
}

function namedFailures(xml) {
  const found = [];
  const cases = xml.matchAll(/<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g);
  for (const match of cases) {
    const body = match[2] ?? "";
    if (!body.includes("<failure") && !body.includes("<error")) continue;
    const attrs = match[1];
    const name = attrs.match(/\bname="([^"]*)"/)?.[1] ?? "";
    const classname = attrs.match(/\bclassname="([^"]*)"/)?.[1] ?? "";
    const raw = body.match(/<(?:failure|error)\b[^>]*\bmessage="([^"]*)"/)?.[1] ?? "";
    const message = raw
      .replaceAll("&quot;", '"')
      .replaceAll("&lt;", "<")
      .replaceAll("&gt;", ">")
      .replaceAll("&amp;", "&");
    found.push(`${classname}.${name}: ${message}`);
  }
  return found;
}

const when = new Intl.DateTimeFormat("en-GB", {
  timeZone: "America/Recife",
  day: "numeric",
  month: "numeric",
  year: "numeric",
}).format(new Date());
const [day, month, year] = when.split("/");
const months = {
  pt: ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"],
  en: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
  es: ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"],
};
const monthName = (lang) => months[lang][Number(month) - 1];
const date = {
  pt: `${Number(day)} de ${monthName("pt")} de ${year}`,
  en: `${Number(day)} ${monthName("en")} ${year}`,
  es: `${Number(day)} de ${monthName("es")} de ${year}`,
};

const passed = reports.length > 0 && failures === 0 && errors === 0;
const color = passed ? "1E6B5E" : "B42318";
const count = reports.length > 0 ? String(tests) : "sem%20relatorio";
const shown = reports.length > 0 ? String(tests) : "sem relatório";
const failShown = reports.length > 0 ? String(failures) : "sem relatório";
const errorShown = reports.length > 0 ? String(errors) : "sem relatório";
const skipShown = reports.length > 0 ? String(skipped) : "sem relatório";

const run = process.env.GITHUB_RUN_ID
  ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
  : "execução local";
const sha = process.env.GITHUB_SHA ?? "sem commit";

function between(text, start, end, body) {
  const a = text.indexOf(start);
  const b = text.indexOf(end, a + start.length);
  if (a < 0 || b < 0) throw new Error(`marcador ausente: ${start}`);
  return text.slice(0, a + start.length) + "\n" + body.trim() + "\n" + text.slice(b);
}

const pages = [
  {
    file: "README.md",
    badge: `![Testes](https://img.shields.io/badge/testes-${passed ? count + "%20passando" : count + "%20com%20falha"}-${color})`,
    body: `Última execução em ${date.pt}, com o comando da seção [Como executar](#como-executar).

| Resultado | Quantidade |
|---|---|
| Testes executados | ${shown} |
| Falhas | ${failShown} |
| Erros | ${errorShown} |
| Ignorados | ${skipShown} |

Os números saem dos relatórios XML do Gradle, nas pastas de \`common\`, \`server\`, \`client/app\` e \`client/worker\`. Os testes de \`common\` entram duas vezes quando rodam na JVM e no JavaScript. O workflow [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) grava esta tabela e uma linha em [TESTES.md](TESTES.md) depois de cada execução na main, com ou sem falha. Registro: ${run}. Commit: \`${sha}\`. Quando há falha, o nome do teste fica em TESTES.md.`,
  },
  {
    file: "README.en.md",
    badge: `![Tests](https://img.shields.io/badge/tests-${passed ? count + "%20passing" : count + "%20failed"}-${color})`,
    body: `Last run on ${date.en}, with the command in [Run](#run).

| Result | Count |
|---|---|
| Tests run | ${shown} |
| Failures | ${failShown} |
| Errors | ${errorShown} |
| Skipped | ${skipShown} |

The numbers come from the Gradle XML reports under \`common\`, \`server\`, \`client/app\`, and \`client/worker\`. \`common\` tests are counted twice when they run on the JVM and on JavaScript. The [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) workflow writes this table and a row in [TESTES.md](TESTES.md) after every run on main, whether it passes or fails. Record: ${run}. Commit: \`${sha}\`. A failed test is named in TESTES.md.`,
  },
  {
    file: "README.es.md",
    badge: `![Pruebas](https://img.shields.io/badge/pruebas-${passed ? count + "%20ok" : count + "%20con%20fallos"}-${color})`,
    body: `Última ejecución el ${date.es}, con el comando de la sección [Cómo ejecutarlo](#cómo-ejecutarlo).

| Resultado | Cantidad |
|---|---|
| Pruebas ejecutadas | ${shown} |
| Fallos | ${failShown} |
| Errores | ${errorShown} |
| Omitidas | ${skipShown} |

Los números salen de los informes XML de Gradle, en las carpetas de \`common\`, \`server\`, \`client/app\` y \`client/worker\`. Las pruebas de \`common\` entran dos veces cuando corren en la JVM y en JavaScript. El workflow [Tests](https://github.com/GrupoTP/App-MedCall-Senac-PE-2026.2/actions/workflows/tests.yml) escribe esta tabla y una línea en [TESTES.md](TESTES.md) después de cada ejecución en main, con o sin fallos. Registro: ${run}. Commit: \`${sha}\`. Si hay un fallo, el nombre de la prueba queda en TESTES.md.`,
  },
];

if (process.env.WRITE_TEST_RECORD === "1") {
  for (const page of pages) {
    const path = join(repo, page.file);
    let text = readFileSync(path, "utf8");
    text = between(text, "<!-- test-badge:start -->", "<!-- test-badge:end -->", page.badge);
    text = between(text, "<!-- test-results:start -->", "<!-- test-results:end -->", page.body);
    writeFileSync(path, text);
  }
  const log = join(repo, "TESTES.md");
  const current = readFileSync(log, "utf8");
  const row = `| ${date.pt} | ${run} | \`${sha}\` | ${shown} | ${failShown} | ${errorShown} | ${skipShown} |`;
  const marker = "<!-- rows:end -->";
  if (!current.includes(marker)) throw new Error("TESTES.md sem marcador de linhas");
  if ((!current.includes(run) || run === "execução local") && !current.includes(row)) {
    writeFileSync(log, current.replace(marker, row + "\n" + marker));
  }
  if (broken.length > 0 && !current.includes(broken[0])) {
    const lines = ["", `Execução ${run}, commit \`${sha}\`:`];
    for (const item of broken) lines.push(`- \`${item}\``);
    appendFileSync(log, lines.join("\n") + "\n");
  }
}

if (process.env.GITHUB_STEP_SUMMARY) {
  appendFileSync(
    process.env.GITHUB_STEP_SUMMARY,
    `### Testes\n\nRelatórios: ${reports.length}. Executados: ${shown}. Falhas: ${failShown}. Erros: ${errorShown}. Ignorados: ${skipShown}. Commit: \`${sha}\`.\n${broken.map((item) => `- ${item}`).join("\n")}\n`,
  );
}

console.log(`recorded tests=${shown} failures=${failShown} errors=${errorShown} skipped=${skipShown} reports=${reports.length}`);
