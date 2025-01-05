plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Ktor
  implementation(Ktor.server.core)
  implementation(Ktor.server.netty)
  implementation(Ktor.server.defaultHeaders)
  implementation(Ktor.server.contentNegotiation)
  implementation(Ktor.server.statusPages)
  implementation(Ktor.server.callLogging)
  implementation(Ktor.plugins.serialization.kotlinx.json)

  // Json
  implementation(KotlinX.serialization.json)

  // Logging
  implementation("org.slf4j:slf4j-api:_")
}
