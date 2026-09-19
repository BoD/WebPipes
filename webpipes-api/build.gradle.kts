plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(25)
}

dependencies {
  // Json
  api(libs.kotlinx.serialization.json)
}
