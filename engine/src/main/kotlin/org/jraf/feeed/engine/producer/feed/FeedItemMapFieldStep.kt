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

class FeedItemMapFieldStep<T>(
  private val mapper: Step,
) : Step {
  override suspend fun execute(context: Context): Result<Context> {
    val feedItem: FeedItem = context["feedItem"]
    val fieldIn: FeedItem.Field<T> = context["fieldIn"]
    val fieldInVariableName: String = context["fieldInVariableName"]
    val fieldOut: FeedItem.Field<T> = context["fieldOut"]
    val fieldOutVariableName: String = context["fieldOutVariableName"]

    return runCatching {
      val fieldCurrentValue: T = feedItem[fieldIn]
      val fieldNewValue: T = mapper.execute(context.with(fieldInVariableName, fieldCurrentValue)).getOrThrow()[fieldOutVariableName]
      // Note: the mapped field's context is lost
      context.with("feedItem", feedItem.with(fieldOut, fieldNewValue))
    }
  }

  override fun close() {
    mapper.close()
  }
}

fun <T> StepChain.feedItemMapField(
  mapper: Step,
  fieldIn: FeedItem.Field<out T?>? = null,
  fieldInVariableName: String? = null,
  fieldOut: FeedItem.Field<out T?>? = null,
  fieldOutVariableName: String? = null,
): StepChain {
  return addToContextIfNotNull("fieldIn", fieldIn)
    .addToContextIfNotNull("fieldInVariableName", fieldInVariableName)
    .addToContextIfNotNull("fieldOut", fieldOut)
    .addToContextIfNotNull("fieldOutVariableName", fieldOutVariableName) +
    FeedItemMapFieldStep<T>(mapper)
}
