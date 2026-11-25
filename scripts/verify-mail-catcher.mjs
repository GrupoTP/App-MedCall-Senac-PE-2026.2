import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");

function check(config, mailpit, kustomization, server) {
  const problems = [];
  const password = config.match(/^  MAIL_PASSWORD: (.*)$/m)?.[1] ?? "";
  if (password !== '""') problems.push("MAIL_PASSWORD is not empty");
  if (!/^  MAIL_HOST: mailpit$/m.test(config)) problems.push("MAIL_HOST is not mailpit");
  if (!/^  MAIL_PORT: "1025"$/m.test(config)) problems.push("MAIL_PORT is not 1025");
  if (!/^  MAIL_FROM: \S+@\S+$/m.test(config)) problems.push("MAIL_FROM is missing");
  if (/MAIL_PASSWORD:\s*"?\S{8,}"?/.test(config) && password !== '""') {
    problems.push("a mail password is written in the manifest");
  }
  if (!config.includes("MAIL_USERNAME: \"\"")) problems.push("MAIL_USERNAME is not empty");
  if (!mailpit.includes("containerPort: 1025")) problems.push("mailpit does not listen on 1025");
  if (!mailpit.includes("axllent/mailpit:v1.34.0")) problems.push("mailpit image is not pinned");
  if (!kustomization.includes("- mailpit.yaml")) problems.push("kustomization omits mailpit");
  if (!server.includes("nc -z mailpit 1025")) problems.push("server does not wait for mailpit");
  return problems;
}

const config = readFileSync(join(root, "k8s/configmap.yaml"), "utf8");
const mailpit = readFileSync(join(root, "k8s/mailpit.yaml"), "utf8");
const kustomization = readFileSync(join(root, "k8s/kustomization.yaml"), "utf8");
const server = readFileSync(join(root, "k8s/server.yaml"), "utf8");
const real = check(config, mailpit, kustomization, server);
if (real.length > 0) {
  console.error(real.join("\n"));
  process.exit(1);
}

const planted = config.replace('MAIL_PASSWORD: ""', 'MAIL_PASSWORD: "segredo-de-teste"');
const negative = check(planted, mailpit, kustomization, server);
if (negative.length === 0) {
  console.error("negative control accepted a mail password");
  process.exit(1);
}

console.log("mail catcher verification passed");
