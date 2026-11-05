import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// JVM 17 bytecode so the server also runs on Android Studio's bundled JDK, not only the Gradle daemon's 25.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

application {
    mainClass.set("travel.roame.takehome.server.MainKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.kotlinx.serialization.json)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
}

tasks.test {
    systemProperty("fixture.path", rootProject.file("fixtures/flights.json").absolutePath)
}

// The fixture ships inside the server, so it serves the same bytes from any working directory.
tasks.processResources {
    from(rootProject.layout.projectDirectory.file("fixtures/flights.json"))
}

interface ArchiveAccess {
    @get:Inject val archives: ArchiveOperations
}

// build/libs/roame-flights-server.jar: the server, its dependencies and the fixture, for `java -jar`.
// Every META-INF/services file appears in only one dependency, so keeping the first copy of a
// duplicate loses nothing.
tasks.register<Jar>("fatJar") {
    group = "distribution"
    description = "Builds a single jar that runs the API with java -jar."
    archiveFileName.set("roame-flights-server.jar")
    manifest { attributes["Main-Class"] = "travel.roame.takehome.server.MainKt" }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val archives = objects.newInstance<ArchiveAccess>().archives
    val runtime = configurations.runtimeClasspath
    dependsOn(runtime)
    from(sourceSets.main.map { it.output })
    from(runtime.map { files -> files.map { if (it.isDirectory) it else archives.zipTree(it) } }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.EC", "module-info.class", "META-INF/versions/*/module-info.class")
    }
}
