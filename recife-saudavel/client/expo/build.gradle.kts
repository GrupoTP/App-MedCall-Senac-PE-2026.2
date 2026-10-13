// Gradle wrapper for the Expo shell: npm and npx run through these tasks, on Windows and Linux.

val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

// npm and npx are .cmd scripts on Windows, which only cmd can run.
fun nodeCommand(vararg args: String): List<String> =
    if (isWindows) listOf("cmd", "/c") + args else args.toList()

// npm ci reinstalls node_modules exactly as package-lock.json says. npm writes
// node_modules/.package-lock.json at the end, so the task stays UP-TO-DATE until the
// lock file changes or node_modules is deleted.
val npmInstall = tasks.register<Exec>("npmInstall") {
    group = "recife saudavel"
    description = "Installs the client/expo npm dependencies when package-lock.json changes."
    inputs.files("package.json", "package-lock.json").withPropertyName("packageFiles")
    outputs.file("node_modules/.package-lock.json").withPropertyName("installedLock")
    commandLine(nodeCommand("npm", "ci"))
}

fun Exec.expo(vararg args: String) {
    group = "recife saudavel"
    dependsOn(npmInstall, ":client:app:buildClientBundle")
    standardInput = System.`in`
    commandLine(nodeCommand("npx", "expo", *args))
}

tasks.register<Exec>("startWeb") {
    description = "Starts the PWA on http://localhost:8081 (npx expo start --web)."
    expo("start", "--web")
}

// Under Gradle stdout is not a terminal, so `expo start` would skip the QR code; the script prints it.
tasks.register<Exec>("startLan") {
    group = "recife saudavel"
    description = "Starts Metro on the local network and prints the Expo Go QR code."
    dependsOn(npmInstall, ":client:app:buildClientBundle")
    standardInput = System.`in`
    commandLine("node", "scripts/start-lan.js")
}

val exportWeb =tasks.register<Exec>("exportWeb") {
    description = "Exports the PWA to client/expo/dist (npx expo export --platform web)."
    expo("export", "--platform", "web")
}

tasks.register<Exec>("serveWeb") {
    description = "Serves the exported PWA on http://localhost:8081 (npx expo serve --port 8081)."
    dependsOn(exportWeb)
    expo("serve", "--port", "8081")
}

// The app calls http://localhost:8080; adb reverse forwards it to the server on this machine.
val adbReverse = tasks.register<Exec>("adbReverse") {
    group = "recife saudavel"
    description = "Forwards the device port 8080 to this machine (adb reverse tcp:8080 tcp:8080)."
    commandLine("adb", "reverse", "tcp:8080", "tcp:8080")
}

tasks.register<Exec>("runAndroid") {
    description = "Builds and installs the release app on the USB device (npx expo run:android --variant release)."
    dependsOn(adbReverse)
    expo("run:android", "--variant", "release")
}
