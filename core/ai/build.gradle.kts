plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.hilt")
}

android {
    namespace = "blue.starry.tokidokiroppou.core.ai"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.timber)
}
