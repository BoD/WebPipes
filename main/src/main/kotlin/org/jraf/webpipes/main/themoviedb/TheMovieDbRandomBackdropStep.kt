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

package org.jraf.webpipes.main.themoviedb

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.Request
import okhttp3.Response
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.httpClient
import org.jraf.webpipes.engine.util.jsonArray
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

class TheMovieDbRandomBackdropStep : Step {
  private val logger = classLogger()

  private suspend fun fetch(url: String, apiToken: String): JsonObject {
    logger.debug("Fetching {}", url)
    val request = Request.Builder()
      .url(url)
      .addHeader("accept", "application/json")
      .addHeader("Authorization", "Bearer $apiToken")
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
    val body = response.use { resp ->
      resp.body?.string() ?: throw Exception("Failed to fetch $url: empty body")
    }
    return Json.parseToJsonElement(body).jsonObject
  }

  override suspend fun execute(context: JsonObject): JsonObject {
    val apiToken: String = context.string("apiToken")

    val url =
      "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&sort_by=vote_average.desc&vote_count.gte=1000&without_genres=16"
    val json = fetch(url = url, apiToken = apiToken)
//    val totalResults = json.int("total_results")
    val maxSize = 500
    val resultsPerPage = json.jsonArray("results").size

    // Make the seed depend on the current day
    val seed = Instant.now().toString().substring(0, 10).replace("-", "").toInt()
    val randomIndex = (0..<maxSize).random(Random(seed))
    val randomPage = randomIndex / resultsPerPage
    val randomIndexOnPage = randomIndex % resultsPerPage
    val randomUrl =
      "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=en-US&page=$randomPage&sort_by=vote_average.desc&vote_count.gte=1000&without_genres=16"

    val randomJson = fetch(url = randomUrl, apiToken = apiToken)
    val randomItem = randomJson.jsonArray("results")[randomIndexOnPage].jsonObject
    val backdropPath = randomItem.string("backdrop_path")
    val backdropFullUrl = "https://image.tmdb.org/t/p/w1280/$backdropPath"

    val resultJson = buildJsonObject {
      put("url", backdropFullUrl)
    }

    return context + ("text" to resultJson.toString())
  }
}
