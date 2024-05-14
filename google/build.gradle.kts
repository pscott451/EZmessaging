@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

val publishGroup: String by rootProject.extra
val publishArtifactID: String by rootProject.extra
val publishVersion: String by rootProject.extra

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
    useLibrary("org.apache.http.legacy")

    publishing {
        publishing {
            singleVariant("release") {
                withSourcesJar()
                withJavadocJar()
            }
        }
    }
}

dependencies {
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = publishGroup
            artifactId = publishArtifactID
            version = publishVersion

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}