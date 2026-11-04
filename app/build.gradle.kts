import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Firebase подключается только если рядом лежит google-services.json
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Настройки Cloudinary (для прикрепления фото на форуме) берутся из local.properties или переменных окружения
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun cloudinaryProp(key: String, env: String): String =
    (localProps.getProperty(key) ?: System.getenv(env) ?: "").trim()

android {
    namespace = "ru.huegrad.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.huegrad.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "2.5"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${cloudinaryProp("cloudinary.cloudName", "CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${cloudinaryProp("cloudinary.uploadPreset", "CLOUDINARY_UPLOAD_PRESET")}\"")
    }

    buildTypes {
        release {
            // R8 убирает неиспользуемый код, а сжатие ресурсов выбрасывает неиспользуемые файлы (keep.xml защищает картинки и треки)
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    // Применяет готовые профили запуска Compose и других библиотек и в установленном вручную APK
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.coil.compose)
    implementation(libs.jsoup)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
}
