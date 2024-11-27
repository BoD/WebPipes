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

package org.jraf.feeed.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.port
import io.ktor.server.request.uri
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import org.slf4j.event.Level

private const val DEFAULT_PORT = 8042

private const val ENV_PORT = "PORT"

private const val PATH_PRODUCER_ID = "producerId"

class Server(
  private val producers: Map<String, suspend (RequestParams) -> String>,
) {
  fun start() {
    val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
    embeddedServer(Netty, listenPort, module = { mainModule() }).start(wait = true)
  }

  private fun Application.mainModule() {
    install(DefaultHeaders)

    install(CallLogging) {
      level = Level.DEBUG
      format { call ->
        val status = call.response.status()
        val httpMethod = call.request.httpMethod.value
        val host = call.request.origin.remoteHost
        val headers = call.request.headers
        """
          |
          |-----------------------------------
          |Host: $host
          |Method: $httpMethod
          |Headers:
          ${headers.entries().joinToString("\n") { "|- ${it.key}: ${it.value.joinToString()}" }}
          |Status: $status
          |-----------------------------------
          |
        """.trimMargin()
      }
    }

    install(StatusPages) {
      status(HttpStatusCode.NotFound) { call, status ->
        call.respondText(
          text = "Usage: ${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/<a>/<b>\n\nSee https://github.com/BoD/feeed for more info.",
          status = status,
        )
      }
    }

    routing {
      get("{$PATH_PRODUCER_ID}") {
        val producerId = call.parameters[PATH_PRODUCER_ID]!!
        val producer = producers[producerId] ?: run {
          call.response.status(HttpStatusCode.NotFound)
          return@get
        }

        val requestUrl = URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.portStr()}${call.request.uri}")
          .buildString()
        call.respondText(
          producer(
            RequestParams(
              requestUrl = requestUrl,
              queryParams = call.request.queryParameters.toMap().mapValues { it.value.first() },
            ),
          ),
          ContentType.Application.Atom.withCharset(Charsets.UTF_8),
        )
      }
    }
  }
}

data class RequestParams(
  val requestUrl: String,
  val queryParams: Map<String, String>,
)

private fun ApplicationCall.portStr() = request.port().let { if (it == 80) "" else ":$it" }
