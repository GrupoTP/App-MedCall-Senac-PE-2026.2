# Marks build outputs as ignored by Dropbox (com.dropbox.ignored), so a clone inside a Dropbox folder
# can be built without Dropbox syncing gigabytes of generated files or locking them mid-build.
# Missing folders are created first, because the flag only sticks to an existing folder.
# Run again after `gradlew clean`, which deletes the build/ folders. `expo prebuild` recreates android/,
# and the config plugin in client/expo/plugins flags it again.
$ErrorActionPreference = "Stop"

$repo = Split-Path -Parent $PSScriptRoot
$client = Join-Path $repo "client/expo"
$targets = [System.Collections.Generic.List[string]]::new()

# Every Gradle project, including the native modules inside node_modules: its build/ and, for native
# Android libraries, the CMake .cxx/ folder. Every Gradle build root: its .gradle/ and .kotlin/ caches.
$gradleFiles = Get-ChildItem $repo -Recurse -File -Include build.gradle, build.gradle.kts, settings.gradle, settings.gradle.kts |
    Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.cxx)[\\/]' -and -not $_.FullName.StartsWith((Join-Path $client "android")) }
foreach ($file in $gradleFiles) {
    $dir = $file.DirectoryName
    if ($file.Name -like "settings.gradle*") {
        $targets.Add((Join-Path $dir ".gradle"))
        $targets.Add((Join-Path $dir ".kotlin"))
    } else {
        $targets.Add((Join-Path $dir "build"))
        if ((Split-Path -Leaf $dir) -eq "android") { $targets.Add((Join-Path $dir ".cxx")) }
    }
}

# Expo: the generated native project (gitignored, recreated by prebuild), the CLI cache and the web export.
$targets.Add((Join-Path $client "android"))
$targets.Add((Join-Path $client ".expo"))
$targets.Add((Join-Path $client "dist"))

foreach ($target in $targets | Sort-Object -Unique) {
    if (-not (Test-Path $target)) { New-Item -ItemType Directory $target | Out-Null }
    Set-Content -Path $target -Stream com.dropbox.ignored -Value 1
    Write-Output "ignored: $($target.Substring($repo.Length + 1))"
}
