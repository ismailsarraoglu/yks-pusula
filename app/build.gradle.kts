import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

val localProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String, default: String): String = localProps.getProperty(name) ?: default

android {
    namespace = "app.ykspusula"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.ykspusula"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Varsayılanlar Google'ın test kimlikleridir. Üretim değerleri keystore.properties dosyasından gelir.
        val testAppId = "ca-app-pub-3940256099942544~3347511713"
        manifestPlaceholders["admobAppId"] = prop("ADMOB_APP_ID", testAppId)
        buildConfigField("String", "ADMOB_APP_ID", "\"${prop("ADMOB_APP_ID", testAppId)}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${prop("ADMOB_BANNER_ID", "ca-app-pub-3940256099942544/9214589741")}\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"${prop("ADMOB_NATIVE_ID", "ca-app-pub-3940256099942544/2247696110")}\"")
        buildConfigField("String", "PLAY_PUBLIC_KEY", "\"${prop("PLAY_PUBLIC_KEY", "")}\"")
    }

    signingConfigs {
        create("sideload") {
            storeFile = file("signing/yks-pusula-sideload.p12")
            storeType = "PKCS12"
            storePassword = "KALDIRILDI"
            keyAlias = "ykspusula"
            keyPassword = "KALDIRILDI"
        }
        // Google Play yükleme anahtarı keystore.properties ile verilir; depoya konmaz.
        if (localProps.getProperty("UPLOAD_STORE_FILE") != null) {
            create("upload") {
                storeFile = rootProject.file(localProps.getProperty("UPLOAD_STORE_FILE"))
                storePassword = localProps.getProperty("UPLOAD_STORE_PASSWORD")
                keyAlias = localProps.getProperty("UPLOAD_KEY_ALIAS")
                keyPassword = localProps.getProperty("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("sideload")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions { animationsDisabled = true }
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.10.2")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    implementation("com.android.billingclient:billing:9.1.0")
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.6.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.1")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
