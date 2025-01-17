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

import kotlinx.serialization.json.JsonObject
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.execute.StepExecutor
import org.jraf.webpipes.engine.util.jsonObject
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string

/**
 * Given a feed item `feedItem`, executes the step `mapperId`.
 * The feed item field `inFeedItemFieldName` is put into context with the name specified in `inContextFieldName`.
 * The field `outContextFieldName` of the result of executing the step is set to the feed item with the name specified in `outFeedItemFieldName`.
 */
class FeedItemMapFieldStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val mapperId = context.string("mapperId")
    val mapper = StepExecutor()
    val feedItem: JsonObject = context.jsonObject("feedItem")

    val inFeedItemFieldName = context.string("inFeedItemFieldName")
    val inContextFieldName = context.string("inContextFieldName")
    val outFeedItemFieldName = context.string("outFeedItemFieldName")
    val outContextFieldName = context.string("outContextFieldName")

    val fieldCurrentValue = feedItem[inFeedItemFieldName]
    val fieldNewValue = mapper.execute(context + (inContextFieldName to fieldCurrentValue) + ("stepId" to mapperId))[outContextFieldName]

    // Note: the rest of the context is lost
    return context + ("feedItem" to feedItem + (outFeedItemFieldName to fieldNewValue))
  }
}
