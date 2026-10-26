plugins {
    kotlin("jvm") version "2.1.10"
    
    application
    id("com.gradleup.shadow") version "8.3.5"
}

application{
    mainClass = "com.botofan.MainKt"   // <-- your fully-qualified main class
}

group = "com.botofan"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    
    val pi4jVersion = "4.0.0"
    val slf4jVersion = "2.0.16"
    val kotlinCoroutinesVersion = "1.10.1"
    implementation("com.pi4j:pi4j-core:$pi4jVersion")
    implementation("com.pi4j:pi4j-plugin-raspberrypi:$pi4jVersion")
    implementation("com.pi4j:pi4j-plugin-ffm:$pi4jVersion")
    implementation("com.fazecast:jSerialComm:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("org.slf4j:slf4j-simple:$slf4jVersion")
}

tasks.test {
    useJUnitPlatform()
}