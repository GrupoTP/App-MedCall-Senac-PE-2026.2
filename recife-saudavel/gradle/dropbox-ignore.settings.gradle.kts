// Keeps generated folders out of Dropbox when the clone lives in a Dropbox folder, so the computers
// sharing it don't sync gigabytes of build output, lock files mid-build or copy the H2 database
// while the server writes it. Each computer builds and installs its own copies.
//
// Dropbox reads the com.dropbox.ignored attribute: an NTFS alternate data stream on Windows and a
// user extended attribute on Linux and macOS; Java's UserDefinedFileAttributeView writes either.
// The attribute goes away with its folder, so it is set again on every build, before any task runs,
// and after the tasks that delete folders (clean, npm ci, expo export). Missing folders are created
// first, because the attribute only sticks to an existing folder.
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.attribute.UserDefinedFileAttributeView

val dropboxIgnoredAttribute = "com.dropbox.ignored"

// The Dropbox root holds a .dropbox file.
val insideDropbox = generateSequence(settingsDir) { it.parentFile }.any { File(it, ".dropbox").isFile }

fun ignoreInDropbox(dir: File, create: Boolean) {
    if (!dir.isDirectory) {
        if (!create || !dir.mkdirs()) return
    }
    val view = Files.getFileAttributeView(dir.toPath(), UserDefinedFileAttributeView::class.java) ?: return
    if (dropboxIgnoredAttribute !in view.list()) {
        view.write(dropboxIgnoredAttribute, ByteBuffer.wrap("1".toByteArray()))
    }
}

fun projectDirs(project: ProjectDescriptor): Sequence<File> =
    sequenceOf(project.projectDir) + project.children.asSequence().flatMap(::projectDirs)

fun ignoreGeneratedFolders() {
    if (!insideDropbox) return
    for (buildRoot in listOf(settingsDir, settingsDir.resolve("grupotp-kotlin-react-native"))) {
        for (cache in listOf(".gradle", ".kotlin", "build")) {
            ignoreInDropbox(buildRoot.resolve(cache), create = true)
        }
    }
    projectDirs(settings.rootProject).forEach { ignoreInDropbox(it.resolve("build"), create = true) }

    val expo = settingsDir.resolve("client/expo")
    for (generated in listOf("node_modules", ".expo", "dist")) {
        ignoreInDropbox(expo.resolve(generated), create = true)
    }
    // `expo run:android` only runs prebuild when android/ is missing, so it is never created here;
    // plugins/withKotlinAndroidBuild.js flags it right after prebuild.
    ignoreInDropbox(expo.resolve("android"), create = false)

    // H2 database of `:server:run`, one per computer. H2 is temporary; drop this with the move to MySQL.
    ignoreInDropbox(settingsDir.resolve("server/data"), create = true)
}

ignoreGeneratedFolders()

val deletesIgnoredFolders = setOf(":client:expo:npmInstall", ":client:expo:exportWeb")
gradle.allprojects {
    tasks.configureEach {
        if (name == "clean" || path in deletesIgnoredFolders) {
            doLast { ignoreGeneratedFolders() }
        }
    }
}
