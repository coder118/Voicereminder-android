
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)

}
//configurations.all {
//    resolutionStrategy {
//        force("com.google.protobuf:protobuf-javalite:3.23.4")
//        // 모든 구성에서 protobuf-java 제외
//        exclude(group = "com.google.protobuf", module = "protobuf-java")
//    }
//}
// 1. resolutionStrategy 수정 (전체 protobuf-java 제외 제거)
//configurations.all {
//    resolutionStrategy {
//        // 버전 일관성만 강제 (force 제거)
//        eachDependency {
//            if (requested.group == "com.google.protobuf") {
//                useVersion("3.23.4")
//            }
//        }
//    }
//}

//configurations.all {
//    resolutionStrategy {
//        // 1. GRPC 버전 강제 (TOML 버전 사용)
//        force(libs.grpc.okhttp.get())
//
//
//        dependencySubstitution {
//            substitute(module("com.google.protobuf:protobuf-java"))
//                .using(module("com.google.protobuf:protobuf-javalite:${libs.versions.protobuf.get()}"))
//                .because("Android requires Lite runtime")
//        }
//    }
//}

android {
    namespace = "com.example.voicereminder"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.voicereminder"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources{
            excludes.add("META-INF/*")
        }
    }
//    packaging {
//        resources {
//            // 필수 제외 항목 (META-INF 충돌 방지)
////            excludes.add("META-INF/INDEX.LIST")
////            excludes.add("META-INF/DEPENDENCIES")
////            excludes.add("META-INF/LICENSE*")
////            excludes.add("META-INF/LGPL2.1")
////            excludes.add("META-INF/*.kotlin_module")
////
////            // gRPC/OkHttp 사용 시 추가
////            excludes.add("META-INF/services/**")
//            // 서비스 관련 중복 파일 제외
//            excludes.add("META-INF/services/**")
//
//            // 그 외 중복으로 문제가 될 수 있는 파일들 추가
//            excludes += setOf(
//                "META-INF/INDEX.LIST",
//                "META-INF/DEPENDENCIES",
//                "META-INF/LICENSE",
//                "META-INF/LICENSE.txt",
//                "META-INF/LICENSE.md",
//                "META-INF/LICENSE-notice",
//                "META-INF/AL2.0",
//                "META-INF/LGPL2.1",
//                "META-INF/*.kotlin_module",
//                "**/okhttp3/**"
//            )
//        }
//    }
}

dependencies {


    implementation(libs.firebase.messaging)

    implementation(libs.google.auth.library)
    implementation(libs.google.cloud.texttospeech)
    implementation(libs.grpc.okhttp)

    implementation(libs.okhttp)
    implementation(libs.json)

    // Media3 라이브러리 추가
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui) // UI 관련 라이브러리 (선택 사항)

    implementation(libs.okio)
//    // grpc-okhttp 및 내부 grpc 라이브러리들은 모두 1.70.0으로 통일
//    implementation(libs.grpc.okhttp) {
//        exclude(group = "com.google.protobuf")
//    }
//    implementation(libs.google.auth.library)
//    implementation("com.google.api:gax-grpc:2.38.0")
//
//    // Protobuf 버전 통일 (Firebase와 호환)
//    implementation("com.google.protobuf:protobuf-javalite:3.23.4")
////    // 2. Firebase용으로 Java 버전 선택적 추가
//    implementation("com.google.protobuf:protobuf-java:3.23.4") {
//        isTransitive = false // 다른 모듈에 영향 X
//    }
////    {
////        exclude(group = "com.google.protobuf", module = "protobuf-java")
////    }
//
//    // Google Cloud Text-to-Speech가장 최신
////    implementation(libs.google.cloud.texttospeech){
////        exclude (group = "com.google.protobuf", module= "protobuf-java")
////    }
//
//    // 2. TTS에는 javalite 전용 버전 사용 (protobuf-java 제외)
//    implementation("com.google.cloud:google-cloud-texttospeech:2.32.0") {
//        exclude(group = "com.google.protobuf", module = "protobuf-java")
//    }
//
//    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
//    // Firebase Messaging (protobuf 그룹 제외)
//    implementation(libs.firebase.messaging) {
//        exclude(group = "com.google.protobuf")
//    }
    ////////
//    implementation(libs.grpc.okhttp)
//    implementation(libs.google.auth.library)
//
//    implementation("com.google.api:gax-grpc:2.38.0")
//
//    // Protobuf 버전 통일 (Firebase와 호환)
//    implementation("com.google.protobuf:protobuf-javalite:3.23.4")
//
//    // Google Cloud TTS
//    implementation(libs.google.cloud.texttospeech)
    // 필수 gRPC 라이브러리 추가
//    implementation(libs.grpc.core)
//    implementation(libs.grpc.api)
//    implementation(libs.grpc.protobuf)
//    implementation(libs.grpc.stub)

    // Protobuf Java Lite (Android 필수)
//    implementation("com.google.protobuf:protobuf-javalite:3.22.0")
//    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
//    implementation(libs.firebase.messaging){
//        exclude(group = "com.google.protobuf")
//    }
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.eventbus)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation (libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}