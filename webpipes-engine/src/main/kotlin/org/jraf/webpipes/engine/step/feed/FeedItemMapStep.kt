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

package org.jraf.webpipes.engine.step.feed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.execute.StepExecutor
import org.jraf.webpipes.engine.util.jsonArray
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string

class FeedItemMapStep : Step {
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override suspend fun execute(context: JsonObject): JsonObject {
    val mapperId = context.string("mapperId")
    val mapper = StepExecutor()
    val feed: JsonArray = context.jsonArray("feed")
    val asyncResults: List<Deferred<JsonObject>> =
      feed.map { feedItem -> coroutineScope.async { mapper.execute(context + ("feedItem" to feedItem) + ("stepId" to mapperId)) } }

    return context + (
      "feed" to
        JsonArray(
          List(feed.size) { i ->
            // Note: the mapped item's context is lost
            asyncResults[i].await()["feedItem"] ?: error("Missing 'feedItem' in mapper result")
          },
        )
      )
  }

  override fun close() {
    coroutineScope.cancel()
  }
}
