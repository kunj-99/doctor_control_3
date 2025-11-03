plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.infowave.doctor_control"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.infowave.doctor_control"
        minSdk = 23
        targetSdk = 35
        versionCode = 4
        versionName = "3.1"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.swiperefreshlayout)
    implementation(libs.gridlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation ("com.android.volley:volley:1.2.1")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.google.android.gms:play-services-location:19.0.0")
       implementation ("com.google.android.gms:play-services-maps:18.1.0")



    implementation ("com.google.android.material:material:1.11.0")


    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.23")

    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // Core analytics (optional but nice for console testing)
    implementation("com.google.firebase:firebase-analytics")
    // ⚡ Cloud Messaging – REQUIRED for push notifications
    implementation("com.google.firebase:firebase-messaging")
}