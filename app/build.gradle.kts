plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "ru.debajo.locationprovider"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.debajo.locationprovider"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            keyAlias = rootProject.properties["RELEASE_KEY_ALIAS"] as? String
            keyPassword = rootProject.properties["RELEASE_KEY_PASSWORD"] as? String
            storeFile = file(rootProject.properties["RELEASE_STORE_FILE"] as String)
            storePassword = rootProject.properties["RELEASE_STORE_PASSWORD"] as? String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs["release"]
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }



    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.accompanist.permissions)
    implementation(libs.kotlinx.serialization)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.datetime)

    debugImplementation(libs.androidx.ui.tooling)
}