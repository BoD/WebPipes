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
  implementation(Ktor.server.statusPages)
  implementation(Ktor.server.callLogging)

  // Logging
  implementation("org.slf4j:slf4j-api:_")
  runtimeOnly("ch.qos.logback:logback-classic:_")
}

