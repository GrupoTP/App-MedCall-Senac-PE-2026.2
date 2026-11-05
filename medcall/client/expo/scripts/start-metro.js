// Starts Metro and keeps the Kotlin bundle in sync with the Kotlin sources.
//
//   node scripts/start-metro.js web   PWA on http://localhost:8081
//   node scripts/start-metro.js lan   Expo Go on the local network, with the QR code
//
// Metro only reads the compiled Kotlin bundle, so this script watches the Kotlin sources and runs
// `gradlew :client:app:buildClientBundle` on change; Metro then reloads the app. Gradle's
// continuous mode (`-t`) is not used: the Kotlin plugin runs webpack differently there and missed
// changes. `expo start` only prints the QR code when stdout is a terminal, which is not the
// case under Gradle, so the lan mode picks the LAN address the way the Expo CLI does and prints
// the QR code itself.
const { spawn, spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const PORT = 8081;
const KOTLIN_SOURCES = ["common/src", "client/app/src", "client/worker/src", "grupotp-kotlin-react-native/src"];
const isWindows = process.platform === "win32";
const rootDir = path.resolve(__dirname, "../../..");

// lan-network and toqr come with the Expo CLI; resolve them from it so we use the same versions.
const expoCliDir = path.dirname(
  require.resolve("@expo/cli/package.json", { paths: [path.dirname(require.resolve("expo/package.json"))] }),
);
const fromExpoCli = (name) => require(require.resolve(name, { paths: [expoCliDir] }));

// Draws each module as two spaces with an ANSI background colour. The Expo CLI uses Unicode
// half-blocks, but Gradle hands our UTF-8 bytes to a console that may decode them with another
// code page (cp850 on a Portuguese Windows), which garbles the code. ANSI sequences are plain ASCII.
const QUIET_ZONE = 2;
function renderQR(url) {
  const data = fromExpoCli("toqr").toQR(url);
  const extent = Math.sqrt(data.byteLength) | 0;
  const dark = "\x1b[40m  ";
  const light = "\x1b[107m  ";
  const isDark = (row, col) =>
    row >= 0 && row < extent && col >= 0 && col < extent && data[row * extent + col] === 1;
  const lines = [];
  for (let row = -QUIET_ZONE; row < extent + QUIET_ZONE; row++) {
    let line = "";
    for (let col = -QUIET_ZONE; col < extent + QUIET_ZONE; col++) {
      line += isDark(row, col) ? dark : light;
    }
    lines.push(line + "\x1b[0m");
  }
  return lines.join("\n");
}

function lanAddress() {
  let address;
  try {
    address = fromExpoCli("lan-network").lanNetworkSync().address;
  } catch {
    address = undefined;
  }
  if (!address || address.startsWith("127.")) {
    console.error("No local network address found. Connect this computer to the phone's Wi-Fi network.");
    process.exit(1);
  }
  return address;
}

const children = [];

// Stops the given processes and everything they started. On Linux each child leads its own
// process group. Windows has no groups, and `taskkill /T` cannot walk past a parent that already
// exited (cmd ends with this script, leaving the Gradle client behind), but Windows keeps the
// parent id of orphans, so we walk the parent ids ourselves.
function stopTrees(pids) {
  if (!isWindows) {
    for (const pid of pids) {
      try {
        process.kill(-pid, "SIGTERM");
      } catch {
        // Already gone.
      }
    }
    return;
  }
  const listing = spawnSync(
    "powershell",
    ["-NoProfile", "-Command", 'Get-CimInstance Win32_Process | ForEach-Object { "$($_.ProcessId) $($_.ParentProcessId)" }'],
    { encoding: "utf8", windowsHide: true },
  ).stdout ?? "";
  const childrenOf = new Map();
  for (const line of listing.split(/\r?\n/)) {
    const [pid, parent] = line.trim().split(" ").map(Number);
    if (!pid) continue;
    if (!childrenOf.has(parent)) childrenOf.set(parent, []);
    childrenOf.get(parent).push(pid);
  }
  const tree = [];
  const queue = [...pids];
  while (queue.length > 0) {
    const pid = queue.shift();
    if (tree.includes(pid) || pid === process.pid) continue;
    tree.push(pid);
    queue.push(...(childrenOf.get(pid) ?? []));
  }
  for (const pid of tree) {
    spawnSync("taskkill", ["/pid", String(pid), "/F"], { stdio: "ignore", windowsHide: true });
  }
}

function isAlive(pid) {
  try {
    process.kill(pid, 0);
    return true;
  } catch {
    return false;
  }
}

// When Gradle cancels the task it kills this script without running the handlers below (on
// Windows it is TerminateProcess), which would leave Metro and a running build behind. A detached
// guard process stops them once this script is gone. On Windows it walks everything the script
// started, builds included; on Linux Gradle sends SIGTERM, so only Metro needs the guard.
function guard(parentPid, childPids) {
  setInterval(() => {
    if (isAlive(parentPid)) return;
    stopTrees(isWindows ? [parentPid] : childPids);
    process.exit(0);
  }, 1000);
}

function start(command, args, options) {
  const child = spawn(command, args, { stdio: "inherit", detached: !isWindows, ...options });
  children.push(child);
  return child;
}

function gradle(args) {
  return isWindows
    ? spawn("cmd", ["/c", path.join(rootDir, "gradlew.bat"), ...args], { cwd: rootDir, stdio: "inherit" })
    : spawn(path.join(rootDir, "gradlew"), args, { cwd: rootDir, stdio: "inherit", detached: true });
}

// One build at a time; changes made during a build trigger one more build after it.
let currentBuild = null;
function watchKotlinSources() {
  let pending = false;
  let timer = null;
  const build = () => {
    if (currentBuild) {
      pending = true;
      return;
    }
    console.log("Kotlin sources changed, rebuilding the bundle...");
    currentBuild = gradle(["-q", "--console=plain", ":client:app:buildClientBundle"]);
    currentBuild.on("exit", (code) => {
      currentBuild = null;
      console.log(code === 0 ? "Kotlin bundle updated." : `Kotlin bundle build failed (exit code ${code}).`);
      if (pending) {
        pending = false;
        build();
      }
    });
  };
  for (const dir of KOTLIN_SOURCES.map((source) => path.join(rootDir, source))) {
    if (!fs.existsSync(dir)) continue;
    fs.watch(dir, { recursive: true }, () => {
      clearTimeout(timer);
      timer = setTimeout(build, 300);
    });
  }
}

function shutdown(code) {
  stopTrees([...children, currentBuild].filter(Boolean).map((child) => child.pid));
  process.exit(code);
}

function main(mode) {
  const env = { ...process.env };
  const expoArgs = ["start", "--port", String(PORT)];
  if (mode === "web") {
    expoArgs.push("--web");
  } else {
    const address = lanAddress();
    const url = `exp://${address}:${PORT}`;
    console.log(renderQR(url));
    console.log(`\nScan the QR code with Expo Go (Android) or open ${url}`);
    console.log(`The app calls the server at http://${address}:8080; keep :server:run running.\n`);
    expoArgs.push("--lan");
    env.REACT_NATIVE_PACKAGER_HOSTNAME = address;
  }

  watchKotlinSources();

  const metro = start(process.execPath, [require.resolve("expo/bin/cli"), ...expoArgs], { env });
  metro.on("exit", (code, signal) => shutdown(signal ? 1 : code ?? 0));

  spawn(process.execPath, [__filename, "--guard", String(process.pid), ...children.map((child) => child.pid)], {
    detached: true,
    stdio: "ignore",
    windowsHide: true,
  }).unref();

  for (const signal of ["SIGINT", "SIGTERM", "SIGHUP"]) {
    process.on(signal, () => shutdown(1));
  }
}

const mode = process.argv[2];
if (mode === "--guard") {
  guard(Number(process.argv[3]), process.argv.slice(4).map(Number));
} else if (mode === "web" || mode === "lan") {
  main(mode);
} else {
  console.error("Usage: node scripts/start-metro.js <web|lan>");
  process.exit(1);
}
