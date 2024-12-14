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

package org.jraf.feeed.engine.producer.json

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step
import org.jraf.feeed.engine.producer.core.StepChain
import org.jraf.feeed.engine.producer.core.addToContextIfNotNull

class JsonFilterKeysStep : Step {
  override suspend fun execute(context: Context): Result<Context> {
    return runCatching {
      val allowedKeys: List<String> = context["allowedKeys", emptyList()]
      val jsonElement = buildJsonObject {
        val jsonObject: JsonObject = context["json"]
        for ((key, value) in jsonObject) {
          if (key in allowedKeys) {
            put(key, value)
          }
        }
      }
      context.with("json", jsonElement)
    }
  }
}

fun StepChain.jsonFilterKeys(
  allowedKeys: List<String>? = null,
): StepChain =
  addToContextIfNotNull("allowedKeys", allowedKeys) +
    JsonFilterKeysStep()
