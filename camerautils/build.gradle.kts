import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.library)
    // FIXED: Removed 'apply false' so the compose compiler actually processes this library module
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "app.misartech.camerautils"
    // FIXED: Set to standard integer assignments (using standard target sdk versions e.g., 34)
    compileSdk = 37

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    // FIXED: Configured Java 17 toolchains required for CameraX 1.4.0 and modern Compose stability
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }


    buildFeatures {
        compose = true
    }

    // FIXED: Completely removed the deprecated composeOptions block

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.misartech.sdk"
            artifactId = "camerautils"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Misar Tech Camera Library")
                description.set("A high-performance CameraX utility SDK package equipped with customizable Jetpack Compose layouts.")
                url.set("https://github.com/misartechstudio/camera-sdk.git")
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "LocalSandbox"
            url = uri(layout.buildDirectory.dir("outputs/maven-repo"))
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Core Android Framework
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Jetpack Compose UI Layers
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.compose.ui:ui:1.6.1")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.1")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))

    // CameraX Core Engine & Functional Extensions
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-video:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Asynchronous Flow Streams
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}