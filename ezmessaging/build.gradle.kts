@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinKapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.junit5)
    `maven-publish`
}

android {
    namespace = "com.scott.ezmessaging"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    publishing {
        publishing {
            singleVariant("release") {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
    useLibrary("org.apache.http.legacy")
}

dependencies {
    //implementation(project(":google"))

    // Android Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.coroutines)

    // Hilt
    kapt(libs.hilt.compiler)
    api(libs.hilt)

    // OKHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.pscott451"
            artifactId = "EZmessaging"
            version = "1.1.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}