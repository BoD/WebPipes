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

package org.jraf.feeed.engine.producer

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = LoggerFactory.getLogger(UrlTextProducer::class.java)

class UrlTextProducer : Producer<String, String> {
  private val httpClient = OkHttpClient.Builder().build()

  override suspend fun produce(context: ProducerContext, input: String): Result<Pair<ProducerContext, String>> {
    logger.debug("Fetching {}", input)
    val call = httpClient.newCall(Request.Builder().url(input).build())
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
      logger.error("Failed to fetch $input", e)
      return Result.failure(e)
    }

    return response.use { resp ->
      val body = resp.body?.string() ?: return Result.failure(Exception("Failed to fetch $input: empty body"))
      Result.success(context to body)
    }
  }

  override fun close() {
    httpClient.dispatcher.executorService.shutdown()
    httpClient.connectionPool.evictAll()
  }
}
