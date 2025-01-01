plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Logging
  implementation("org.slf4j:slf4j-api:_")

  // WebPipes API
  implementation(project(":webpipes-engine"))

  // RSS / Atom
  implementation("com.rometools:rome:_")
}
