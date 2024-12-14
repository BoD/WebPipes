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

package org.jraf.feeed.engine.producer.net

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = LoggerFactory.getLogger(UrlTextStep::class.java)

class UrlTextStep : Step {
  private val httpClient = OkHttpClient.Builder().build()

  override suspend fun execute(context: Context): Result<Context> {
    val url: String = context["url"]
    logger.debug("Fetching {}", url)
    val request = Request.Builder()
      .url(url)
      .apply {
        context["headers", emptyMap<String, String>()].let { headers ->
          for ((key, value) in headers) {
            addHeader(key, value)
          }
        }
      }
      .apply {
        val body: String? = context["body", null]
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
      return Result.failure(e)
    }

    return response.use { resp ->
      val body = resp.body?.string() ?: return Result.failure(Exception("Failed to fetch $url: empty body"))
      Result.success(context.with("text", body))
    }
  }

  override fun close() {
    httpClient.dispatcher.executorService.shutdown()
    httpClient.connectionPool.evictAll()
  }
}

fun StepChain.urlText(
  url: String? = null,
  body: String? = null,
  headers: Map<String, String>? = null,
): StepChain = addToContextIfNotNull("url", url)
  .addToContextIfNotNull("body", body)
  .addToContextIfNotNull("headers", headers) +
  UrlTextStep()
