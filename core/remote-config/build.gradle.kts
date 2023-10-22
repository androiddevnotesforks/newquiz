plugins {
    id("newquiz.android.library")
    alias(libs.plugins.newquiz.android.hilt)
}

android {
    namespace = "com.infinitepower.newquiz.core.remote_config"
}

dependencies {
    implementation(libs.androidx.startup.runtime)

    normalImplementation(platform(libs.firebase.bom))
    normalImplementation(libs.firebase.remoteConfig)

    normalImplementation(projects.core)
}
