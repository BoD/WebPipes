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

package org.jraf.feeed.engine.step.text

import kotlinx.serialization.json.JsonObject
import org.jraf.feeed.api.Step
import org.jraf.feeed.engine.util.plus
import org.jraf.feeed.engine.util.string

class ReplaceStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val inputFieldName = context.string("inputFieldName", "text")
    val text = context.string(inputFieldName)
    val outputFieldName = context.string("outputFieldName", "text")
    val regex = context.string("regex")
    val replacement = context.string("replacement")
    return context + (outputFieldName to text.replace(regex.toRegex(), replacement))
  }
}
