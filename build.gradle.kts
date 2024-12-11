buildscript {
    repositories {
        google() // Ensure Google Maven is included
        mavenCentral() // Include Maven Central for other dependencies
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.15") // Use the latest version
        classpath("com.android.tools.build:gradle:8.7.2") // Update if needed
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
        classpath("com.google.android.gms:play-services-auth:16.0.1")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
