plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tokidokiroppou.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "tokidokiroppou.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("compose") {
            id = "tokidokiroppou.compose"
            implementationClass = "ComposeLibraryConventionPlugin"
        }
        register("hilt") {
            id = "tokidokiroppou.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "tokidokiroppou.kotlin.serialization"
            implementationClass = "KotlinSerializationConventionPlugin"
        }
    }
}

dependencies {
    compileOnly(libs.plugins.android.application.toDep())
    compileOnly(libs.plugins.android.library.toDep())
    compileOnly(libs.plugins.kotlin.android.toDep())
    compileOnly(libs.plugins.kotlin.compose.toDep())
    compileOnly(libs.plugins.kotlin.serialization.toDep())
    compileOnly(libs.plugins.ksp.toDep())
    compileOnly(libs.plugins.hilt.toDep())
}

fun Provider<PluginDependency>.toDep() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
