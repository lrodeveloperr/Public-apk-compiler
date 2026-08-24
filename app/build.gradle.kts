plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "studio.gooduse.kitchenprep"
    compileSdk = 37

    defaultConfig {
        applicationId = "studio.gooduse.kitchenprep"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        // Closed-testing build intentionally uses Google's official demo AdMob IDs.
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
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.core:core-ktx:1.17.0")

    implementation("com.android.billingclient:billing-ktx:9.1.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}
