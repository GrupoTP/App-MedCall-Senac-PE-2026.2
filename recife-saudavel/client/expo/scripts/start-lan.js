// Starts Metro for phones on the same network and prints the Expo Go QR code.
//
// `expo start` only prints the QR code when stdout is a terminal, which is not the case under
// Gradle. This script picks the LAN address the way the Expo CLI does, prints the QR code itself
// and starts Metro on that address.
const { spawn } = require("child_process");
const path = require("path");

const PORT = 8081;

// lan-network and toqr come with the Expo CLI; resolve them from it so we use the same versions.
const expoCliDir = path.dirname(
  require.resolve("@expo/cli/package.json", { paths: [path.dirname(require.resolve("expo/package.json"))] }),
);
const fromExpoCli = (name) => require(require.resolve(name, { paths: [expoCliDir] }));
const { lanNetworkSync } = fromExpoCli("lan-network");
const { toQR } = fromExpoCli("toqr");

// Same rendering as the Expo CLI: two QR rows per line with half-block characters.
function renderQR(url) {
  const data = toQR(url);
  const extent = Math.sqrt(data.byteLength) | 0;
  const chars = ["\u2588", "\u2580", "\u2584", " "];
  let output = "\u2584".repeat(extent + 2);
  for (let row = 0; row < extent; row += 2) {
    output += "\n\u2588";
    for (let col = 0; col < extent; col++) {
      output += chars[(data[row * extent + col] << 1) | data[(row + 1) * extent + col]];
    }
    output += "\u2588";
  }
  if (extent % 2 === 0) output += "\n" + "\u2580".repeat(extent + 2);
  return output;
}

let address;
try {
  address = lanNetworkSync().address;
} catch {
  address = undefined;
}
if (!address || address.startsWith("127.")) {
  console.error("No local network address found. Connect this computer to the phone's Wi-Fi network.");
  process.exit(1);
}

const url = `exp://${address}:${PORT}`;
console.log(renderQR(url));
console.log(`\nScan the QR code with Expo Go (Android) or open ${url}`);
console.log(`The app calls the server at http://${address}:8080; keep :server:run running.\n`);

const metro = spawn(process.execPath, [require.resolve("expo/bin/cli"), "start", "--lan", "--port", String(PORT)], {
  stdio: "inherit",
  env: { ...process.env, REACT_NATIVE_PACKAGER_HOSTNAME: address },
});
metro.on("exit", (code, signal) => process.exit(signal ? 1 : code ?? 0));
for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => metro.kill(signal));
}
