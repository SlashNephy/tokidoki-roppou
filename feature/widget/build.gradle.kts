plugins {
    id("tokidokiroppou.android.library")
    id("tokidokiroppou.compose")
    id("tokidokiroppou.hilt")
}

android {
    namespace = "blue.starry.tokidokiroppou.feature.widget"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.timber)
}
