plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.kuscher.discosweeper"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.kuscher.discosweeper"
        minSdk = 31
        // 36, not 37: the windowing behaviour in docs/WINDOWING.md was measured against 36.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    // Release signing from ~/.config/discosweeper (never committed). Absent -> unsigned release build.
    val keyDir = File(System.getProperty("user.home"), ".config/discosweeper")
    val keyFile = File(keyDir, "keystore.jks")
    val keyPassFile = File(keyDir, "keystore.pass")
    signingConfigs {
        if (keyFile.exists() && keyPassFile.exists()) {
            create("release") {
                storeFile = keyFile
                val pw = keyPassFile.readText().trim()
                storePassword = pw
                keyAlias = "discosweeper"
                keyPassword = pw
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
