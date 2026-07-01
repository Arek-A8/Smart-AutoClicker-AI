/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.smart.ai.cloud

import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import kotlinx.serialization.encodeToString

/**
 * Talks to an OpenAI-compatible chat completions API:
 * POST {baseUrl}/chat/completions  with  Authorization: Bearer {apiKey}
 *
 * The image is sent as a data-URL image_url content part.
 */
internal class OpenAiTransport(private val config: AiConfig) : CloudTransport {

    override fun request(systemText: String, userText: String, imageBase64Jpeg: String): String {
        val dataUrl = "data:image/jpeg;base64,$imageBase64Jpeg"
        val body = ChatRequest(
            model = config.model,
            messages = listOf(
                ChatMessage(role = "system", content = listOf(ContentPart.text(systemText))),
                ChatMessage(
                    role = "user",
                    content = listOf(ContentPart.text(userText), ContentPart.image(dataUrl)),
                ),
            ),
            responseFormat = ResponseFormat(type = "json_object"),
        )

        val base = config.baseUrl.trimEnd('/')
        val result = httpPostJson(
            url = "$base/chat/completions",
            jsonBody = cloudJson.encodeToString(body),
            headers = mapOf("Authorization" to "Bearer ${config.apiKey}"),
            timeoutMs = config.requestTimeoutMs,
        )
        if (!result.isSuccess) {
            throw CloudRequestException("OpenAI-compatible request failed with HTTP ${result.code}")
        }
        val parsed = cloudJson.decodeFromString<ChatResponse>(result.body)
        return parsed.choices.firstOrNull()?.message?.content
            ?: throw CloudRequestException("OpenAI-compatible response contained no content")
    }
}
