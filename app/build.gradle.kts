plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.knownniu.douyinaweme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.knownniu.douyinaweme"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    compileOnly(files("libs/api-82.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// AGP 会自动给 debug 包加 android:testOnly=true，这里强制改成 false
// 否则 Run 按钮会因为 INSTALL_FAILED_TEST_ONLY 而失败
tasks.matching { it.name == "processDebugMainManifest" }.configureEach {
    doLast {
        val f = file("$buildDir/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
        if (f.exists()) f.writeText(f.readText().replace("testOnly=\"true\"", "testOnly=\"false\""))
    }
}