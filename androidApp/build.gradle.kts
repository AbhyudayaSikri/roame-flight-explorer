import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Override with -PapiBaseUrl=http://<host>:8080 (default: the host machine as seen from the emulator).
// A localhost address also makes debug builds forward its port with adb reverse (see adbReverse below).
val apiBaseUrl = providers.gradleProperty("apiBaseUrl").getOrElse("http://10.0.2.2:8080")

android {
    buildToolsVersion = "37.0.0"
    namespace = "travel.roame.takehome.android"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig {
        applicationId = "travel.roame.takehome.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

// A device reaches this computer's localhost only through `adb reverse`, which lasts until the device
// restarts. So with a localhost apiBaseUrl (a phone on USB, or an emulator that can't reach 10.0.2.2),
// every debug build sets it up on each connected device. Android Studio's Run builds assembleDebug.
val reversedPort = Regex("""^https?://(?:localhost|127\.0\.0\.1):(\d+)(?:/.*)?$""").find(apiBaseUrl)?.groupValues?.get(1)
if (reversedPort != null) {
    val adbReverse = tasks.register<AdbReverse>("adbReverse") {
        adb.set(androidComponents.sdkComponents.adb)
        port.set(reversedPort)
    }
    tasks.configureEach {
        if (name == "assembleDebug" || name == "installDebug") finalizedBy(adbReverse)
    }
}

/** `adb reverse tcp:<port> tcp:<port>` on every connected device. It never fails the build. */
abstract class AdbReverse @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Internal abstract val adb: RegularFileProperty
    @get:Input abstract val port: Property<String>

    @TaskAction
    fun forward() {
        val adb = adb.get().asFile.absolutePath
        val port = port.get()
        val listing = ByteArrayOutputStream()
        runCatching {
            exec.exec {
                commandLine(adb, "devices")
                standardOutput = listing
                isIgnoreExitValue = true
            }
        }
        val serials = listing.toString().lines()
            .map { it.split('\t') }
            .filter { it.size == 2 && it[1].trim() == "device" }
            .map { it[0] }
        if (serials.isEmpty()) {
            logger.lifecycle("adbReverse: no device is connected, so the app can't reach localhost:$port yet. Start the emulator, then build again.")
        }
        for (serial in serials) {
            val forwarded = runCatching {
                exec.exec {
                    commandLine(adb, "-s", serial, "reverse", "tcp:$port", "tcp:$port")
                    isIgnoreExitValue = true
                }.exitValue == 0
            }.getOrDefault(false)
            logger.lifecycle(
                if (forwarded) "adbReverse: localhost:$port on $serial now reaches this computer."
                else "adbReverse: couldn't forward localhost:$port on $serial."
            )
        }
    }
}

composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
    // -PcomposeReports writes the compiler's stability reports to androidApp/build/compose-reports.
    if (providers.gradleProperty("composeReports").isPresent) {
        reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
        metricsDestination.set(layout.buildDirectory.dir("compose-reports"))
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.maplibre.android)
    // Debug builds only: watches for leaked activities, fragments, views and view models (QA T2).
    debugImplementation(libs.leakcanary.android)

    // Emulator UI tests against the real local API (.github/workflows/android-ui.yml).
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
