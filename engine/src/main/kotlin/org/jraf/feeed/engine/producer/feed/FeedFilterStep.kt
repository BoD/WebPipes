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

import org.jraf.feeed.api.feed.Feed
import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull

class FeedFilterStep : Step {
  override suspend fun execute(context: Context): Result<Context> {
    val feed: Feed = context["feed"]
    val field: FeedItem.Field<Boolean> = context["field"]
    return runCatching {
      context.with("feed", feed.copy(items = feed.items.filter { it[field] }))
    }
  }
}

fun StepChain.feedFilter(
  field: FeedItem.Field<Boolean>? = null,
): StepChain {
  return addToContextIfNotNull("field", field) +
    FeedFilterStep()
}
