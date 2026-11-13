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
// Значения по умолчанию те же, что в forum.js сайта: так вложения из приложения видны и на сайте
fun cloudinaryProp(key: String, env: String, default: String): String =
    (localProps.getProperty(key) ?: System.getenv(env) ?: default).trim()

android {
    namespace = "ru.huegrad.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.huegrad.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "3.0"

        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${cloudinaryProp("cloudinary.cloudName", "CLOUDINARY_CLOUD_NAME", "xwhjf44a")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${cloudinaryProp("cloudinary.uploadPreset", "CLOUDINARY_UPLOAD_PRESET", "dealdo")}\"")
    }

    buildTypes {
        release {
            // R8 сжимает и обфусцирует код. Сжатие ресурсов выключено: картинки и треки ищутся по имени
            // (getIdentifier и android.resource://), а такие ресурсы в release вырезаются или переименовываются,
            // и вместо картинок остаётся заглушка, а треки не играют.
            isMinifyEnabled = true
            isShrinkResources = false
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
    implementation(libs.media3.ui) // плеер с элементами управления для видео на форуме

    implementation(libs.coil.compose)
    implementation(libs.haze) // размытие фона под плавающей панелью ввода (матовое стекло)
    implementation(libs.jsoup)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
}
