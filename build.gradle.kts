import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "cn.awalol"
version = "1.2.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.inmo:tgbotapi:3.1.1")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.11")
    implementation("org.slf4j:slf4j-simple:1.7.36")
//    implementation("cn.hutool:hutool-all:5.8.5")
    implementation("org.jsoup:jsoup:1.15.2")
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
