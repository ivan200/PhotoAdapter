plugins {
    alias(libs.plugins.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.plugin.parcelize)
}

android {
    namespace = ("com.ivan200.photoadapter")
    compileSdk = libs.versions.compileSdkVersion.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdkVersion.get().toInt()
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    //appcompat
    implementation(libs.appcompat)
    implementation(libs.core.ktx)

    //Material 3
    implementation(libs.material)

    //gallery view pager
    implementation(libs.viewpager2)

    //camera implementation for camera1
    implementation(libs.cameraview)

    //CameraX (camera implementation for camera2)
    implementation(libs.camera.camera2)
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)

    //pages indicator in gallery
    implementation(libs.circleindicator)

    //Glide for preview images
    implementation(libs.glide)
    kapt(libs.glide)

    //allow zoom in images
    implementation(libs.photoview)

    //for flip frontal image
    implementation(libs.exifinterface)
}
