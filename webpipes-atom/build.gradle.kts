plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(25)
}

dependencies {
  // Logging
  implementation(libs.slf4j.api)

  // WebPipes API
  implementation(project(":webpipes-engine"))

  // RSS / Atom
  implementation(libs.rome)
}
