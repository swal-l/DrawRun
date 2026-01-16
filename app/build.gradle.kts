import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.orbital.run"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.orbital.run"
        minSdk = 26
        targetSdk = 35
        versionCode = 108
        versionName = "5.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        
        val sId = localProperties.getProperty("strava.client_id") ?: ""
        val sSecret = localProperties.getProperty("strava.client_secret") ?: ""
        
        buildConfigField("String", "STRAVA_CLIENT_ID", "\"$sId\"")
        buildConfigField("String", "STRAVA_CLIENT_SECRET", "\"$sSecret\"")

        val fitbitId = localProperties.getProperty("fitbit.client_id") ?: ""
        buildConfigField("String", "FITBIT_CLIENT_ID", "\"$fitbitId\"")
        
        val withingsId = localProperties.getProperty("withings.client_id") ?: ""
        buildConfigField("String", "WITHINGS_CLIENT_ID", "\"$withingsId\"")
        
        val garminUrl = localProperties.getProperty("garmin.api_url") ?: "https://drawrunvercel-d2nnvlyct-lomics-projects.vercel.app/api/garmin_sync"
        buildConfigField("String", "GARMIN_API_URL", "\"$garminUrl\"")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties().apply {
                    load(FileInputStream(keystorePropertiesFile))
                }
                
                keyAlias = properties.getProperty("keyAlias") ?: ""
                keyPassword = properties.getProperty("keyPassword") ?: ""
                storeFile = properties.getProperty("storeFile")?.let { file(it) }
                storePassword = properties.getProperty("storePassword") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Apply release signing to debug to allow Health Connect to work without manual signed builds for every test
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)?.outputFileName = "DrawRun_v${defaultConfig.versionName}.apk"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Charts (Vico)
    implementation("com.patrykandpatrick.vico:compose:1.12.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.12.0")
    implementation("com.patrykandpatrick.vico:core:1.12.0")
    
    // Network & JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("org.json:json:20231013")
    
    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha11")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    
    
    // 3D Maps (Open Source)
    implementation("org.maplibre.gl:android-sdk:11.0.0")
    
    // Splash Screen (Android 12+)
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Material3
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    
    // Lifecycle Compose (collectAsStateWithLifecycle)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Room (Local Database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    
    // Kotlinx Serialization (for JSON in Room)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

// Satisfy IDE expectation for testClasses task
tasks.register("testClasses") {
    dependsOn("compileDebugUnitTestSources")
}














































































































