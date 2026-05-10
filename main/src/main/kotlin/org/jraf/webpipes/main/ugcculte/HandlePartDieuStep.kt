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

package org.jraf.webpipes.main.ugcculte

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Request
import okhttp3.Response
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.httpClient
import org.jraf.webpipes.engine.util.jsonObject
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HandlePartDieuStep : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val feedItem: JsonObject = context.jsonObject("feedItem")
    val link = feedItem.string("link")
    val movieId = link.substringAfterLast('_').substringBeforeLast('.')
    val showingsUrl = "https://www.ugc.fr/showingsFilmAjaxAction!getShowingsByFilm.action?filmId=$movieId"

    val request = Request.Builder().url(showingsUrl).build()
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
      logger.warn("Failed to download $showingsUrl", e)
      throw e
    }
    val htmlBody = response.use { resp ->
      resp.body.string()
    }
    val isPartDieu = htmlBody.contains("part-dieu", ignoreCase = true)
    val newFeedItem: JsonObject = if (isPartDieu) {
      JsonObject(
        feedItem.toMutableMap().apply {
          this["link"] = JsonPrimitive("$link?isPartDieu=true")
          this["title"] = JsonPrimitive(feedItem.string("title") + " (⚠️ à Part-Dieu !)")
        },
      )
    } else {
      feedItem
    }
    return context + ("feedItem" to newFeedItem)
  }
}
