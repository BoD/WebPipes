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

package org.jraf.feeed.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.event.Level

private const val DEFAULT_PORT = 8080

private const val ENV_PORT = "PORT"

private const val PATH_TOKEN = "token"
private const val PATH_GITHUB_USER_NAME = "username"

class Server {
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
          text = "Usage: ${call.request.local.scheme}://${call.request.local.host}:${call.request.local.port}/<Auth token>/<GitHub user name>\n\nSee https://github.com/BoD/github-to-bookmarks for more info.",
          status = status
        )
      }
    }

    routing {
      get("{$PATH_TOKEN}/{$PATH_GITHUB_USER_NAME}") {
        val token = call.parameters[PATH_TOKEN]!!
        val userName = call.parameters[PATH_GITHUB_USER_NAME]!!
        call.respondText("Hello, World!", ContentType.Application.Json.withCharset(Charsets.UTF_8))
      }
    }
  }
}
