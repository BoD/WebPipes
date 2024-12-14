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

package org.jraf.feeed.main.ugc

import org.jraf.feeed.api.step.Context
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContext
import org.jraf.feeed.engine.producer.core.addVariableToContext
import org.jraf.feeed.engine.producer.core.cache
import org.jraf.feeed.engine.producer.html.htmlExtract
import org.jraf.feeed.engine.producer.json.contextToJson
import org.jraf.feeed.engine.producer.json.jsonFilterKeys
import org.jraf.feeed.engine.producer.json.jsonToText
import org.jraf.feeed.engine.producer.net.urlText
import org.jraf.feeed.engine.producer.text.substring
import org.jraf.feeed.server.RequestParams

private val url = "https://www.merriam-webster.com/word-of-the-day"

private val stepChain: StepChain =
  StepChain()
    .addToContext("baseUrl", url)
    .urlText()
    .htmlExtract(variableName = "word", xPath = "//h2[@class='word-header-txt']")
    .htmlExtract(variableName = "type", xPath = "//span[@class='main-attr']")
    .htmlExtract(variableName = "pronunciation", xPath = "//span[@class='word-syllables']")
    .htmlExtract(variableName = "definition", xPath = "//div[@class='wod-definition-container']/p[1]")
    .htmlExtract(variableName = "inContext", xPath = "//div[@class='wotd-examples']/div[1]/p[1]")
    .htmlExtract(variableName = "text", xPath = "//div[@class='wod-definition-container']/p[2]")
    .substring(startIndex = 3)
    .addVariableToContext(newVariableName = "example", existingVariableName = "text")
    .contextToJson()
    .jsonFilterKeys(
      listOf(
        "word",
        "type",
        "pronunciation",
        "definition",
        "example",
        "inContext",
      ),
    )
    .jsonToText()
    .cache()

private var context = Context()

suspend fun produceWordOfTheDay(requestParams: RequestParams): String {
  context = stepChain
    .execute(
      context
        .with("requestUrl", requestParams.requestUrl)
        .with("url", url),
    )
    .getOrThrow()
  return context["text"]
}
