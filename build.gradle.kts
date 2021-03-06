import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.collections.*

plugins {
    kotlin("jvm") version "1.3.0"
    application
    `maven-publish`
    maven
    id("com.palantir.docker-run") version "0.20.1"

}


application{
    mainClassName = "App"
}

group = "nl.dorost.flow"
version = "1.0-SNAPSHOT"
val ktor_version = "1.0.0-beta-3"
kotlin.experimental.coroutines = Coroutines.ENABLE

repositories {
    mavenCentral()
    jcenter()
    mavenLocal()
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
}

dependencies {

    compile(kotlin("stdlib-jdk8"))

    compile("com.natpryce:konfig:1.6.10.0")
    compile("com.google.cloud:google-cloud-datastore:1.54.0")
    compile("com.google.cloud:google-cloud-storage:1.54.0")


    compile("com.github.kittinunf.fuel:fuel:1.15.0")
    compile("com.moandjiezana.toml:toml4j:0.7.2")
    compile("io.ktor:ktor-server-netty:$ktor_version")
    compile("io.ktor:ktor-websockets:$ktor_version")
    compile("io.ktor:ktor-auth:$ktor_version")
    compile("io.ktor:ktor-client-apache:$ktor_version")

    compile("org.apache.commons:commons-text:1.6")
    compile("io.github.microutils:kotlin-logging:1.6.20")
    compile("org.slf4j:slf4j-api:1.8.0-beta2")
    compile("org.slf4j:slf4j-simple:1.8.0-beta2")


    compile("com.fasterxml.jackson.core:jackson-databind:2.7.1-1")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.7.1-2")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.7.1")


    compile("com.beust:klaxon:3.0.1")

    testCompile("org.junit.jupiter:junit-jupiter-api:5.1.0")
    testCompile("org.awaitility:awaitility:3.1.3")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

