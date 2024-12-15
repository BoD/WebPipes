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

package org.jraf.feeed.engine.step.net

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.classLogger
import org.jraf.feeed.engine.util.jsonObject
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import org.jraf.feeed.engine.util.stringOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UrlTextStep : Step {
  private val logger = classLogger()

  private val httpClient = OkHttpClient.Builder().build()

  override suspend fun execute(context: JsonObject): JsonObject {
    val url: String = context.string("url")
    logger.debug("Fetching {}", url)
    val request = Request.Builder()
      .url(url)
      .apply {
        context.jsonObject("headers", JsonObject(emptyMap())).let { headers ->
          for ((key, value) in headers) {
            addHeader(key, value.string)
          }
        }
      }
      .apply {
        val body: String? = context.stringOrNull("body")
        body?.let {
          post(it.toRequestBody(null))
        }
      }
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
      logger.error("Failed to fetch $url", e)
      throw e
    }

    return response.use { resp ->
      val body = resp.body?.string() ?: throw Exception("Failed to fetch $url: empty body")
      context + ("text" to body)
    }
  }

  override fun close() {
    httpClient.dispatcher.executorService.shutdown()
    httpClient.connectionPool.evictAll()
  }
}
