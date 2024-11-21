plugins {
    alias(libs.plugins.newquiz.android.library)
    alias(libs.plugins.newquiz.android.hilt)
    alias(libs.plugins.newquiz.kotlin.serialization)
    alias(libs.plugins.newquiz.detekt)
}

android {
    namespace = "com.infinitepower.newquiz.core.user_services"
}

dependencies {
    implementation(projects.model)
    implementation(projects.core)
    implementation(projects.core.analytics)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(projects.core.remoteConfig)
    androidTestImplementation(projects.core.testing)

    implementation(libs.hilt.ext.work)
    implementation(libs.androidx.work.ktx)
    ksp(libs.hilt.ext.compiler)
    androidTestImplementation(libs.androidx.work.testing)

    implementation(libs.kotlinx.datetime)

    testImplementation(projects.core.testing)
}
