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

package org.jraf.feeed.engine.execute

import kotlinx.serialization.json.JsonObject
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.jsonArray
import org.jraf.feeed.engine.util.jsonObject
import org.jraf.feeed.engine.util.string

class StepExecutor : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val stepId = context.string("stepId")
    val stepsById = context.jsonArray("steps")
      .associateBy { (it as JsonObject).string("id") }
      .mapValues { it.value as JsonObject }
    val stepDeclaration = stepsById[stepId] ?: error("Step id '$stepId' not found in steps")
    val stepType = stepDeclaration.string("type")
    val stepConfiguration = stepDeclaration.jsonObject("configuration", JsonObject(emptyMap()))
    val stepInstance = Class.forName(stepType).getDeclaredConstructor().newInstance() as Step
    return stepInstance.execute(JsonObject(context + stepConfiguration))
  }
}
