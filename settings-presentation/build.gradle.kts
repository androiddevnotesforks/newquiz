plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp") version "1.6.21-1.0.5"
}

android {
    namespace = "com.infinitepower.newquiz.settings_presentation"
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        minSdk = ProjectConfig.minSdk
        targetSdk = ProjectConfig.targetSdk

        testInstrumentationRunner = ProjectConfig.testInstrumentationRunner
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Compose.composeCompilerVersion
    }
    libraryVariants.all {
        kotlin.sourceSets {
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
    lint {
        disable += "DialogFragmentCallbacksDetector"
    }
}

dependencies {
    implementation(AndroidX.core.ktx)
    implementation(AndroidX.lifecycle.runtimeKtx)

    testImplementation(Testing.junit.jupiter)
    testImplementation("com.google.truth:truth:_")
    androidTestImplementation(Kotlin.test.junit)

    implementation(Google.android.material)

    debugImplementation(AndroidX.compose.ui.testManifest)
    implementation(AndroidX.compose.ui.tooling)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.activity.compose)
    implementation(AndroidX.compose.material3)
    implementation(AndroidX.compose.material.icons.extended)

    implementation(Google.dagger.hilt.android)
    kapt(Google.dagger.hilt.compiler)
    kapt(AndroidX.hilt.compiler)
    implementation(AndroidX.hilt.navigationCompose)
    androidTestImplementation(Google.dagger.hilt.android.testing)
    kaptAndroidTest(Google.dagger.hilt.compiler)

    implementation(AndroidX.dataStore.preferences)

    implementation("io.github.raamcosta.compose-destinations:core:_")
    ksp("io.github.raamcosta.compose-destinations:ksp:_")

    implementation(project(Modules.core))
    implementation(project(Modules.data))
    implementation(project(Modules.domain))
    implementation(project(Modules.translationDynamicFeature))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ksp {
    arg("compose-destinations.mode", "destinations")
    arg("compose-destinations.moduleName", "settings")
}