plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

sourceSets.main.get().java.srcDir("src/main")
sourceSets.main.get().resources.srcDir("src/resources")

application {
    mainClassName = "sc.player2022.Starter"
}

repositories {
    jcenter()
    maven("https://dist.wso2.org/maven2")
    maven("https://jitpack.io")
}

dependencies {
    if(gradle.startParameter.isOffline) {
        implementation(fileTree("lib"))
    } else {
        implementation("com.github.software-challenge.backend", "ostseeschach_2022", "22.1.0")
    }
}

tasks.shadowJar {
    archiveBaseName.set("ostseeschach_2022_client")
    archiveClassifier.set("")
    destinationDirectory.set(rootDir)
}
