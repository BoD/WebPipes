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

import org.jraf.feeed.api.feed.FeedItem
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull

class FeedItemTextContainsStep : Step {
  override suspend fun execute(context: Context): Result<Context> {
    val feedItem: FeedItem = context["feedItem"]
    val fieldIn: FeedItem.Field<String?> = context["fieldIn"]
    val textToFind: String = context["textToFind"]
    val fieldOut: FeedItem.Field<Boolean> = context["fieldOut"]

    return runCatching {
      val fieldValue: String? = feedItem[fieldIn]
      context.with("feedItem", feedItem.with(fieldOut, fieldValue?.contains(textToFind, ignoreCase = true) == true))
    }
  }
}

fun StepChain.feedItemTextContains(
  fieldIn: FeedItem.Field<String?>? = null,
  textToFind: String? = null,
  fieldOut: FeedItem.Field<Boolean>? = null,
): StepChain {
  return addToContextIfNotNull("fieldIn", fieldIn)
    .addToContextIfNotNull("textToFind", textToFind)
    .addToContextIfNotNull("fieldOut", fieldOut) +
    FeedItemTextContainsStep()
}
