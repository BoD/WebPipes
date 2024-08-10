plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Logging
  implementation("org.slf4j:slf4j-api:_")
  runtimeOnly("ch.qos.logback:logback-classic:_")

  // Feeed API
  implementation(project(":engine"))

  // RSS / Atom
  implementation("com.rometools:rome:_")
}
