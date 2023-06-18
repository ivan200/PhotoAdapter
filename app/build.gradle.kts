plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = ("com.ivan200.photoadapterexample")
    compileSdk = rootProject.extra.get("compileSdkVersion") as Int
    @Suppress("UnstableApiUsage")
    defaultConfig {
        applicationId = "com.ivan200.photoadapterexample"
        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }
    buildTypes {
        @Suppress("UnstableApiUsage")
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "$project.rootDir/tools/proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    //appcompat
    //https://mvnrepository.com/artifact/androidx.appcompat/appcompat
    implementation("androidx.appcompat:appcompat:1.6.1")
    //https://mvnrepository.com/artifact/androidx.constraintlayout/constraintlayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //multidex
    implementation("androidx.multidex:multidex:2.0.1")

    //Navigation
    //https://mvnrepository.com/artifact/androidx.navigation/navigation-fragment-ktx
    implementation("androidx.navigation:navigation-fragment-ktx:2.6.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.6.0")

    //Preference
    //https://mvnrepository.com/artifact/androidx.preference/preference-ktx
    implementation("androidx.preference:preference-ktx:1.2.0")

    //preview photo in gallery
    //https://github.com/chrisbanes/PhotoView
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //Glide for preview images
    //https://github.com/bumptech/glide
    implementation("com.github.bumptech.glide:glide:4.15.1")
    kapt("com.github.bumptech.glide:compiler:4.15.1")

    implementation(project(":photoadapter"))
}