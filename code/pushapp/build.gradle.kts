import com.adobe.marketing.mobile.gradle.BuildConstants

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

val mavenCoreVersion: String by project
val mavenLifecycleVersion: String by project
val mavenAssuranceVersion: String by project
val mavenEdgeVersion: String by project
val mavenEdgeIdentityVersion: String by project

android {
    namespace = "com.adobe.marketing.mobile.pushtestapp"

    defaultConfig {
        applicationId = "com.adobe.marketing.mobile.pushtestapp"
        compileSdk = BuildConstants.Versions.COMPILE_SDK_VERSION
        minSdk = BuildConstants.Versions.MIN_SDK_VERSION
        targetSdk = BuildConstants.Versions.TARGET_SDK_VERSION
        versionCode = 1
        versionName = "1.0"
    }

    kotlinOptions {
        jvmTarget = BuildConstants.Versions.KOTLIN_JVM_TARGET
        languageVersion = BuildConstants.Versions.KOTLIN_LANGUAGE_VERSION
        apiVersion = BuildConstants.Versions.KOTLIN_API_VERSION
    }

    buildTypes {
        getByName(BuildConstants.BuildTypes.RELEASE) {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.6"
    }
}

dependencies {
    implementation(project(":messaging"))
    implementation("com.adobe.marketing.mobile:core:$mavenCoreVersion")
    implementation("com.adobe.marketing.mobile:lifecycle:$mavenLifecycleVersion")
    implementation("com.adobe.marketing.mobile:assurance:$mavenAssuranceVersion")
    implementation("com.adobe.marketing.mobile:edge:$mavenEdgeVersion")
    implementation("com.adobe.marketing.mobile:edgeidentity:$mavenEdgeIdentityVersion")

    implementation("com.google.firebase:firebase-messaging:23.4.1")

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
}
