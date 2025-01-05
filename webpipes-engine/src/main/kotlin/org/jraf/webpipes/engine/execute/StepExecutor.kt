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

package org.jraf.webpipes.engine.execute

import kotlinx.serialization.json.JsonObject
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.jsonArray
import org.jraf.webpipes.engine.util.jsonObject
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string

class StepExecutor : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val stepId = context.string("stepId")
    val stepsById = context.jsonArray("steps")
      .associateBy { (it as JsonObject).string("id") }
      .mapValues { it.value as JsonObject }
    val stepDeclaration = stepsById[stepId] ?: error("Step id '$stepId' not found in steps")
    val stepType = stepDeclaration.string("type")
    val stepConfiguration = stepDeclaration.jsonObject("configuration", JsonObject(emptyMap()))
    logger.debug("Executing step {}", stepType)
    return when {
      stepType.startsWith("local:") -> {
        val localStepType = stepType.removePrefix("local:")
        LocalStepExecutor(localStepType).execute(context + stepConfiguration)
      }

      stepType.startsWith("http:") || stepType.startsWith("https:") -> {
        RemoteStepExecutor(stepType).execute(context + stepConfiguration)
      }

      else -> error("Unknown step type '$stepType'")
    }
  }
}
