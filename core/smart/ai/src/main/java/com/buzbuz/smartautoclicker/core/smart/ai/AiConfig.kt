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
package com.buzbuz.smartautoclicker.core.smart.ai

/**
 * Which [VisionModel] implementation to use.
 */
enum class VisionBackend {
    /** Remote cloud vision endpoint (see [CloudProtocol]). */
    CLOUD,
    /** On-device model (llama.cpp / GGUF). */
    LOCAL,
}

/**
 * Wire protocol for the cloud backend.
 */
enum class CloudProtocol {
    /** Google Generative Language native API (models/{model}:generateContent). */
    GEMINI_NATIVE,
    /** OpenAI-compatible /chat/completions API (also covers Google's OpenAI compatibility shim). */
    OPENAI_COMPATIBLE,
}

/**
 * Configuration for the AI vision layer.
 *
 * @param backend which implementation to route calls to.
 * @param protocol wire protocol for the cloud backend (ignored for [VisionBackend.LOCAL]).
 * @param baseUrl base URL of the cloud endpoint (CLOUD only). For Gemini native, e.g.
 * "https://generativelanguage.googleapis.com"; for OpenAI-compatible, e.g. "https://api.example.com/v1".
 * @param apiKey credential for the endpoint (CLOUD only). Never log or serialize this value. Populated at runtime
 * from a user-entered settings field; never hard-coded.
 * @param model model identifier sent in the request (CLOUD only), e.g. "gemini-3.1-flash-lite".
 * @param requestTimeoutMs per-request timeout in milliseconds.
 * @param maxImageDimensionPx frames larger than this on their longest edge are downscaled before being sent, to bound
 * latency and token cost. This is the value driven by the dedicated "AI image resolution" setting and is also the
 * pixel space the model is told to report coordinates in.
 */
data class AiConfig(
    val backend: VisionBackend = VisionBackend.CLOUD,
    val protocol: CloudProtocol = CloudProtocol.GEMINI_NATIVE,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val requestTimeoutMs: Int = 30_000,
    val maxImageDimensionPx: Int = 1280,
) {
    /** True if the config has the minimum fields required for the selected [backend]. */
    fun isComplete(): Boolean = when (backend) {
        VisionBackend.CLOUD -> baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
        VisionBackend.LOCAL -> model.isNotBlank()
    }
}
