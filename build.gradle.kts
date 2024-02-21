plugins {
    java

    id("idea")
    id("org.owasp.dependencycheck") version "8.3.1"
}

group = "fi.solita.clamav"
version = "1.0.3"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")

    implementation(platform("org.testcontainers:testcontainers-bom:1.19.2"))
    testImplementation("org.testcontainers:junit-jupiter:1.19.2")
    testImplementation("net.java.dev.jna:jna:5.7.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
