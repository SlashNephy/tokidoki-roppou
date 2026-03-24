plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.hilt")
    id("tokidokiroppou.compose")
}

android {
    namespace = "blue.starry.tokidokiroppou.core.ai"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.timber)
}
