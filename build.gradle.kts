plugins {
    kotlin("jvm") version "1.9.23"
//    application
}

group = "org.bongani"
version = "1.0-SNAPSHOT"
val vertxVersion = "4.3.8"


repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.77.Final") {
        artifact {
            classifier = "osx-aarch_64" // or "osx-x86_64" for Intel Macs
        }
    }


    // Vert.x
    implementation("io.vertx:vertx-web:$vertxVersion")
    implementation("io.vertx:vertx-web-client:$vertxVersion")
    implementation("io.vertx:vertx-junit5:$vertxVersion")


    // Kotlin reflection (fixes typeOf)
    implementation("org.jetbrains.kotlin:kotlin-reflect")


    // Netty native transport (fixes DNS warning)
    implementation("io.netty:netty-transport-native-epoll:4.1.86.Final")
    implementation("io.netty:netty-resolver-dns-native-macos:4.1.86.Final:osx-aarch_64")


    // For JSON serialization
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.vertx:vertx-junit5:4.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")


    // For assertThrows
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(20)
}