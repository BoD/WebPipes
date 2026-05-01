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
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.string
import org.jraf.webpipes.main.dropbox.getDropboxClient
import java.io.File
import java.util.Base64

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class OpenAIGenerateImageStep : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val dropboxAppKey: String = context.string("appKey")
    val dropboxAppSecret: String = context.string("appSecret")
    val dropboxRefreshToken: String = context.string("refreshToken")
    val dropboxFolder: String = context.string("folder")
    val openAiApiKey: String = context.string("openAiApiKey")

    val tmpFile = File("/tmp/webpipes-OpenAIGenerateImageStep.jpg")
    if (tmpFile.exists()) return context
    // Do this in the background because it can take ~30s
    coroutineScope.launch {
      try {
        tmpFile.createNewFile()
        generateImage(openAiApiKey = openAiApiKey, file = tmpFile)
        uploadToDropbox(
          appKey = dropboxAppKey,
          appSecret = dropboxAppSecret,
          refreshToken = dropboxRefreshToken,
          file = tmpFile,
          destinationFilePath = "$dropboxFolder/image-${System.currentTimeMillis()}.jpg",
        )
      } finally {
        tmpFile.delete()
      }
    }
    return context
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

  private fun uploadToDropbox(
    appKey: String,
    appSecret: String,
    refreshToken: String,
    file: File,
    destinationFilePath: String,
  ) {
    logger.debug("Uploading $destinationFilePath to $destinationFilePath")
    val client = getDropboxClient(
      appKey = appKey,
      appSecret = appSecret,
      refreshToken = refreshToken,
    )
    client.files().uploadBuilder(destinationFilePath).uploadAndFinish(file.inputStream())
  }
}
