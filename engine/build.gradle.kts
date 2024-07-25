plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Coroutines
  implementation(KotlinX.coroutines.core)

  // Okio
  api(Square.okio)

  // Logging
  implementation("org.slf4j:slf4j-api:_")
  runtimeOnly("ch.qos.logback:logback-classic:_")

  // OKHttp
  implementation(Square.okHttp3)

  // XSoup
  implementation("us.codecraft:xsoup:_")

  // Feeed API
  api(project(":feeed-api"))
}
