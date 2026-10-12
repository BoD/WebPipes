plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(25)
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

  // Playwright
  implementation(libs.playwright)

  // OpenTelemetry
  implementation(libs.opentelemetry.kotlin.api)
  implementation(libs.opentelemetry.kotlin.noop)

  // WebPipes API
  api(project(":webpipes-api"))
}
