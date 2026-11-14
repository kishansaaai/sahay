// Pure-Kotlin JVM module: no Android or third-party dependencies, so it also runs on a laptop and in CI.
// Include from the root build with: include(":engine")   (Stream B owns the root Gradle setup, issue B01).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories { mavenCentral() }

sourceSets {
    main { kotlin.srcDir("src/main/kotlin") }
    create("parity") { kotlin.srcDir("src/test/kotlin"); compileClasspath += sourceSets.main.get().output; runtimeClasspath += sourceSets.main.get().output }
}

// Dependency-free test runner (port of engine/tests/test_engine.py). Needs the bundled content files.
tasks.register<JavaExec>("engineTests") {
    group = "verification"
    classpath = sourceSets["parity"].runtimeClasspath
    mainClass.set("com.sahay.engine.EngineTestsKt")
    args(rootProject.file("content").path.let { if (file("../content").exists()) file("../content").path else it })
}
tasks.named("check") { dependsOn("engineTests") }
