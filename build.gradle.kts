plugins {
    kotlin("jvm") version "1.8.10" apply false
    kotlin("plugin.serialization") version "1.8.10" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

//
//group = "su.spy_me"
//version = "1.0-SNAPSHOT"
//

//
//kotlin {
//    jvm()
//
//    sourceSets {
//        val jvmMain by getting {
//            dependencies {

//

//            }
//        }
//    }
//}
//
//tasks.test {
//    useJUnitPlatform()
//}
//
//tasks.withType<KotlinCompile> {
//    kotlinOptions.jvmTarget = "1.8"
//}
//
//application {
//    mainClass.set("MainKt")
//}