// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google() // Ensure Google Maven is included
        mavenCentral() // Include Maven Central for other dependencies
    }
    dependencies {
        classpath("com.google.gms:google-services:4.3.15") // Use the latest version
        classpath("com.android.tools.build:gradle:8.1.4") // Update if needed
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
