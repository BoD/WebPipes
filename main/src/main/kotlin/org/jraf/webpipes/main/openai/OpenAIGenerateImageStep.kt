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
import com.openai.models.images.Image
import com.openai.models.images.ImageGenerateParams
import com.openai.models.images.ImageGenerateParams.Companion.builder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.string
import org.jraf.webpipes.main.dropbox.getDropboxClient
import java.io.File
import java.util.Base64

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class OpenAIGenerateImageStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val dropboxAppKey: String = context.string("appKey")
    val dropboxAppSecret: String = context.string("appSecret")
    val dropboxRefreshToken: String = context.string("refreshToken")
    val dropboxFolder: String = context.string("folder")
    val openAiApiKey: String = context.string("openAiApiKey")

    // Do this in the background because it can take ~30s
    coroutineScope.launch {
      val file = generateImage(openAiApiKey = openAiApiKey)
      uploadToDropbox(
        appKey = dropboxAppKey,
        appSecret = dropboxAppSecret,
        refreshToken = dropboxRefreshToken,
        file = file,
        destinationFilePath = "$dropboxFolder/${file.name}",
      )
      file.delete()
    }
    return context
  }

  private fun generateImage(openAiApiKey: String): File {
    val client: OpenAIClient = OpenAIOkHttpClient.builder()
      .apiKey(openAiApiKey)
      .build()

    val imageGenerateParams = builder()
      .model("gpt-image-2")
      .size(ImageGenerateParams.Size.of("1280x768"))
      .prompt(
        """
        |A random "picture of the day", which can be anything, but should be at least either interesting, beautiful, surprising, or otherwise worthwhile to look at.
        |It could be about nature, technology, animals, an object, an abstract or geometric shape, a photo or drawing or painting.
        |Surprise me!
        |The image will be displayed on an e-paper display in my hallway, and I'll generate a new one every day.
        |""".trimMargin(),
      )
      .build()

    val file = File("/tmp/image-${System.currentTimeMillis()}.jpg")
    client.images().generate(imageGenerateParams).data().orElseThrow().stream()
      .flatMap { image: Image -> image.b64Json().stream() }
      .forEach { base64: String ->
        file.writeBytes(Base64.getDecoder().decode(base64))
      }
    return file
  }

  private fun uploadToDropbox(
    appKey: String,
    appSecret: String,
    refreshToken: String,
    file: File,
    destinationFilePath: String,
  ) {
    val client = getDropboxClient(
      appKey = appKey,
      appSecret = appSecret,
      refreshToken = refreshToken,
    )
    client.files().uploadBuilder(destinationFilePath).uploadAndFinish(file.inputStream())
  }
}
