// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
//// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        // Android Gradle Plugin와 Kotlin Gradle Plugin, Google Services 플러그인의 클래스패스 지정
//        classpath("com.android.tools.build:gradle:8.9.0")
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.21")
//        classpath("com.google.gms:google-services:4.4.2")
//    }
//}
//
//plugins {
//    // Version Catalog을 사용한 플러그인 선언 (apply false로 각 모듈에서 개별 적용)
//    alias(libs.plugins.android.application) apply false
//    alias(libs.plugins.kotlin.android) apply false
//    alias(libs.plugins.kotlin.compose) apply false
//    alias(libs.plugins.google.gms.google.services) apply false
//}
//
//allprojects {
//    repositories {
//        google()
//        mavenCentral()
//    }
//
//    // 모든 모듈에 적용: com.google.protobuf:protobuf-java 의존성을
//    // protobuf-javalite:3.23.4 로 강제 치환
////    configurations.all {
////        resolutionStrategy.eachDependency { details ->
////            if (details.requested.group == "com.google.protobuf" &&
////                details.requested.name == "protobuf-java") {
////                details.useTarget group: "com.google.protobuf", name: "protobuf-javalite", version: "3.23.4"
////            }
////        }
////    }
////}
////
////// 클린 빌드를 위한 태스크 정의
////task clean(type: Delete) {
////    delete rootProject.buildDir
////}
//    // 모든 모듈에 적용: com.google.protobuf:protobuf-java 대신 protobuf-javalite 사용
//    configurations.all {
//        resolutionStrategy.eachDependency { details: org.gradle.api.artifacts.DependencyResolveDetails ->
//            if (details.requested.group == "com.google.protobuf" &&
//                details.requested.name == "protobuf-java") {
//                details.useTarget("com.google.protobuf:protobuf-javalite:3.23.4")
//            }
//        }
//    }
//}
//
//// 클린 빌드 태스크 정의 (Kotlin DSL 방식)
//tasks.register<Delete>("clean") {
//    delete(rootProject.buildDir)
//}
