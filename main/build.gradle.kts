import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import com.bmuschko.gradle.docker.tasks.image.Dockerfile.CopyFileInstruction

plugins {
  alias(libs.plugins.kotlin.jvm)
  id("application")
  alias(libs.plugins.dockerJavaApplication)
}

kotlin {
  jvmToolchain(11)
}

application {
  mainClass.set("org.jraf.webpipes.main.MainKt")
}

dependencies {
  // Logging
  implementation(libs.slf4j.api)
  implementation(libs.slf4j.simple)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)

  // WebPipes
  implementation(project(":webpipes-server"))
  implementation(project(":webpipes-engine"))
  implementation(project(":webpipes-atom"))

  // OKHttp
  implementation(Square.okHttp3)

  // Dropbox
  implementation(libs.dropbox.core.sdk)

  // OpenAI
  implementation(libs.openai)

  // JGit
  implementation(libs.jgit)
  implementation(libs.jgit.ssh)

  // Playwright
  implementation(libs.playwright)
}

docker {
  javaApplication {
    // Use OpenJ9 instead of the default one
    baseImage.set("adoptopenjdk/openjdk11-openj9:x86_64-ubuntu-jre-11.0.30_7.1_openj9-0.57.0")
    maintainer.set("BoD <BoD@JRAF.org>")
    ports.set(listOf(8042))
    images.add("bodlulu/${rootProject.name.lowercase()}:latest")
    jvmArgs.set(listOf("-Xms16m", "-Xmx128m"))
  }
  registryCredentials {
    username.set(System.getenv("DOCKER_USERNAME"))
    password.set(System.getenv("DOCKER_PASSWORD"))
  }
}

tasks.withType<DockerBuildImage> {
  platform.set("linux/amd64")
}

tasks.withType<Dockerfile> {
  environmentVariable("PLAYWRIGHT_BROWSERS_PATH", "/playwright-browsers")

  // Install browser dependencies
  runCommand("apt-get update")
  runCommand(
    """
    apt-get install -y \
      libxcb-shm0\
      libx11-xcb1\            
      libx11-6\               
      libxcb1\                
      libxext6\               
      libxrandr2\             
      libxcomposite1\         
      libxcursor1\            
      libxdamage1\            
      libxfixes3\             
      libxi6\                 
      libgtk-3-0\             
      libpangocairo-1.0-0\    
      libpango-1.0-0\         
      libatk1.0-0\            
      libcairo-gobject2\      
      libcairo2\              
      libgdk-pixbuf2.0-0\     
      libglib2.0-0\           
      libasound2\             
      libxrender1\            
      libdbus-1-3
     """.trimIndent()
  )

  // Move the COPY instructions to the end
  // See https://github.com/bmuschko/gradle-docker-plugin/issues/1093
  instructions.set(
    instructions.get().sortedBy { instruction ->
      if (instruction.keyword == CopyFileInstruction.KEYWORD) 1 else 0
    }
  )
}

// `DOCKER_USERNAME=<your docker hub login> DOCKER_PASSWORD=<your docker hub password> ./gradlew dockerPushImage` to build and push the image
