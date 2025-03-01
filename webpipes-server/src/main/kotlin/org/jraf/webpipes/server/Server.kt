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

package org.jraf.webpipes.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.withCharset
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.port
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.toMap
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.slf4j.event.Level

private const val DEFAULT_PORT = 8042

private const val ENV_PORT = "PORT"

private const val PATH_RECIPE_ID = "recipeId"

private const val PATH_STEP_ID = "stepId"

class Server(
  private val recipeExecutor: suspend (requestParams: RequestParams) -> Pair<String, String>?,
  private val stepExecutor: suspend (stepType: String, context: JsonObject) -> JsonObject?,
) {
  fun start() {
    val listenPort = System.getenv(ENV_PORT)?.toInt() ?: DEFAULT_PORT
    embeddedServer(
      factory = Netty,
      configure = {
        connectors.add(
          EngineConnectorBuilder().apply {
            port = listenPort
          },
        )
        // Needed so the server can call itself recursively multiple times
        callGroupSize = 256
      },
      module = { mainModule() },
    ).start(wait = true)
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
          text = """
            Usage:
            ${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/step/<step id>
            or
            ${call.request.local.scheme}://${call.request.local.serverHost}:${call.request.local.serverPort}/recipe/<recipe URL>

            See https://github.com/BoD/WebPipes for more info.
          """.trimIndent(),
          status = status,
        )
      }
    }

    install(ContentNegotiation) {
      json(
        Json {
          prettyPrint = true
        },
      )
    }

    routing {
      get("/recipe/{$PATH_RECIPE_ID}") {
        val recipeId = call.parameters[PATH_RECIPE_ID] ?: run {
          call.response.status(HttpStatusCode.NotFound)
          return@get
        }
        val requestUrl = URLBuilder("${call.request.origin.scheme}://${call.request.host()}${call.portStr()}${call.request.uri}")
          .buildString()
        val (body, contentType) = recipeExecutor(
          RequestParams(
            recipeId = recipeId,
            requestUrl = requestUrl,
            queryParams = call.request.queryParameters.toMap().mapValues { it.value.first() },
          ),
        ) ?: run {
          call.response.status(HttpStatusCode.NotFound)
          return@get
        }
        val (type, subtype) = contentType.split("/")
        call.respondText(
          body,
          ContentType(contentType = type, contentSubtype = subtype).withCharset(Charsets.UTF_8),
        )
      }

      post("/step/{$PATH_STEP_ID}") {
        val stepType = call.parameters[PATH_STEP_ID] ?: run {
          call.response.status(HttpStatusCode.NotFound)
          return@post
        }
        val jsonIn = call.receive<JsonObject>()
        val result = stepExecutor(stepType, jsonIn)
        if (result == null) {
          call.response.status(HttpStatusCode.NotFound)
        } else {
          call.respond(result)
        }
      }

      staticResources("/static", "recipes")
    }
  }
}

data class RequestParams(
  val recipeId: String,
  val requestUrl: String,
  val queryParams: Map<String, String>,
)

private fun ApplicationCall.portStr() = request.port().let { if (it == 80) "" else ":$it" }
