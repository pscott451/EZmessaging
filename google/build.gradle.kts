@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinKapt)
    `maven-publish`
}

android {
    namespace = "com.scott.google"
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
    // Android Core
    implementation(libs.androidx.core.ktx)

    // OKHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
}

/*
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.pscott451"
            artifactId = "EZmessaginggoogle"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}*/
