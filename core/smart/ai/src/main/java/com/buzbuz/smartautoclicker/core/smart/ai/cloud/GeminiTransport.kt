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
 * Talks to Google's Generative Language native API:
 * POST {baseUrl}/v1beta/models/{model}:generateContent?key={apiKey}
 *
 * The API key is passed as a query parameter per Google's spec; it is never logged.
 */
internal class GeminiTransport(private val config: AiConfig) : CloudTransport {

    override fun request(systemText: String, userText: String, imageBase64Jpeg: String): String {
        // Gemini has no dedicated system role here; prepend the system instruction to the user text.
        val combined = "$systemText\n\n$userText"
        val body = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart.text(combined),
                        GeminiPart.image("image/jpeg", imageBase64Jpeg),
                    ),
                ),
            ),
            generationConfig = GeminiGenerationConfig(),
        )

        val base = config.baseUrl.trimEnd('/')
        val url = "$base/v1beta/models/${config.model}:generateContent?key=${config.apiKey}"
        val result = httpPostJson(
            url = url,
            jsonBody = cloudJson.encodeToString(body),
            headers = emptyMap(),
            timeoutMs = config.requestTimeoutMs,
        )
        if (!result.isSuccess) {
            throw CloudRequestException("Gemini request failed with HTTP ${result.code}")
        }
        val parsed = cloudJson.decodeFromString<GeminiResponse>(result.body)
        return parsed.candidates.firstOrNull()?.content?.parts
            ?.firstOrNull { it.text != null }?.text
            ?: throw CloudRequestException("Gemini response contained no text")
    }
}
