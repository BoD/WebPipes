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

package org.jraf.webpipes.main.astronomyquizoftheday

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatModel
import com.openai.models.responses.ResponseCreateParams
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.classLogger
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import java.time.LocalDate

class AstronomyQuizOfTheDayStep : Step {
  private val logger = classLogger()

  override suspend fun execute(context: JsonObject): JsonObject {
    val openAiApiKey: String = context.string("openAiApiKey")
    val quiz = generateQuiz(openAiApiKey = openAiApiKey)
    return context + ("text" to buildJsonObject {
      put("question", quiz.question)
      put("answer", quiz.answer)
    }.toString())
  }

  private fun generateQuiz(openAiApiKey: String): AstronomyQuizOfTheDay {
    logger.debug("Generating quiz")

    val openAIClient = OpenAIOkHttpClient.builder()
      .apiKey(openAiApiKey)
      .build()

    val createQuizResponseCreateParams = ResponseCreateParams.builder()
      .model(ChatModel.GPT_5_6_LUNA)
      .text(AstronomyQuizOfTheDay::class.java)
      .input("Today is ${LocalDate.now()}, create an Astronomy question of the day")
      .build()
    val createQuizResponse = openAIClient.responses().create(createQuizResponseCreateParams)
    val quiz = createQuizResponse.output()
      .filter { it.message().isPresent }
      .flatMap { it.message().get().content() }
      .map { it.outputText().get() }
      .first()
    logger.debug("Generated quiz: {}", quiz)
    return quiz
  }
}
