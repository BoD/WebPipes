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

class AddToContextProducer<T>(private val additionalContext: ProducerContext) : Producer<T, T> {
  constructor(vararg additionalContext: Pair<String, Any?>) : this(additionalContext.fold(ProducerContext()) { acc, (key, value) ->
    acc.with(
      key,
      value
    )
  })

  override suspend fun produce(context: ProducerContext, input: T): Result<Pair<ProducerContext, T>> {
    return Result.success((context + additionalContext) to input)
  }

  override fun close() {}
}

fun <IN, OUT> Producer<IN, OUT>.addToContext(additionalContext: ProducerContext): Producer<IN, OUT> {
  return pipe(AddToContextProducer(additionalContext))
}

fun <IN, OUT> Producer<IN, OUT>.addToContext(vararg additionalContext: Pair<String, Any?>): Producer<IN, OUT> {
  return pipe(AddToContextProducer(*additionalContext))
}
