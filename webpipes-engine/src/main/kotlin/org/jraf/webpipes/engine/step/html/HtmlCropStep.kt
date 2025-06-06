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

package org.jraf.webpipes.engine.step.html

import kotlinx.serialization.json.JsonObject
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.boolean
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import org.jraf.webpipes.engine.util.stringOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import us.codecraft.xsoup.Xsoup

/**
 * Given an HTML text in `text`, extracts the content of the first element matching the XPath expression in `xPath` and stores it in the
 * `text` field.
 */
class HtmlCropStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val text = context.string("text")
    val baseUrl = context.stringOrNull("baseUrl")
    val xPath = context.string("xPath")
    val extractText = context.boolean("extractText", false)
    val xPathEvaluator = Xsoup.compile(xPath)
    val document: Document = if (baseUrl != null) Jsoup.parse(text, baseUrl) else Jsoup.parse(text)
    val element = xPathEvaluator.evaluate(document).elements.first()
    val newText = (if (extractText) element?.text() else element?.outerHtml()) ?: ""
    return context + ("text" to newText)
  }
}
