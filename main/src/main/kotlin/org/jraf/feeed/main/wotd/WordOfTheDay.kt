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

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.feeed.engine.execute.StepChainExecutor
import org.jraf.feeed.engine.execute.StepExecutor
import org.jraf.feeed.engine.step.core.CacheStep
import org.jraf.feeed.engine.step.html.HtmlExtractStep
import org.jraf.feeed.engine.step.json.ContextToJsonStep
import org.jraf.feeed.engine.step.json.JsonFilterKeysStep
import org.jraf.feeed.engine.step.json.JsonToTextStep
import org.jraf.feeed.engine.step.net.UrlTextStep
import org.jraf.feeed.engine.step.text.ReplaceStep
import org.jraf.feeed.engine.step.text.SubstringStep
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import org.jraf.feeed.server.RequestParams

private val url = "https://www.merriam-webster.com/word-of-the-day"

private var context = buildJsonObject {
  put(
    "steps",
    buildJsonArray {
      add(
        buildJsonObject {
          put("id", "downloadPage")
          put("type", UrlTextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("url", url)
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractWord")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//h2[@class='word-header-txt']")
              put("outputFieldName", "word")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractType")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//span[@class='main-attr']")
              put("outputFieldName", "type")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractPronunciation")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//span[@class='word-syllables']")
              put("outputFieldName", "pronunciation")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractDefinition")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//div[@class='wod-definition-container']/p[1]")
              put("outputFieldName", "definition")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractInContext")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//div[@class='wotd-examples']/div[1]/p[1]")
              put("outputFieldName", "inContext")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "extractExample")
          put("type", HtmlExtractStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//div[@class='wod-definition-container']/p[2]")
              put("outputFieldName", "example")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "substringExample")
          put("type", SubstringStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inputFieldName", "example")
              put("outputFieldName", "example")
              put("startIndex", 3)
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "removeAFromInContext1")
          put("type", ReplaceStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inputFieldName", "inContext")
              put("outputFieldName", "inContext")
              put("regex", "<a")
              put("replacement", "<i")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "removeAFromInContext2")
          put("type", ReplaceStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inputFieldName", "inContext")
              put("outputFieldName", "inContext")
              put("regex", "</a")
              put("replacement", "</i")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "contextToJson")
          put("type", ContextToJsonStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("outputFieldName", "json")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "filterJsonKeys")
          put("type", JsonFilterKeysStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inputFieldName", "json")
              put(
                "allowedKeys",
                buildJsonArray {
                  add("word")
                  add("type")
                  add("pronunciation")
                  add("definition")
                  add("example")
                  add("inContext")
                },
              )
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "jsonToText")
          put("type", JsonToTextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inputFieldName", "json")
              put("outputFieldName", "text")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "cachedChain")
          put("type", StepChainExecutor::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put(
                "chain",
                buildJsonArray {
                  add("downloadPage")
                  add("extractWord")
                  add("extractType")
                  add("extractPronunciation")
                  add("extractDefinition")
                  add("extractInContext")
                  add("extractExample")
                  add("substringExample")
                  add("removeAFromInContext1")
                  add("removeAFromInContext2")
                  add("contextToJson")
                  add("filterJsonKeys")
                  add("jsonToText")
                },
              )
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "cache")
          put("type", CacheStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("cachedStepId", "cachedChain")
              put("maxAge", 60 * 60 * 3)
            },
          )
        },
      )
    },
  )
}

suspend fun produceWordOfTheDay(requestParams: RequestParams): String {
  context = StepExecutor()
    .execute(
      context +
        ("stepId" to "cache"),
    )
  return context.string("text")
}
