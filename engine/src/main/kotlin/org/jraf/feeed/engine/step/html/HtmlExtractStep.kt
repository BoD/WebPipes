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

package org.jraf.feeed.engine.step.html

//import org.jraf.feeed.api.step.Context
//import org.jraf.feeed.api.Step
//import org.jraf.feeed.engine.producer.core.StepChain
//import org.jraf.feeed.engine.producer.core.addToContextIfNotNull
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import us.codecraft.xsoup.Xsoup
//
//class HtmlExtractStep : Step {
//  override suspend fun execute(context: Context): Result<Context> {
//    return runCatching {
//      val baseUrl: String = context["baseUrl"]
//      val xPath: String = context["xPath"]
//      val xPathEvaluator = Xsoup.compile(xPath)
//      val text: String = context["text"]
//      val variableName: String = context["variableName", "text"]
//
//      val document: Document = Jsoup.parse(text, baseUrl)
//      val extracted = xPathEvaluator.evaluate(document).elements.first()!!.html()
//      context.with(variableName, extracted)
//    }
//  }
//}
//
//fun StepChain.htmlExtract(
//  text: String? = null,
//  baseUrl: String? = null,
//  xPath: String? = null,
//  variableName: String? = null,
//): StepChain {
//  return addToContextIfNotNull("text", text)
//    .addToContextIfNotNull("baseUrl", baseUrl)
//    .addToContextIfNotNull("xPath", xPath)
//    .addToContextIfNotNull("variableName", variableName) +
//    HtmlExtractStep()
//}
