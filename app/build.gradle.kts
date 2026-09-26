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
    // 公开 SDK 仓库里 API 37 的平台包名是 platforms;android-37.0（没有 android-37），
    // 因此需要显式声明 minor 版本；否则 AGP 会报 "Failed to find target with hash string 'android-37'"
    compileSdkMinor = 0
    namespace = "safe.kernel.flash"

    defaultConfig {
        applicationId = "safe.kernel.flash"
        minSdk = 29
        targetSdk = 36
        versionCode = 20800
        versionName = "2.8"

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
                // libnsexec.so 是预编译好的 setns 助手（可执行文件伪装成 .so），
                // 不要被 strip 任务处理
                keepDebugSymbols += "**/libnsexec.so"
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