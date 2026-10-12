/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2026-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

@file:OptIn(ExperimentalApi::class)

package org.jraf.webpipes.engine.util

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.NoopOpenTelemetry
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.tracing.SpanCreationAction
import io.opentelemetry.kotlin.tracing.StatusData
import io.opentelemetry.kotlin.tracing.Tracer

var globalOpenTelemetry: OpenTelemetry = NoopOpenTelemetry
  private set

fun setGlobalOpenTelemetry(openTelemetry: OpenTelemetry) {
  globalOpenTelemetry = openTelemetry
}

fun Any.classTracer(): Tracer = globalOpenTelemetry.tracerProvider.getTracer(javaClass.name)

inline fun <T> Tracer.withSpan(
  name: String,
  noinline configureSpan: (SpanCreationAction.() -> Unit)? = null,
  block: () -> T,
): T {
  val span = startSpan(name, action = configureSpan)
  val scope = globalOpenTelemetry.context.implicit().storeSpan(span).attach()

  var throwable: Throwable? = null
  try {
    return block()
  } catch (t: Throwable) {
    throwable = t
    throw t
  } finally {
    if (throwable != null) {
      span.setStatus(StatusData.Error(throwable.message))
    } else {
      span.setStatus(StatusData.Ok)
    }
    scope.detach()
    span.end()
  }
}
