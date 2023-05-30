plugins {
    kotlin("jvm") version "1.8.20"
    id("org.jetbrains.compose") version "1.4.0"
    id("dev.hydraulic.conveyor") version "1.4"
}

group = "br.com.selkeys"
version = "1.2.2"
java.toolchain.languageVersion.set(JavaLanguageVersion.of(19))
compose.desktop.application.mainClass = "MainKt"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    windowsAmd64(compose.desktop.windows_x64)
    linuxAmd64(compose.desktop.linux_x64)

    implementation("com.azure:azure-cosmos:4.45.1")
    implementation("com.azure:azure-identity:1.9.0")

    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")

    implementation("com.h2database:h2:2.1.214")

    implementation("org.apache.pdfbox:pdfbox:2.0.28")

}

tasks.processResources {
    inputs.properties(System.getenv())
    inputs.properties(Pair("version", version))
    expand(inputs.properties)
}

configurations.all {
    attributes {
        // https://github.com/JetBrains/compose-jb/issues/1404#issuecomment-1146894731
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}




