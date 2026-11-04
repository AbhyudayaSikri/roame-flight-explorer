import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.skie)
}

// Embeds fixtures/flights.json in commonTest as Kotlin source, so tests on the JVM and on the iOS
// simulator read the real data without file I/O. Literals stay under the JVM's 64 KB constant limit.
val generateTestFixture = tasks.register("generateTestFixture") {
    val fixture = rootProject.layout.projectDirectory.file("fixtures/flights.json")
    val outputDir = layout.buildDirectory.dir("generated/testFixture/kotlin")
    inputs.file(fixture)
    outputs.dir(outputDir)
    doLast {
        fun String.asKotlinLiteral(): String = buildString {
            append('"')
            for (c in this@asKotlinLiteral) {
                when (c) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '$' -> append("\\$")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(c)
                }
            }
            append('"')
        }
        val source = buildString {
            appendLine("package travel.roame.takehome")
            appendLine()
            appendLine("// Generated from fixtures/flights.json by :shared:generateTestFixture. Do not edit.")
            appendLine("internal val FIXTURE_JSON: String = listOf(")
            fixture.asFile.readText().chunked(16_000).forEach { appendLine("    ${it.asKotlinLiteral()},") }
            appendLine(").joinToString(\"\")")
        }
        outputDir.get().file("travel/roame/takehome/FixtureJson.kt").asFile.apply {
            parentFile.mkdirs()
            writeText(source)
        }
    }
}

kotlin {
    android {
        namespace = "travel.roame.takehome.shared"
        compileSdk { version = release(37) { minorApiLevel = 0 } }
        // kotlinx-datetime uses java.time on the JVM, which Android only has from API 26.
        minSdk = 26
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTest {}
    }
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework { baseName = "Shared"; isStatic = true }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest {
            kotlin.srcDir(generateTestFixture)
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
