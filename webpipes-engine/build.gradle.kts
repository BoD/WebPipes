plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Coroutines
  implementation(KotlinX.coroutines.core)

  // Logging
  implementation("org.slf4j:slf4j-api:_")

  // OKHttp
  implementation(Square.okHttp3)

  // XSoup
  implementation("us.codecraft:xsoup:_")

  // WebPipes API
  api(project(":webpipes-api"))
}
