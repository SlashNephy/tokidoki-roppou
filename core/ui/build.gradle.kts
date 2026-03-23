plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.compose")
}

android {
    namespace = "blue.starry.tokidokiroppou.core.ui"
}

dependencies {
    implementation(project(":core:domain"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.markdown.renderer)
    debugImplementation(libs.compose.ui.tooling)
}
