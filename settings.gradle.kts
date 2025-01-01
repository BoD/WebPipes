rootProject.name = "WebPipes"

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

plugins {
  // See https://splitties.github.io/refreshVersions/
  id("de.fayard.refreshVersions") version "0.60.5"
}

include(
  ":webpipes-api",
  ":webpipes-engine",
  ":webpipes-atom",
  ":webpipes-server",
  ":main",
)
