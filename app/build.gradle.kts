plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val signingPath = System.getenv("HANI_KEYSTORE_PATH")
    ?: error("HANI_KEYSTORE_PATH is missing. The build must use the permanent signing key.")

val signingStorePassword = System.getenv("HANI_KEYSTORE_PASSWORD")
    ?: error("HANI_KEYSTORE_PASSWORD is missing.")

val signingKeyPassword = System.getenv("HANI_KEY_PASSWORD")
    ?: error("HANI_KEY_PASSWORD is missing.")

val signingAlias = System.getenv("HANI_KEY_ALIAS")
    ?: error("HANI_KEY_ALIAS is missing.")

android {
    namespace = "com.abihaniisa.hani"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abihaniisa.hani"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"
    }

    signingConfigs {
        create("permanent") {
            storeFile = file(signingPath)
            storePassword = signingStorePassword
            keyAlias = signingAlias
            keyPassword = signingKeyPassword
            storeType = "JKS"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("permanent")
        }
        release {
            signingConfig = signingConfigs.getByName("permanent")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
