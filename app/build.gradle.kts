plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.localagent.tcin"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Signing configurations for production releases
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")
      if (!keystorePath.isNullOrBlank()) {
        val keystoreFile = file(keystorePath)
        if (keystoreFile.exists()) {
          storeFile = keystoreFile
          storePassword = System.getenv("STORE_PASSWORD") ?: ""
          keyAlias = System.getenv("KEY_ALIAS") ?: ""
          keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      // Enable R8 code and resource shrinking for release
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val keystorePath = System.getenv("KEYSTORE_PATH")
      if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
        signingConfig = signingConfigs.getByName("release")
      }
    }
    debug {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("debug")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  packaging {
    resources {
      excludes.addAll(listOf(
        "META-INF/AL2.0",
        "META-INF/LGPL2.1",
        "META-INF/licenses/**",
        "META-INF/DEPENDENCIES",
        "META-INF/*.version"
      ))
    }
    jniLibs {
      useLegacyPackaging = false
      excludes.addAll(listOf(
        "lib/x86/**",
        "lib/x86_64/**",
        "lib/mips/**",
        "lib/mips64/**"
      ))
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }
}

kotlin {
  jvmToolchain(17)
}

// Configure the Secrets Gradle Plugin to avoid baking credentials into APK BuildConfig
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("GEMINI_API_KEY") // Protect API keys: use Android Keystore or user entry at runtime
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  
  // Room Local SQLite Persistence (Durable Memory & Learning)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  "ksp"(libs.androidx.room.compiler)

  // JSON serialization & parsing
  implementation(libs.converter.moshi)
  implementation(libs.moshi.kotlin)
  "ksp"(libs.moshi.kotlin.codegen)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // HTTP Networking for autonomous web research
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)

  // Unit and JVM Testing
  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.compose.ui.test.junit4)

  // Debug tooling
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
