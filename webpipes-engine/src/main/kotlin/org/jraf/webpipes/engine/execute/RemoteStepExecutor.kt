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

package org.jraf.webpipes.engine.execute

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.httpClient
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RemoteStepExecutor(
  private val stepUrl: String,
) : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    logger.debug("Executing remote step {}", stepUrl)
    val request = Request.Builder()
      .url(stepUrl)
      .addHeader("Content-Type", "application/json")
      .post(Json.encodeToString(context).toRequestBody("application/json".toMediaTypeOrNull()))
      .build()
    val call = httpClient.newCall(request)
    val response: Response = try {
      suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation {
          call.cancel()
        }
        try {
          continuation.resume(call.execute())
        } catch (e: Exception) {
          continuation.resumeWithException(e)
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to execute remote step $stepUrl", e)
      throw e
    }

    return response.use { resp ->
      val body = resp.body?.string() ?: throw Exception("Failed to read response body")
      Json.parseToJsonElement(body).jsonObject
    }
  }
}
