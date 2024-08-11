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

import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.ProducerOutput
import org.jraf.feeed.api.producer.context
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.engine.producer.UrlTextProducer
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.toKotlinDuration

private val logger = LoggerFactory.getLogger(UrlTextProducer::class.java)

class CacheProducer<IN, OUT>(
  private val producer: Producer<IN, OUT>,
) : Producer<IN, OUT> {
  override suspend fun produce(context: ProducerContext, input: IN): Result<ProducerOutput<OUT>> {
    return runCatching {
      val maxAge: Duration = context["maxAge", 1.hours]
      val cachedValue: OUT? = context["cachedValue", null]
      if (cachedValue == null) {
        logger.debug("Nothing in cache, producing")
        val output = producer.produce(context, input).getOrThrow()
        (output.context
          .with("cachedValue", output.value)
          .with("cachedTime", Instant.now())) to output.value

      } else {
        val cachedTime: Instant = context["cachedTime"]
        if (java.time.Duration.between(Instant.now(), cachedTime).abs().toKotlinDuration() > maxAge) {
          logger.debug("Cached value is stale, producing")
          val output = producer.produce(context, input).getOrThrow()
          (output.context
            .with("cachedValue", output.value)
            .with("cachedTime", Instant.now())) to output.value
        } else {
          logger.debug("Returning cached value")
          context to cachedValue
        }
      }
    }
  }

  override fun close() {
    producer.close()
  }
}

fun <IN, OUT> Producer<IN, OUT>.cache(): Producer<IN, OUT> {
  return CacheProducer(this)
}
