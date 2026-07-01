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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal request/response models for Google's Generative Language native API:
 * POST {baseUrl}/v1beta/models/{model}:generateContent?key={apiKey}
 * Only the fields used by [CloudVisionModel] are modelled.
 */

@Serializable
internal data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null,
)

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Float = 0f,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 512,
    @SerialName("responseMimeType") val responseMimeType: String = "application/json",
)

@Serializable
internal data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>,
)

@Serializable
internal data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null,
) {
    companion object {
        fun text(value: String): GeminiPart = GeminiPart(text = value)
        fun image(mimeType: String, base64Data: String): GeminiPart =
            GeminiPart(inlineData = GeminiInlineData(mimeType = mimeType, data = base64Data))
    }
}

@Serializable
internal data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String,
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent? = null,
)
