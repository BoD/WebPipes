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

package org.jraf.webpipes.main.ugc

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.atom.AtomStep
import org.jraf.webpipes.engine.execute.StepChainExecutor
import org.jraf.webpipes.engine.execute.StepExecutor
import org.jraf.webpipes.engine.step.core.CacheStep
import org.jraf.webpipes.engine.step.core.CopyContextFieldStep
import org.jraf.webpipes.engine.step.feed.AddFeedItemFieldToContextStep
import org.jraf.webpipes.engine.step.feed.FeedFilterStep
import org.jraf.webpipes.engine.step.feed.FeedItemMapFieldStep
import org.jraf.webpipes.engine.step.feed.FeedItemMapStep
import org.jraf.webpipes.engine.step.feed.FeedItemTextContainsStep
import org.jraf.webpipes.engine.step.feed.FeedMaxItemsStep
import org.jraf.webpipes.engine.step.feed.MergeFeedsStep
import org.jraf.webpipes.engine.step.html.HtmlCropStep
import org.jraf.webpipes.engine.step.html.HtmlFeedStep
import org.jraf.webpipes.engine.step.net.UrlTextStep
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import org.jraf.webpipes.server.RequestParams

private val url =
  "https://www.ugc.fr/filmsAjaxAction!getFilmsAndFilters.action?filter=stillOnDisplay&page=30010&cinemaId=&reset=false&__multiselect_versions=&labels=UGC%20Culte&__multiselect_labels=&__multiselect_groupeImages="

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
          put("id", "createFeed")
          put("type", HtmlFeedStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("baseUrl", url)
              put("aElementsXPath", "//div[@class='info-wrapper']//a")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "downloadMoviePage")
          put("type", UrlTextStep::class.qualifiedName)
        },
      )

      add(
        buildJsonObject {
          put("id", "mapMovieDownloadToFeedItemBody")
          put("type", FeedItemMapFieldStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("mapperId", "downloadMoviePage")
              put("inFeedItemFieldName", "link")
              put("inContextFieldName", "url")
              put("outFeedItemFieldName", "body")
              put("outContextFieldName", "text")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "mapperDownloadMoviePage")
          put("type", FeedItemMapStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("mapperId", "mapMovieDownloadToFeedItemBody")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "addIsLyon")
          put("type", FeedItemTextContainsStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("inFeedItemFieldName", "body")
              put("outFeedItemFieldName", "isLyon")
              put("textToFind", "lyon")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "mapperAddIsLyon")
          put("type", FeedItemMapStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("mapperId", "addIsLyon")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "filterIsLyon")
          put("type", FeedFilterStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("conditionFeedItemFieldName", "isLyon")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "cropMovieBody")
          put("type", HtmlCropStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("xPath", "//div[@class='group-info d-none d-md-block'][4]/p[2]")
              put("extractText", true)
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "mapMovieCroppedBodyToFeedItemBody")
          put("type", FeedItemMapFieldStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("mapperId", "cropMovieBody")
              put("inFeedItemFieldName", "body")
              put("inContextFieldName", "text")
              put("outFeedItemFieldName", "body")
              put("outContextFieldName", "text")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "putLinkIntoContext")
          put("type", AddFeedItemFieldToContextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("contextFieldName", "baseUrl")
              put("feedItemFieldName", "link")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "putLinkIntoContextAndMapMovieCroppedBodyToFeedItemBody")
          put("type", StepChainExecutor::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put(
                "chain",
                buildJsonArray {
                  add("putLinkIntoContext")
                  add("mapMovieCroppedBodyToFeedItemBody")
                },
              )
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "mapperCropMovieBody")
          put("type", FeedItemMapStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("mapperId", "putLinkIntoContextAndMapMovieCroppedBodyToFeedItemBody")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "mergeFeeds")
          put("type", MergeFeedsStep::class.qualifiedName)
        },
      )

      add(
        buildJsonObject {
          put("id", "feedMaxItems")
          put("type", FeedMaxItemsStep::class.qualifiedName)
        },
      )

      add(
        buildJsonObject {
          put("id", "saveExistingFeed")
          put("type", CopyContextFieldStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("sourceFieldName", "feed")
              put("targetFieldName", "existingFeed")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "addAtomLink")
          put("type", CopyContextFieldStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("sourceFieldName", "requestUrl")
              put("targetFieldName", "atomLink")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "atom")
          put("type", AtomStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("atomTitle", "UGC Culte")
              put("atomDescription", "UGC Culte")
              put("atomEntriesAuthor", "WebPipes")
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
                  add("createFeed")
                  add("mapperDownloadMoviePage")
                  add("mapperAddIsLyon")
                  add("filterIsLyon")
                  add("mapperCropMovieBody")
                  add("mergeFeeds")
                  add("feedMaxItems")
                  add("saveExistingFeed")
                  add("addAtomLink")
                  add("atom")
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
            },
          )
        },
      )
    },
  )
}

suspend fun executeUgc(requestParams: RequestParams): String {
  context = StepExecutor()
    .execute(
      context +
        ("requestUrl" to requestParams.requestUrl) +
        ("stepId" to "cache"),
    )
  return context.string("text")
}
