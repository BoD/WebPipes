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

package org.jraf.feeed.engine.source.html

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jraf.feeed.engine.model.feed.Feed
import org.jraf.feeed.engine.model.feed.FeedItem
import org.jraf.feeed.engine.model.source.FeedSource
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import us.codecraft.xsoup.Xsoup
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class HtmlFeedSource(
  private val url: String,
  private val xPath: String,
) : FeedSource {
  private val logger = LoggerFactory.getLogger(HtmlFeedSource::class.java)

  private val xPathEvaluator = Xsoup.compile(xPath)
  private val httpClient = OkHttpClient.Builder().build()

  override suspend fun produce(): Result<Feed> {
    val call = httpClient.newCall(Request.Builder().url(url).build())
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
      return Result.failure(e)
    }

    return response.use { resp ->
      if (!resp.isSuccessful) {
        return Result.failure(Exception("Failed to fetch $url: ${resp.code} - ${resp.message}"))
      }
      val body = resp.body?.string() ?: return Result.failure(Exception("Failed to fetch $url: empty body"))
      val document: Document = Jsoup.parse(body, url)
      val items = xPathEvaluator.evaluate(document).elements.map { aElement ->
        FeedItem(
          title = aElement.text(),
          link = aElement.attr("abs:href"),
          date = "",
        )
      }
      Result.success(Feed(items))
    }
  }

  override fun close() {
    httpClient.dispatcher.executorService.shutdown()
    httpClient.connectionPool.evictAll()
  }
}

suspend fun main() {
  val feedSource = HtmlFeedSource(
    url = "https://www.ugc.fr/filmsAjaxAction!getFilmsAndFilters.action?filter=stillOnDisplay&page=30010&cinemaId=&reset=false&__multiselect_versions=&labels=UGC%20Culte&__multiselect_labels=&__multiselect_groupeImages=",
    xPath = "//div[@class='info-wrapper']//a",
  )
  val feed = feedSource.produce()
  println(feed)
  feedSource.close()
}
