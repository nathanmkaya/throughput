plugins {
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.throughput.server.ApplicationKt")
}

dependencies {
    implementation(project(":throughput-common"))
    
    // Ktor Server
    implementation(libs.bundles.ktor.server)
    
    // Koin for dependency injection
    implementation(libs.bundles.koin)
    
    // Logging
    implementation(libs.logback.classic)
    
    // Testing
    //testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}
