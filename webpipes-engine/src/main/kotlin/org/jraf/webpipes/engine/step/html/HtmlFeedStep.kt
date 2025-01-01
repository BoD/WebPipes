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

@file:OptIn(ExperimentalSerializationApi::class)

package org.jraf.webpipes.engine.step.html

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import us.codecraft.xsoup.Xsoup
import java.time.Instant

class HtmlFeedStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val baseUrl = context.string("baseUrl")
    val xPath = context.string("aElementsXPath")
    val xPathEvaluator = Xsoup.compile(xPath)
    val text = context.string("text")

    val document: Document = Jsoup.parse(text, baseUrl)
    val items = xPathEvaluator.evaluate(document).elements.map { aElement ->
      buildJsonObject {
        put("title", aElement.text())
        put("link", aElement.absUrl("href"))
        put("date", Instant.now().toString())
      }
    }
    return context + ("feed" to JsonArray(items))
  }
}
