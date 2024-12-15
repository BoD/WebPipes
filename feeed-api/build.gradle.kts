plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Okio
  api(Square.okio)

  // Json
  api(KotlinX.serialization.json)
}
