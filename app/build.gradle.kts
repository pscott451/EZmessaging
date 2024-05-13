@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    //id("kotlin-kapt")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinKapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.junit5)
}

android {
    compileSdk = 34

    defaultConfig {
        applicationId = "com.scott.app"
        minSdk = 26
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
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildFeatures.buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.2"
    }
}

dependencies {
    implementation(project(":ezmessaging"))

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.coroutines)

    // Coil (Displaying Gifs)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.compose.constraintlayout)
    implementation(libs.androidx.compose.activity)
    implementation(libs.androidx.lifecycle.viewmodelcompose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Hilt
    kapt(libs.hilt.compiler)
    implementation(libs.hilt)

    // Moshi
    kapt(libs.moshi.codegen)
    implementation(libs.moshi)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter)

    // Unit Test
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.coroutines)
    testRuntimeOnly(libs.test.junit5engine)
    testImplementation(libs.test.junit5api)
    testImplementation(libs.test.junit5parameterized)
    testImplementation(libs.test.junit4)

    // Android Test
    androidTestRuntimeOnly(libs.test.junit5engine)
    androidTestImplementation(libs.test.junit5api)
    androidTestImplementation(libs.test.junit5parameterized)
    androidTestImplementation(libs.androidtest.core)
    androidTestImplementation(libs.androidtest.runner)
    androidTestImplementation(libs.androidtest.rules)
    androidTestImplementation(libs.androidtest.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidtest.junit4)
    androidTestImplementation(libs.androidtest.junit4.compose)
    androidTestImplementation(libs.androidtest.mockk)

    // Shared Test
    api(libs.sharedtest.kotest.api)
    api(libs.sharedtest.kotest.core)
    api(libs.sharedtest.kotest.shared)
}