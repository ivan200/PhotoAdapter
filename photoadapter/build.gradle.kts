plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = ("com.ivan200.photoadapter")

    compileSdk = rootProject.extra.get("compileSdkVersion") as Int

    defaultConfig {
        minSdk = rootProject.extra.get("minSdkVersion") as Int
        targetSdk = rootProject.extra.get("targetSdkVersion") as Int
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "$project.rootDir/tools/proguard-rules.pro")
        }
    }
}


dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    //appcompat
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.core:core-ktx:1.9.0")

    //Material 3
    implementation("com.google.android.material:material:1.7.0")

    //gallery view pager
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    //camera implementation for camera1
    //https://github.com/natario1/CameraView
    implementation("com.otaliastudios:cameraview:2.7.2")

    val cameraxVersion = "1.2.0"
    //CameraX (camera implementation for camera2)
    //https://developer.android.com/jetpack/androidx/releases/camera
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")

    //pages indicator in gallery
    //https://github.com/ongakuer/CircleIndicator
    implementation("me.relex:circleindicator:2.1.6")

    //Glide for preview images
    //https://github.com/bumptech/glide
    implementation("com.github.bumptech.glide:glide:4.14.2")
    kapt("com.github.bumptech.glide:compiler:4.14.2")

    //allow zoom in images
    //https://github.com/chrisbanes/PhotoView
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    //for flip frontal image
    implementation("androidx.exifinterface:exifinterface:1.3.5")

}
