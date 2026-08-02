import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.devtools.ksp)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    compileSdk = 37
    namespace = "safe.kernel.flash"

    defaultConfig {
        applicationId = "safe.kernel.flash"
        minSdk = 29
        targetSdk = 36
        versionCode = 20600
        versionName = "2.6"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                )
            }
        }

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters.add("arm64-v8a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
        }

        val keystorePropertiesFile = rootProject.file("keystore.properties")
        val keystoreProperties = Properties()
        if (keystorePropertiesFile.exists()) {
            keystoreProperties.load(keystorePropertiesFile.inputStream())
        }

        signingConfigs {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile", "RKF-release-key.jks"))
                storePassword = keystoreProperties.getProperty("storePassword", "")
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
            }
        }

        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
                isMinifyEnabled = false
                isShrinkResources = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
                )
            }
        }

        sourceSets {
            getByName("main") {
                jniLibs.srcDirs("src/main/jniLibs")
            }
        }

        buildFeatures {
            buildConfig = true
            aidl = true
            compose = true
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

        kotlin {
            jvmToolchain(21)

        }

        packaging {
            resources {
                excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
            }
            jniLibs {
                useLegacyPackaging = true
            }
            dex {
                useLegacyPackaging = true
            }
        }

        androidResources {
            generateLocaleConfig = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
        }
}

    dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.compose.material)
        implementation(libs.androidx.compose.material.icons.extended)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.foundation)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.core.splashscreen)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        implementation(libs.androidx.navigation.compose)
        implementation(libs.androidx.room.runtime)
        annotationProcessor(libs.androidx.room.compiler)
        ksp(libs.androidx.room.compiler)
        implementation(libs.libsu.core)
        implementation(libs.libsu.io)
        implementation(libs.libsu.nio)
        implementation(libs.libsu.service)
        implementation(libs.material)
        implementation(libs.okhttp)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.retrofit)
        implementation(libs.converter.gson)
        implementation(libs.miuix.blur)
    }