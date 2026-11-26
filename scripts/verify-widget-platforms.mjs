import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const read = (relative) => readFileSync(join(root, relative), "utf8");

const version = read("medcall/client/accessability-widget.version").trim();
const pin = `accessability-widget@${version}/src/accessability-widget.js`;
const html = read("medcall/client/expo/public/index.html");
const config = read("medcall/client/expo/app.config.js");
const index = read("medcall/client/expo/index.js");
const nativeHost = read("medcall/client/expo/accessability-host.native.js");
const webHost = read("medcall/client/expo/accessability-host.web.js");

function webPinCount(text, widgetVersion) {
  const needle = `accessability-widget@${widgetVersion}/src/accessability-widget.js`;
  return text.split(needle).length - 1;
}

const drifted = html.replaceAll(pin, "accessability-widget@9.9.9/src/accessability-widget.js");
if (webPinCount(drifted, version) === 1) {
  console.error("negative control failed");
  process.exit(1);
}
if (!/^\d+\.\d+\.\d+$/.test(version)) {
  console.error("version file", version);
  process.exit(1);
}
if (html.split(pin).length !== 2) {
  console.error("web index.html does not pin this version once");
  process.exit(1);
}
if (!config.includes("accessability-widget.version") || !config.includes("ios:") || !config.includes("android:")) {
  console.error("app config does not feed Android and iOS from the version file");
  process.exit(1);
}
if (config.includes(version) || nativeHost.includes(version) || webHost.includes(version) || index.includes(version)) {
  console.error("version is copied outside the version file");
  process.exit(1);
}
if (!nativeHost.includes("accessabilityWidgetUrl") || !index.includes("accessability-host")) {
  console.error("native host does not use the shared url");
  process.exit(1);
}
if (!webHost.includes("wrapWithAccessAbility")) {
  console.error("web host missing");
  process.exit(1);
}
console.log("widget platforms verification passed");
