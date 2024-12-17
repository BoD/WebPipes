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

package org.jraf.feeed.main.bsky

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.feeed.atom.AtomStep
import org.jraf.feeed.engine.execute.StepChainExecutor
import org.jraf.feeed.engine.execute.StepExecutor
import org.jraf.feeed.engine.step.core.CacheStep
import org.jraf.feeed.engine.step.core.CopyContextFieldStep
import org.jraf.feeed.engine.step.core.RemoveFromContextStep
import org.jraf.feeed.engine.step.feed.FeedMaxItemsStep
import org.jraf.feeed.engine.step.feed.MergeFeedsStep
import org.jraf.feeed.engine.step.json.TextToJsonStep
import org.jraf.feeed.engine.step.net.UrlTextStep
import org.jraf.feeed.engine.step.text.BuildStringFromContextFields
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string
import org.jraf.feeed.server.RequestParams

private val createSessionUrl = "https://bsky.social/xrpc/com.atproto.server.createSession"
private val getListFeedUrl =
  "https://bsky.social/xrpc/app.bsky.feed.getListFeed?list=at%3A%2F%2Fdid%3Aplc%3Azrwjh3urruteuvjonaajoq3r%2Fapp.bsky.graph.list%2F3lbclce6ypy2p&limit=30"

private var context = buildJsonObject {
  put(
    "steps",
    buildJsonArray {
      add(
        buildJsonObject {
          put("id", "createSessionBody")
          put("type", BuildStringFromContextFields::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("template", """{"identifier": "{{identifier}}", "password": "{{password}}"}""")
              put("outputField", "body")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "createSession")
          put("type", UrlTextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("url", createSessionUrl)
              put(
                "headers",
                buildJsonObject {
                  put("Content-Type", "application/json")
                },
              )
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "addAuthorizationHeader")
          put("type", BlueSkyAddAuthorizationHeaderStep::class.qualifiedName)
        },
      )

      add(
        buildJsonObject {
          put("id", "removeBody")
          put("type", RemoveFromContextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("fieldName", "body")
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "getListFeed")
          put("type", UrlTextStep::class.qualifiedName)
          put(
            "configuration",
            buildJsonObject {
              put("url", getListFeedUrl)
            },
          )
        },
      )

      add(
        buildJsonObject {
          put("id", "listFeedTextToJson")
          put("type", TextToJsonStep::class.qualifiedName)
        },
      )

      add(
        buildJsonObject {
          put("id", "blueSkyJsonToFeed")
          put("type", BlueSkyJsonToFeedStep::class.qualifiedName)
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
              put("atomTitle", "Bluesky")
              put("atomDescription", "Bluesky")
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
                  add("createSessionBody")
                  add("createSession")
                  add("addAuthorizationHeader")
                  add("removeBody")
                  add("getListFeed")
                  add("listFeedTextToJson")
                  add("blueSkyJsonToFeed")
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

suspend fun produceBlueSky(requestParams: RequestParams): String {
  context = StepExecutor()
    .execute(
      context +
        ("requestUrl" to requestParams.requestUrl) +
        ("identifier" to requestParams.queryParams["identifier"]!!) +
        ("password" to requestParams.queryParams["password"]!!) +
        ("stepId" to "cache"),
    )
  return context.string("text")
}
