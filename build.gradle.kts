
plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    `maven-publish`
    signing
}

group = "io.github.hmiyado"
version = "0.2-SNAPSHOT"

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
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn.add(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ktor-csrf-protection"

            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name.set("Ktor Csrf Protection")
                description.set("Protect Csrf in Ktor server")
                url.set("https://github.com/hmiyado/ktor-csrf-protection")
                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("https://github.com/hmiyado/ktor-csrf-protection/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("hmiyado")
                        name.set("hmiyado")
                    }
                }
                scm {
                    url.set("https://github.com/hmiyado/ktor-csrf-protection.git")
                }
            }
            repositories {
                maven {
                    val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials {
                        val ossrhUsername: String? by project
                        val ossrhPassword: String? by project
                        username = ossrhUsername
                        password = ossrhPassword
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
