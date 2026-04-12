plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Coroutines
  implementation(libs.kotlinx.coroutines.core)

  // Logging
  implementation(libs.slf4j.api)

  // OKHttp
  implementation(libs.okhttp)

  // XSoup
  implementation(libs.xsoup)

  // WebPipes API
  api(project(":webpipes-api"))
}
