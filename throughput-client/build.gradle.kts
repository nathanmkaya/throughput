dependencies {
    implementation(project(":throughput-common"))
    
    // Ktor Client
    implementation(libs.bundles.ktor.client)
    
    // Logging
    implementation(libs.logback.classic)
    
    // Testing
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.client.mock)
}
