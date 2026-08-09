plugins { alias(libs.plugins.android.library) }

android {
    namespace = "com.englishcoach60.speech"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly(files("libs/sherpa-onnx-static-link-onnxruntime-1.13.4.aar"))
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
