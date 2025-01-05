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

class LocalStepExecutor(
  private val type: String,
) : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val stepInstance = Class.forName(type).getDeclaredConstructor().newInstance() as Step
    logger.debug("Executing local step {}", stepInstance)
    return stepInstance.execute(JsonObject(context))
  }
}
