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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.engine.producer.core.pipe
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(JsonProducer::class.java)

class JsonProducer : Producer<String, JsonElement> {
  override suspend fun produce(context: ProducerContext, input: String): Result<Pair<ProducerContext, JsonElement>> {
    return runCatching {
      logger.debug("Parsing JSON")
      context to Json.parseToJsonElement(input)
    }
  }

  override fun close() {}
}

fun <IN> Producer<IN, String>.json(): Producer<IN, JsonElement> = this.pipe(JsonProducer())
