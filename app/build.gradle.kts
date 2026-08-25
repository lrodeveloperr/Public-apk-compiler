import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "studio.gooduse.kitchenprep"
    compileSdk = 36

    defaultConfig {
        applicationId = "studio.gooduse.kitchenprep"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        buildConfigField("String", "REMOVE_ADS_PRODUCT_ID", "\"remove_ads_monthly\"")
        buildConfigField("String", "REMOVE_ADS_BASE_PLAN_ID", "\"monthly\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://lrodeveloperr.github.io/kitchen-prep-board-policies-repo/privacy/\"")
        buildConfigField("String", "TERMS_URL", "\"https://lrodeveloperr.github.io/kitchen-prep-board-policies-repo/terms/\"")
        buildConfigField("String", "SUPPORT_URL", "\"https://lrodeveloperr.github.io/kitchen-prep-board-policies-repo/support/\"")
        buildConfigField("String", "SAFETY_URL", "\"https://lrodeveloperr.github.io/kitchen-prep-board-policies-repo/safety/\"")
        buildConfigField("String", "SUBSCRIPTION_URL", "\"https://lrodeveloperr.github.io/kitchen-prep-board-policies-repo/subscription/\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(layout.buildDirectory.dir("generated/kpbAssets").get().asFile)
            res.srcDir(layout.buildDirectory.dir("generated/kpbRes").get().asFile)
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val generatedAssets = layout.buildDirectory.dir("generated/kpbAssets")
val generatedRes = layout.buildDirectory.dir("generated/kpbRes")

val generateKitchenNativeInputs by tasks.registering {
    val htmlParts = rootProject.fileTree("compiler-input") { include("html.part*") }
    val iconSource = rootProject.file("compiler-input/icons/icon-mdpi.b64")
    inputs.files(htmlParts)
    inputs.file(iconSource)
    outputs.dir(generatedAssets)
    outputs.dir(generatedRes)

    doLast {
        val assetsDir = generatedAssets.get().asFile
        val resDir = generatedRes.get().asFile
        assetsDir.mkdirs()

        val html = htmlParts.files.sortedBy { it.name }.joinToString(separator = "") { it.readText() }
        val marker = "const I18N="
        val start = html.indexOf(marker)
        check(start >= 0) { "I18N dictionary not found in compiler-input" }
        val jsonStart = start + marker.length
        var depth = 0
        var inString = false
        var escaped = false
        var jsonEnd = -1
        for (index in jsonStart until html.length) {
            val ch = html[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            jsonEnd = index + 1
                            break
                        }
                    }
                }
            }
        }
        check(jsonEnd > jsonStart) { "I18N dictionary terminator not found" }
        assetsDir.resolve("i18n.json").writeText(html.substring(jsonStart, jsonEnd).trim())

        val clean = iconSource.readText().replace(Regex("[^A-Za-z0-9+/=]"), "")
        val png = Base64.getDecoder().decode(clean)
        check(png.size > 8 && png.copyOfRange(0, 8).contentEquals(
            byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        )) { "launcher icon source is not a PNG" }
        val mipmap = resDir.resolve("mipmap-mdpi").apply { mkdirs() }
        mipmap.resolve("ic_launcher.png").writeBytes(png)
        mipmap.resolve("ic_launcher_round.png").writeBytes(png)
    }
}

tasks.named("preBuild").configure {
    dependsOn(generateKitchenNativeInputs)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("com.android.billingclient:billing-ktx:9.1.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
