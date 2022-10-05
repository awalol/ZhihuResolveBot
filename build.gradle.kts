import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "cn.awalol"
version = "1.2.3"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.inmo:tgbotapi:3.2.7")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.15")
    implementation("org.slf4j:slf4j-simple:2.0.3")
//    implementation("cn.hutool:hutool-all:5.8.5")
    implementation("org.jsoup:jsoup:1.15.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}
