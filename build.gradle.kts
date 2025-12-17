// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    alias(libs.plugins.ksp) apply false

}
allprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            // "org.jetbrains.kotlin" 그룹의 모든 라이브러리는
            if (requested.group == "org.jetbrains.kotlin") {
                // 무조건 2.0.21 버전을 사용해라! (2.2.21 절대 금지)
                useVersion("2.0.21")
            }
        }
    }
}