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

import android.util.Log

import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import kotlinx.serialization.encodeToString

/**
 * Talks to an OpenAI-compatible chat completions API:
 * POST {baseUrl}/chat/completions  with  Authorization: Bearer {apiKey}
 *
 * The image is sent as a data-URL image_url content part. Also used for the local llama.cpp server, whose endpoint is
 * OpenAI-compatible and ignores the requested model name (uses whatever model is loaded).
 */
internal class OpenAiTransport(private val config: AiConfig) : CloudTransport {

    override fun request(systemText: String, userText: String, imageBase64Jpeg: String): String {
        val dataUrl = "data:image/jpeg;base64,$imageBase64Jpeg"
        val body = ChatRequest(
            // Local llama-server ignores this; some servers reject an empty string, so default it.
            model = config.model.ifBlank { "local-model" },
            messages = listOf(
                ChatMessage(role = "system", content = listOf(ContentPart.text(systemText))),
                ChatMessage(
                    role = "user",
                    content = listOf(ContentPart.text(userText), ContentPart.image(dataUrl)),
                ),
            ),
            // Thinking models (Gemma 4) spend tokens reasoning before the JSON; give generous headroom.
            maxTokens = 1024,
            // json_object mode is not universally supported (and can conflict with thinking); omit it.
            responseFormat = null,
        )

        val base = config.baseUrl.trimEnd('/')
        val url = "$base/chat/completions"
        Log.i(TAG, "POST $url (model=${body.model}, imageBytes~${imageBase64Jpeg.length})")

        val headers = if (config.apiKey.isNotBlank()) {
            mapOf("Authorization" to "Bearer ${config.apiKey}")
        } else {
            emptyMap()
        }
        val result = httpPostJson(
            url = url,
            jsonBody = cloudJson.encodeToString(body),
            headers = headers,
            timeoutMs = config.requestTimeoutMs,
        )
        if (!result.isSuccess) {
            Log.e(TAG, "HTTP ${result.code}: ${result.body.take(300)}")
            throw CloudRequestException("Request failed: HTTP ${result.code} ${result.body.take(200)}")
        }

        val parsed = cloudJson.decodeFromString<ChatResponse>(result.body)
        val text = parsed.choices.firstOrNull()?.message?.effectiveText()
        if (text.isNullOrBlank()) {
            Log.e(TAG, "Empty response content. Raw: ${result.body.take(300)}")
            throw CloudRequestException("Response contained no usable text")
        }
        Log.i(TAG, "Response text (${text.length} chars): ${text.take(200)}")
        return text
    }

    private companion object {
        const val TAG = "AiOpenAiTransport"
    }
}
