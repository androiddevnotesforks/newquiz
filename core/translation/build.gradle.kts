plugins {
    alias(libs.plugins.newquiz.android.library)
    alias(libs.plugins.newquiz.android.hilt)
    alias(libs.plugins.newquiz.detekt)
}

android {
    namespace = "com.infinitepower.newquiz.core.translation"
}

dependencies {
    implementation(projects.model)
    implementation(projects.core)
    implementation(projects.core.datastore)

    normalImplementation(libs.google.mlKit.translate)
    normalImplementation(libs.kotlinx.coroutines.playServices)
}
