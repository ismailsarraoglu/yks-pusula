import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

/*
 * Gizli değerler depoya yazılmaz. Önce keystore.properties (Git'e eklenmez), sonra ortam değişkenleri okunur.
 * Ayrıntı: play/play-yayin-talimat.md
 */
val localProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(name: String): String? =
    localProps.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() } ?: System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }

/** Google'ın herkese açık test reklam kimlikleri. Yalnızca debug ve yan yükleme test derlemesinde kullanılır. */
val testAdAppId = "ca-app-pub-3940256099942544~3347511713"
val testAdBannerId = "ca-app-pub-3940256099942544/9214589741"
val testAdNativeId = "ca-app-pub-3940256099942544/2247696110"
val googleTestPublisher = "ca-app-pub-3940256099942544"

/**
 * Yalnızca debug derlemesinde satın alma doğrulamasını test etmek için üretilmiş anahtar.
 * Google Play'in anahtarı değildir; bununla imzalanmış satın alma Play'den gelemez.
 */
val debugPurchaseTestKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtDzyyx+zbAlYZLvhYjiXXhD9lRLcblfR2riQfB247gbF6tT83BD2lsByu9it838OVUPGezsjJ6fuiU0PX1neIXm8x1von+83olQZFiK8ygstNjFR0FxkPgy6X6v/LPFlRIA7a0R5biQNo9sjaeDcWw6C3rRs6xdTIiwojiqiuFO1jNxtkyF49jtULYOGc6tS2J9XYmiix8JpQLMwNNFZ/fh0Jya3qSuvWiDouROe0irvVy6VS+dzQp8W34l2RHbGYBCAkWznrccQdKyJSyo2MrcUwKtrJVlJqHbEWisOorJexPK3lf5THAZ/hxOYSdwfWUbYgOIJIkyqZOSwHgPDVwIDAQAB"

val uploadStore = secret("UPLOAD_STORE_FILE")
val sideloadStore = secret("SIDELOAD_STORE_FILE")

android {
    namespace = "app.ykspusula"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.ykspusula"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "2.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Bu test internet ister; bağlantılı emülatörde ayrıca çalıştırılır.
        testInstrumentationRunnerArguments["notClass"] = "app.ykspusula.AdOnlineTest,app.ykspusula.StresTest,app.ykspusula.SystemIntegrationTest,app.ykspusula.NotificationDeniedTest,app.ykspusula.GateProbeTest"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        // Yan yükleme test anahtarı depoda değildir. Verilmezse yan yükleme derlemesi debug anahtarıyla imzalanır.
        if (sideloadStore != null) {
            create("sideload") {
                storeFile = rootProject.file(sideloadStore)
                storeType = "PKCS12"
                storePassword = secret("SIDELOAD_STORE_PASSWORD")
                keyAlias = secret("SIDELOAD_KEY_ALIAS") ?: "ykspusula"
                keyPassword = secret("SIDELOAD_KEY_PASSWORD") ?: secret("SIDELOAD_STORE_PASSWORD")
            }
        }
        // Google Play yükleme anahtarı. Yoksa release derlemesi durur; başka anahtara dönülmez.
        if (uploadStore != null) {
            create("upload") {
                storeFile = rootProject.file(uploadStore)
                storePassword = secret("UPLOAD_STORE_PASSWORD")
                keyAlias = secret("UPLOAD_KEY_ALIAS")
                keyPassword = secret("UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdBannerId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$testAdNativeId\"")
            buildConfigField("String", "PLAY_PUBLIC_KEY", "\"$debugPurchaseTestKey\"")
            buildConfigField("String", "DISTRIBUTION", "\"DEBUG\"")
        }
        // Google Play'e yüklenecek tek derleme. Gerçek kimlikler ve yükleme anahtarı olmadan üretilemez.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("upload")
            manifestPlaceholders["admobAppId"] = secret("ADMOB_APP_ID") ?: "EKSIK"
            buildConfigField("String", "ADMOB_APP_ID", "\"${secret("ADMOB_APP_ID").orEmpty()}\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"${secret("ADMOB_BANNER_ID").orEmpty()}\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"${secret("ADMOB_NATIVE_ID").orEmpty()}\"")
            buildConfigField("String", "PLAY_PUBLIC_KEY", "\"${secret("PLAY_PUBLIC_KEY").orEmpty()}\"")
            buildConfigField("String", "DISTRIBUTION", "\"PLAY\"")
        }
        // Telefona doğrudan kurulan test sürümü: release ile aynı küçültme ayarları, Google test reklamları, satın alma kapalı.
        create("sideload") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.findByName("sideload") ?: signingConfigs.getByName("debug")
            manifestPlaceholders["admobAppId"] = testAdAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdAppId\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$testAdBannerId\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$testAdNativeId\"")
            buildConfigField("String", "PLAY_PUBLIC_KEY", "\"\"")
            buildConfigField("String", "DISTRIBUTION", "\"SIDELOAD\"")
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

/** Play derlemesi eksik veya test yapılandırmasıyla üretilmesin diye derleme başında denetlenir. */
val releaseDenetimi = tasks.register("releaseYapilandirmasiniDenetle") {
    val values = mapOf(
        "UPLOAD_STORE_FILE" to uploadStore, "UPLOAD_STORE_PASSWORD" to secret("UPLOAD_STORE_PASSWORD"),
        "UPLOAD_KEY_ALIAS" to secret("UPLOAD_KEY_ALIAS"), "UPLOAD_KEY_PASSWORD" to secret("UPLOAD_KEY_PASSWORD"),
        "ADMOB_APP_ID" to secret("ADMOB_APP_ID"), "ADMOB_NATIVE_ID" to secret("ADMOB_NATIVE_ID"), "PLAY_PUBLIC_KEY" to secret("PLAY_PUBLIC_KEY"),
    )
    val storeExists = uploadStore?.let { rootProject.file(it).exists() } ?: false
    doLast {
        val problems = ArrayList<String>()
        values.filterValues { it == null }.keys.forEach { problems += "$it tanımlı değil" }
        if (uploadStore != null && !storeExists) problems += "UPLOAD_STORE_FILE dosyası bulunamadı: $uploadStore"
        listOf("ADMOB_APP_ID", "ADMOB_NATIVE_ID").forEach { k ->
            if (values[k]?.startsWith(googleTestPublisher) == true) problems += "$k Google test kimliği; Play derlemesinde gerçek AdMob kimliği gerekir"
        }
        if (problems.isNotEmpty()) throw GradleException(
            "Google Play derlemesi durduruldu. Eksik yapılandırma:\n- " + problems.joinToString("\n- ") +
                "\nDeğerleri keystore.properties dosyasına veya ortam değişkenlerine girin (play/play-yayin-talimat.md). " +
                "Telefonda denemek için assembleSideload kullanın.",
        )
    }
}
tasks.configureEach { if (name == "preReleaseBuild") dependsOn(releaseDenetimi) }

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
    // Google Play Families Self-Certified Ads SDK listesindeki Google Mobile Ads SDK.
    implementation("com.google.android.gms:play-services-ads:25.5.0")
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
