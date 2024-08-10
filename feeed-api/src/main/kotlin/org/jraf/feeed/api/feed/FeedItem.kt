/*
 * This producer is part of the
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

package org.jraf.feeed.api.feed

import java.time.Instant

data class FeedItem(
  val title: String,
  val link: String,
  val date: Instant,
  val body: String,
  val extras: Map<String, String> = emptyMap(),
) {
  sealed interface Field<T> {
    data object Title : Field<String>
    data object Link : Field<String>
    data object Date : Field<Instant>
    data object Body : Field<String>
    data class Extra(val name: String) : Field<Any?>
  }

  operator fun <T> get(field: Field<T>): T {
    @Suppress("UNCHECKED_CAST")
    return when (field) {
      Field.Title -> title
      Field.Link -> link
      Field.Date -> date
      Field.Body -> body
      is Field.Extra -> extras[field.name]
    } as T
  }

  fun <T> with(field: Field<T>, value: T): FeedItem {
    return when (field) {
      Field.Title -> copy(title = value as String)
      Field.Link -> copy(link = value as String)
      Field.Date -> copy(date = value as Instant)
      Field.Body -> copy(body = value as String)
      is Field.Extra -> copy(extras = extras + (field.name to value as String))
    }
  }
}
