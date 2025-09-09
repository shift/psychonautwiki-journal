import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm("desktop")
    
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            // Coroutines
            implementation(libs.kotlinx.coroutines.core)
            
            // Serialization
            implementation(libs.kotlinx.serialization.json)
            
            // Navigation - handled manually for Compose Multiplatform
            // implementation(libs.navigation.compose)
            
            // Dependency Injection
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            
            // Database
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)
            
            // DateTime
            implementation(libs.kotlinx.datetime)
        }
        
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.koin.core)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.mockk)
        }
        
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
                implementation(libs.cucumber.java8)
                implementation(libs.cucumber.junit.platform.engine)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.isaakhanimann.journal.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "PsychonautWiki Journal"
            packageVersion = "1.0.0"
            description = "A safer way to track substance experiences"
            
            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                packageName = "psychonautwiki-journal"
                debMaintainer = "opensource@psychonautwiki.org"
                menuGroup = "Utilities"
                appCategory = "Utility"
            }
        }
        
        buildTypes.release.proguard {
            configurationFiles.from("proguard-rules.pro")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("com.isaakhanimann.journal.database")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
        }
    }
}