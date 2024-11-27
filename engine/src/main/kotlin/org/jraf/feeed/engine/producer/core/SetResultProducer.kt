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

class SetResultProducer<IN, OUT : Any> : Producer<IN, OUT> {
  override suspend fun produce(context: ProducerContext, input: IN): Result<ProducerOutput<OUT>> {
    val newInput = context.get<Any>("result", "result") as OUT
    return Result.success(context to newInput)
  }

  override fun close() {}
}

fun <IN, OUT, RESULT : Any> Producer<IN, OUT>.setResult(result: RESULT?): Producer<IN, RESULT> {
  return addToContextIfNotNull("result", result)
    .pipe(SetResultProducer())
}
