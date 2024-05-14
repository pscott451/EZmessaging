// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.jetbrainsKotlinKapt) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.junit5) apply false
}

val publishGroup by extra { "com.github.pscott451" }
val publishArtifactID by extra { "EZmessaging" }
val publishVersion by extra { "1.0.2" }