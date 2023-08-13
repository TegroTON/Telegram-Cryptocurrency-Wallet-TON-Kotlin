plugins {
    kotlin("jvm") version "1.9.0" apply false
    kotlin("plugin.serialization") version "1.9.0" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/service/local/repositories/snapshots/content/")
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
