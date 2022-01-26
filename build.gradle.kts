
plugins {
    kotlin("jvm") version "1.6.10"
}

group = "com.github.hmiyado"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

dependencies {
    val ktorVersion = "1.6.7"
    implementation("ch.qos.logback:logback-classic:1.2.1")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    val kotestVersion = "4.6.3"
    val ktorAssertionVersion = "1.0.2"
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.mockk:mockk:1.12.1")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:$ktorAssertionVersion")

}
