plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Ktor
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.defaultHeaders)
  implementation(libs.ktor.server.contentNegotiation)
  implementation(libs.ktor.server.statusPages)
  implementation(libs.ktor.server.callLogging)
  implementation(libs.ktor.serialization.kotlinxJson)

  // Json
  implementation(KotlinX.serialization.json)

  // Logging
  implementation(libs.slf4j.api)
}
