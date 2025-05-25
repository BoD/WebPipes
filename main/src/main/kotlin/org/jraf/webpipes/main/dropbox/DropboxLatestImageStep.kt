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

package org.jraf.webpipes.main.dropbox

import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.TokenAccessType
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.sharing.ListSharedLinksResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jraf.webpipes.api.Step
import org.jraf.webpipes.engine.util.plus
import org.jraf.webpipes.engine.util.string
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Date

class DropboxLatestImageStep : Step {
  override suspend fun execute(context: JsonObject): JsonObject {
    val appKey: String = context.string("appKey")
    val appSecret: String = context.string("appSecret")
    val refreshToken: String = context.string("refreshToken")
    val url = getDropboxLatestImageUrl(
      appKey = appKey,
      appSecret = appSecret,
      refreshToken = refreshToken,
    )

    val resultJson = buildJsonObject {
      put("url", url)
    }

    return context + ("text" to resultJson.toString())
  }

  private fun getAccessToken(
    appKey: String,
    appSecret: String,
    refreshToken: String,
  ): String {
    return DbxCredential(
      "",
      0,
      refreshToken,
      appKey,
      appSecret,
    ).refresh(DbxRequestConfig("jraf.org/webpipes"))
      .accessToken
  }

  private fun getDropboxLatestImageUrl(
    appKey: String,
    appSecret: String,
    refreshToken: String,
  ): String {
    val accessToken = getAccessToken(
      appKey = appKey,
      appSecret = appSecret,
      refreshToken = refreshToken,
    )
    val client = DbxClientV2(DbxRequestConfig("jraf.org/webpipes"), accessToken)
    var listFolderResult = client.files().listFolder("/trmnl")
    val sharedLinksWithDates = mutableMapOf<String, Date>()
    while (true) {
      for (metadata in listFolderResult.getEntries()) {
        val listSharedLinksResult: ListSharedLinksResult = client.sharing().listSharedLinksBuilder()
          .withPath(metadata.pathLower)
          .withDirectOnly(true)
          .start()
        val fileDate = (metadata as FileMetadata).serverModified
        var sharedLink = listSharedLinksResult.getLinks().firstOrNull()?.url
        if (sharedLink == null) {
          sharedLink = client.sharing().createSharedLinkWithSettings(metadata.pathLower).url
        }
        sharedLinksWithDates[sharedLink] = fileDate
      }

      // Handle pagination
      if (!listFolderResult.hasMore) {
        break
      }
      listFolderResult = client.files().listFolderContinue(listFolderResult.cursor)
    }
    return (sharedLinksWithDates
      .maxByOrNull { it.value }
      ?: throw Exception("No files found in Dropbox folder"))
      .key
      .replace("dl=0", "dl=1")
  }
}

private fun getRefreshToken(appKey: String, appSecret: String): String {
  val appInfo = DbxAppInfo(appKey, appSecret)
  val requestConfig = DbxRequestConfig("jraf.org/webpipes")
  val webAuth = DbxWebAuth(requestConfig, appInfo)
  val webAuthRequest = DbxWebAuth.newRequestBuilder()
    .withNoRedirect()
    .withTokenAccessType(TokenAccessType.OFFLINE)
    .build()
  val authorizeUrl = webAuth.authorize(webAuthRequest)
  println("1. Go to $authorizeUrl")
  println("2. Click \"Allow\" (you might have to log in first).")
  println("3. Copy the authorization code.")
  print("Enter the authorization code here: ")
  val code = BufferedReader(InputStreamReader(System.`in`)).readLine().trim()
  return webAuth.finishFromCode(code).refreshToken
}

//fun main() {
//  val refreshToken = getRefreshToken(
//    appKey = "xxx",
//    appSecret = "xxx",
//  )
//  println("Refresh token: $refreshToken")
//}
