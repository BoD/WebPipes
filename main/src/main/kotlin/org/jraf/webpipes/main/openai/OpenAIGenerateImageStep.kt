/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.webpipes.main.openai

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.images.ImageGenerateParams
import com.openai.models.responses.ResponseCreateParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder
import org.eclipse.jgit.util.FS
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import java.util.Base64
import kotlin.random.Random
import kotlin.random.nextUInt

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class OpenAIGenerateImageStep : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val openAiApiKey: String = context.string("openAiApiKey")

    val repoDir = File("/tmp/ai-picture-of-the-day")
    val sshSessionFactory = getSshSessionFactory()
    val git = getGit(
      repoDir = repoDir,
      sshSessionFactory = sshSessionFactory,
    )

    val todayFileName = LocalDate.now().toString()
    val yesterdayFileName = LocalDate.now().minusDays(1).toString()
    
    val tmpFile = File("/tmp/webpipes-OpenAIGenerateImageStep.jpg")
    val todayFile = File(repoDir, "images/$todayFileName.jpg")

    if (!todayFile.exists()) {
      // Today's file doesn't exist yet: create it now
      // Except if it's already being created (tmp file exists)
      if (!tmpFile.exists()) {
        // Do this in the background because it can take ~30s
        coroutineScope.launch {
          try {
            tmpFile.createNewFile()
            generateImage(openAiApiKey = openAiApiKey, file = tmpFile)
            uploadToGitHub(
              sshSessionFactory = sshSessionFactory,
              git = git,
              repoDir = repoDir,
              tmpImageFile = tmpFile,
              todayFileName = todayFileName,
              todayFile = todayFile,
            )
          } finally {
            tmpFile.delete()
          }
        }
      }

      // Return yesterday's file while today's file is being created
      val resultJson = buildJsonObject {
        put("url", "https://jraf.org/ai-picture-of-the-day/images/$yesterdayFileName.jpg?${Random.nextUInt()}")
      }
      return context + ("text" to resultJson.toString())
    }

    // Otherwise return today's file
    val resultJson = buildJsonObject {
      put("url", "https://jraf.org/ai-picture-of-the-day/images/$todayFileName.jpg?${Random.nextUInt()}")
    }
    return context + ("text" to resultJson.toString())
  }

  private fun generateImage(openAiApiKey: String, file: File) {
    logger.debug("Generating prompt")
    val client: OpenAIClient = OpenAIOkHttpClient.builder()
      .apiKey(openAiApiKey)
      .build()

    // Create a prompt
    val createPromptResponseCreateParams = ResponseCreateParams.builder()
      .model(ChatModel.GPT_5_4_MINI)
      .input(
        """
          |Create 5 prompts that will be fed to an image generation tool.
          |It's for a random "picture of the day", which can be anything, but should be at least either interesting, beautiful, surprising, absurd, or otherwise worthwhile to look at.
          |It could be about nature, technology, animals, an object, a symbol, an abstract or geometric shape, a photo or drawing or painting, colorful or monochrome...
          |Surprise me!
          |The picture will be displayed on an 8in e-paper screen, please include in the prompts that the image should be optimized for that (e.g. not too much details, good contrast, etc.).
          |Do not output anything other than the prompt itself. Don't specify the resolution or aspect ratio.
          |Separate the 5 prompts with the string `----`.
          |""".trimMargin(),
      )
      .build()
    val createPromptResponse = client.responses().create(createPromptResponseCreateParams)
    val prompts = createPromptResponse.output()
      .flatMap { it.message().get().content() }
      .map { it.outputText().get().text() }
      .first()
    logger.debug("All prompts: `$prompts`")
    val randomPrompt = prompts.split("----").map { it.trim() }.random()
    logger.debug("Picked prompt: `$randomPrompt`")

    // Create the image from the prompt
    logger.debug("Generating image")
    val imageGenerateParams = ImageGenerateParams.builder()
      .model("gpt-image-2")
      .size(ImageGenerateParams.Size.of("1280x768"))
      .prompt(randomPrompt)
      .build()
    val imageGenerateResponse = client.images().generate(imageGenerateParams)
    val base64Image = imageGenerateResponse.data().get()
      .map { it.b64Json().get() }
      .first()
    file.writeBytes(Base64.getDecoder().decode(base64Image))
  }

  private fun uploadToGitHub(
    sshSessionFactory: SshSessionFactory,
    git: Git,
    repoDir: File,
    tmpImageFile: File,
    todayFileName: String,
    todayFile: File,
  ) {

    // Copy file and add it
    tmpImageFile.copyTo(todayFile, overwrite = true)
    git.add().addFilepattern("images").call()

    // Overwrite the atom file
    val atomText =
      //language=xml
      """
        <?xml version="1.0" encoding="utf-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>AI picture of the day</title>
          <id>https://jraf.org/ai-picture-of-the-day/feed.atom</id>
          <updated>${todayFileName}T00:00:00Z</updated>
        
          <entry>
            <title>$todayFileName</title>
            <id>https://jraf.org/ai-picture-of-the-day/images/$todayFileName.jpg</id>
            <updated>${todayFileName}T00:00:00Z</updated>
            <link
              rel="enclosure"
              type="image/png"
              href="https://jraf.org/ai-picture-of-the-day/images/$todayFileName.jpg" />
            <content type="html">
              <![CDATA[
                <img src="https://jraf.org/ai-picture-of-the-day/images/$todayFileName.jpg" />
              ]]>
            </content>
          </entry>
        </feed>
      """.trimIndent()
    val atomFile = File(repoDir, "feed.atom")
    atomFile.writeText(atomText)
    git.add().addFilepattern(atomFile.name).call()

    // Commit
    git.commit().setMessage("Add $todayFileName").call()

    // Push
    git.push()
      .setTransportConfigCallback { transport ->
        transport as SshTransport
        transport.setSshSessionFactory(sshSessionFactory)
      }
      .setProgressMonitor(TextProgressMonitor())
      .call()

    git.close()
  }

  /**
   * Clone the repo if not already cloned, and return a Git client for it.
   */
  private fun getGit(
    repoDir: File,
    sshSessionFactory: SshSessionFactory,
  ): Git {
    val git: Git = if (!repoDir.exists()) {
      Git.cloneRepository()
        .setURI("git@github.com:BoD/ai-picture-of-the-day.git")
        .setDirectory(repoDir)
        .setTransportConfigCallback { transport ->
          transport as SshTransport
          transport.setSshSessionFactory(sshSessionFactory)
        }
        .setProgressMonitor(TextProgressMonitor())
        .call()
    } else {
      Git.open(repoDir)
    }
    return git
  }

  private fun getSshSessionFactory(): SshSessionFactory {
    val sshDir = System.getenv("SSH_DIR")?.let { File(it) } ?: File(FS.DETECTED.userHome(), "/.ssh")
    return SshdSessionFactoryBuilder()
      .setPreferredAuthentications("publickey")
      .setSshDirectory(sshDir)
      .setDefaultIdentities { listOf(Path.of(it.absolutePath, "id_bod_2026")) }
      .setHomeDirectory(FS.DETECTED.userHome())
      .build(null)
  }
}
