
plugins {
    kotlin("jvm")
    application
}

group = "io.github.hmiyado"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("11"))
    }
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

dependencies {
    val ktorVersion: String by project
    implementation(rootProject)
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
}
