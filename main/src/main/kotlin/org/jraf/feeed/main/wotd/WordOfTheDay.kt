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

package org.jraf.feeed.main.wotd

import org.jraf.feeed.api.producer.Producer
import org.jraf.feeed.api.producer.ProducerContext
import org.jraf.feeed.api.producer.context
import org.jraf.feeed.api.producer.value
import org.jraf.feeed.engine.producer.core.IdentityProducer
import org.jraf.feeed.engine.producer.core.cache
import org.jraf.feeed.engine.producer.net.urlText
import org.jraf.feeed.server.RequestParams

private val createSessionUrl = "https://www.merriam-webster.com/word-of-the-day"

private val producer: Producer<String, String> =
  IdentityProducer<String>()
    .urlText()
    .cache()

private var context = ProducerContext()

suspend fun produceWordOfTheDay(requestParams: RequestParams): String {
  val output = producer
    .produce(
      context,
      createSessionUrl,
    )
    .getOrThrow()
  context = output.context
  return output.value.toString()
}
