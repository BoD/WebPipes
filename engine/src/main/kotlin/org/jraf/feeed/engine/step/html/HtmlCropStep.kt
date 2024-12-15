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

package org.jraf.feeed.engine.step.html

import kotlinx.serialization.json.JsonObject
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.boolean
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import us.codecraft.xsoup.Xsoup

class HtmlCropStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val text = context.string("text")
    val baseUrl = context.string("baseUrl")
    val xPath = context.string("xPath")
    val extractText = context.boolean("extractText", false)
    val xPathEvaluator = Xsoup.compile(xPath)
    val document: Document = Jsoup.parse(text, baseUrl)
    val element = xPathEvaluator.evaluate(document).elements.first()
    val newText = (if (extractText) element?.text() else element?.outerHtml()) ?: ""
    return context + ("text" to newText)
  }
}