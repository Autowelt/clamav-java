plugins {
    id("java")
    id("idea")
    id("org.owasp.dependencycheck") version "8.3.1"
    id("maven-publish")
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
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Autowelt/clamav-client")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register("jar", MavenPublication::class) {
            from(components["java"])
            group = group
            artifactId = "clamav-java"
            version = version
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
