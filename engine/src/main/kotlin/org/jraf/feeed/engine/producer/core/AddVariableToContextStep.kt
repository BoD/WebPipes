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

package org.jraf.feeed.engine.producer.core

import org.jraf.feeed.api.step.Context
import org.jraf.feeed.api.step.Step

class AddVariableToContextStep : Step {
  override suspend fun execute(context: Context): Result<Context> {
    return runCatching {
      val existingVariableName: String = context["existingVariableName"]
      val existingVariableValue: Any = context[existingVariableName]
      val newVariableName: String = context["newVariableName"]
      context.with(newVariableName, existingVariableValue)
    }
  }
}

fun StepChain.addVariableToContext(
  newVariableName: String? = null,
  existingVariableName: String? = null,
): StepChain {
  return addToContextIfNotNull("newVariableName", newVariableName)
    .addToContextIfNotNull("existingVariableName", existingVariableName) +
    AddVariableToContextStep()
}
