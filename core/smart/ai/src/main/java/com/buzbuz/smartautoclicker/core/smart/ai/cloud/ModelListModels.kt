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
 * Response of an OpenAI-compatible `GET /v1/models` call. Used by LM Studio, llama.cpp server and Ollama's OpenAI
 * shim. Only the model id is needed for the picker; other fields are ignored (lenient JSON).
 */
@Serializable
internal data class OpenAiModelsResponse(
    val data: List<OpenAiModelEntry> = emptyList(),
)

@Serializable
internal data class OpenAiModelEntry(
    val id: String,
)

/**
 * Response of Google's Generative Language `GET /v1beta/models` (ListModels). The model name comes back as
 * "models/{id}"; the picker strips the "models/" prefix. Only vision-capable chat models are useful, but filtering is
 * left to the caller/UI.
 */
@Serializable
internal data class GeminiModelsResponse(
    val models: List<GeminiModelEntry> = emptyList(),
)

@Serializable
internal data class GeminiModelEntry(
    val name: String,
    @SerialName("supportedGenerationMethods")
    val supportedGenerationMethods: List<String> = emptyList(),
)
