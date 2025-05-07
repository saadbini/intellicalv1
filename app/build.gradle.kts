import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // This applies Kotlin for Android and enables parcelize
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.parcelize)
    // No need to apply libs.plugins.kotlin.parcelize explicitly here
    // alias(libs.plugins.kotlin.serialization) // Apply only if using @Serializable AND it's not picked up
}

val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { fis ->
        keystoreProperties.load(fis)
    }
}

val localPropertiesFile: File = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { fis ->
        localProperties.load(fis)
    }
}

android {
    namespace = libs.versions.app.version.appId.get()
    compileSdk = libs.versions.app.build.compileSDKVersion.get().toInt()

    defaultConfig {
        applicationId = libs.versions.app.version.appId.get()
        minSdk = libs.versions.app.build.minimumSDK.get().toInt()
        targetSdk = libs.versions.app.build.targetSDK.get().toInt()
        versionCode = libs.versions.app.version.versionCode.get().toInt()
        versionName = libs.versions.app.version.versionName.get()

        multiDexEnabled = true
        vectorDrawables.useSupportLibrary = true

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        buildConfigField("String", "AZURE_API_KEY", "\"${localProperties.getProperty("azure.api.key", "")}\"")
        buildConfigField("String", "AZURE_ENDPOINT", "\"${localProperties.getProperty("azure.endpoint", "")}\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists() &&
            keystoreProperties.getProperty("keyAlias") != null &&
            keystoreProperties.getProperty("keyPassword") != null &&
            keystoreProperties.getProperty("storeFile") != null &&
            keystoreProperties.getProperty("storePassword") != null
        ) {
            register("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")!!
                keyPassword = keystoreProperties.getProperty("keyPassword")!!
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile")!!)
                storePassword = keystoreProperties.getProperty("storePassword")!!
            }
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true     // Enables Jetpack Compose
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() // CRITICAL
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("Warning: Release signing configuration not found. APK will be unsigned.")
            }
        }
    }

    flavorDimensions.add("variants")
    productFlavors {
        register("core")
    }

    compileOptions {
        val currentJavaVersionFromLibs = JavaVersion.valueOf(libs.versions.app.build.javaVersion.get().toString())
        sourceCompatibility = currentJavaVersionFromLibs
        targetCompatibility = currentJavaVersionFromLibs

        isCoreLibraryDesugaringEnabled = true
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = libs.versions.app.build.kotlinJVMTarget.get()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xcontext-receivers",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                "-opt-in=com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        jniLibs.pickFirsts.add("lib/x86/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/x86_64/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
        jniLibs.pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        resources.excludes.add("META-INF/LICENSE.md")
        resources.excludes.add("META-INF/LICENSE-notice.md")
    }
}

dependencies {
    // Core AndroidX & Utilities
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.print)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.biometric.ktx)

    // Lifecycle (using bundle)
    implementation(libs.bundles.androidx.lifecycle) // Corrected bundle name access

    // Room (using bundle)
    implementation(libs.bundles.room)  // CORRECTED: Now references the "room" bundle
    ksp(libs.androidx.room.compiler)

    // Material Components for Android (XML Views)
    implementation(libs.material)

    // Desugaring for Java 8+ APIs
    coreLibraryDesugaring(libs.desugar.jdkLibs)

    // Date/Time
    implementation(libs.threetenabp)
    api(libs.joda.time)

    // JSON
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // Image Loading
    api(libs.glide)
    ksp(libs.glide.ksp)
    implementation(libs.glide.compose)

    // Other Libraries
    api(libs.kotlin.immutable.collections)
    api(libs.ez.vcard)
    api(libs.androidx.recyclerview.fastscroller) // Corrected to use the alias from [libraries]
    api(libs.reprint)
    api(libs.androidx.rtl.viewpager) // Corrected to use the alias from [libraries]
    api(libs.patternlockview)


    // --- Jetpack Compose ---
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.compose.ui)
    debugImplementation(libs.bundles.compose.debug)
}
