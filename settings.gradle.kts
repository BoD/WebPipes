rootProject.name = "feeed"

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
  ":feeed-api",
  ":engine",
  ":atom",
  ":server",
  ":main",
)
