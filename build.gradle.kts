// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.3.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")

        // https://mvnrepository.com/artifact/io.realm/realm-gradle-plugin
        classpath("io.realm:realm-gradle-plugin:10.11.1")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

val minSdkVersion by rootProject.extra { 14 }
val targetSdkVersion by rootProject.extra { 33 }
val compileSdkVersion by rootProject.extra { 33 }
val buildToolsVersion by rootProject.extra { "33.0.0" }
