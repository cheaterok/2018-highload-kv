// See https://gradle.org and https://github.com/gradle/kotlin-dsl

// Apply the java plugin to add support for Java
plugins {
    java
    kotlin("jvm") version "1.3.11"
    application
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    // Annotations for better code documentation
    compile("com.intellij:annotations:12.0")

    compile("org.slf4j:slf4j-simple:1.7.25")

    // Kotlin stdlib
    compile(kotlin("stdlib"))
    // For khttp
    compile("org.jetbrains.kotlin:kotlin-reflect:1.3.11")

    // HTTP server
    compile("com.sparkjava:spark-kotlin:1.0.0-alpha")

    // HTTP client
    compile("com.github.jkcclemens:khttp:-SNAPSHOT")

    // Key-value storage - RocksDB
    // https://github.com/iotaledger/iri/issues/350#issuecomment-386713966
    compile("org.rocksdb:rocksdbjni:5.3.6")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")

    // Guava for tests
    testCompile("com.google.guava:guava:23.1-jre")

    // Our beloved one-nio
    testCompile("ru.odnoklassniki:one-nio:1.0.2")

}

tasks {
    "test"(Test::class) {
        maxHeapSize = "128m"
        useJUnitPlatform()
    }
}

application {
    // Define the main class for the application
    mainClassName = "ru.mail.polis.Cluster"

    // And limit Xmx
    applicationDefaultJvmArgs = listOf("-Xmx128m")
}
