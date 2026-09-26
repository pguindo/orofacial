plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pguindo.orofacial"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pguindo.orofacial"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.0.5"
    }

    // Firma estable para sideload: keystore propio versionado en keystore/ (generado una
    // vez por CI). Sin él (p. ej. compilación local), se usa la clave debug.
    val ksFile = rootProject.file("keystore/orofacial.jks")
    val ksPass = System.getenv("OROFACIAL_KS_PASS").let { if (it.isNullOrEmpty()) "orofacial-sideload" else it }
    signingConfigs {
        create("orofacial") {
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = ksPass
                keyAlias = "orofacial"
                keyPassword = ksPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksFile.exists()) signingConfigs.getByName("orofacial") else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
