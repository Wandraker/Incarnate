plugins {
    java
}

group = "dev.onelsey"
version = "0.9.0-dev.12"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.121-stable")
    compileOnly("io.netty:netty-transport:4.2.15.Final")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")

    testImplementation("io.papermc.paper:paper-api:26.2.build.121-stable")
    testImplementation("io.netty:netty-transport:4.2.15.Final")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}
