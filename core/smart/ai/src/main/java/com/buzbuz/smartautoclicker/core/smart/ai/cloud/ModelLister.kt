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
import com.buzbuz.smartautoclicker.core.smart.ai.CloudProtocol
import com.buzbuz.smartautoclicker.core.smart.ai.ModelListResult

/**
 * Enumerates the model ids a configured server exposes, so the user can pick one instead of typing it.
 *
 * - [CloudProtocol.OPENAI_COMPATIBLE]: GET {baseUrl}/models  (baseUrl already ends in /v1 for LM Studio/llama.cpp).
 * - [CloudProtocol.GEMINI_NATIVE]: GET {baseUrl}/v1beta/models?key={apiKey}, keeping only entries that support
 *   generateContent, with the "models/" prefix stripped.
 *
 * Blocking; call from an IO dispatcher. Never throws for HTTP/parse errors — returns a [ModelListResult] describing
 * the outcome. The API key is never included in the returned message.
 */
internal object ModelLister {

    private const val TAG = "AiModelLister"

    fun listModels(config: AiConfig): ModelListResult {
        val base = config.baseUrl.trimEnd('/')
        if (base.isBlank()) return ModelListResult.Failure("Base URL is empty")

        return try {
            when (config.protocol) {
                CloudProtocol.OPENAI_COMPATIBLE -> listOpenAi(base, config)
                CloudProtocol.GEMINI_NATIVE -> listGemini(base, config)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "listModels failed", t)
            ModelListResult.Failure(t.message ?: t.javaClass.simpleName)
        }
    }

    private fun listOpenAi(base: String, config: AiConfig): ModelListResult {
        val url = "$base/models"
        val headers = if (config.apiKey.isNotBlank()) mapOf("Authorization" to "Bearer ${config.apiKey}") else emptyMap()
        Log.i(TAG, "GET $url")
        val result = httpGet(url, headers, config.requestTimeoutMs)
        if (!result.isSuccess) {
            return ModelListResult.Failure("HTTP ${result.code} ${result.body.take(200)}")
        }
        val parsed = cloudJson.decodeFromString<OpenAiModelsResponse>(result.body)
        val ids = parsed.data.map { it.id }.filter { it.isNotBlank() }.distinct().sorted()
        return if (ids.isEmpty()) ModelListResult.Failure("Server returned no models") else ModelListResult.Success(ids)
    }

    private fun listGemini(base: String, config: AiConfig): ModelListResult {
        if (config.apiKey.isBlank()) return ModelListResult.Failure("API key required to list Gemini models")
        val url = "$base/v1beta/models?key=${config.apiKey}"
        // Do not log the URL: it carries the API key as a query parameter.
        Log.i(TAG, "GET {baseUrl}/v1beta/models (key redacted)")
        val result = httpGet(url, emptyMap(), config.requestTimeoutMs)
        if (!result.isSuccess) {
            return ModelListResult.Failure("HTTP ${result.code} ${result.body.take(200)}")
        }
        val parsed = cloudJson.decodeFromString<GeminiModelsResponse>(result.body)
        val ids = parsed.models
            .filter { it.supportedGenerationMethods.isEmpty() || it.supportedGenerationMethods.contains("generateContent") }
            .map { it.name.removePrefix("models/") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        return if (ids.isEmpty()) ModelListResult.Failure("No generateContent models found") else ModelListResult.Success(ids)
    }
}
