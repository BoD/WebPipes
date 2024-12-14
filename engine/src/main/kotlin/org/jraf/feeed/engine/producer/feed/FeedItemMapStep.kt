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

package org.jraf.feeed.engine.producer.feed

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain

class FeedItemMapStep(
  private val mapper: Step,
) : Step {
  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  override suspend fun execute(context: Context): Result<Context> {
    val feed: Feed = context["feed"]
    return runCatching {
      val asyncResults: List<Deferred<Result<Context>>> =
        feed.items.map { feedItem -> coroutineScope.async { mapper.execute(context.with("feedItem", feedItem)) } }

      context.with(
        "feed",
        feed.copy(
          items = List(feed.items.size) { i ->
            // Note: the mapped item's context is lost
            asyncResults[i].await().getOrThrow()["feedItem"]
          },
        ),
      )
    }
  }

  override fun close() {
    mapper.close()
    coroutineScope.cancel()
  }
}

fun StepChain.feedItemMap(mapper: Step): StepChain {
  return this + FeedItemMapStep(mapper)
}
