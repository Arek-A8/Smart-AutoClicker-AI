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
package com.buzbuz.smartautoclicker.core.smart.ai.local

import android.graphics.Bitmap
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.smart.ai.AgentAction
import com.buzbuz.smartautoclicker.core.smart.ai.AgentStep
import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import com.buzbuz.smartautoclicker.core.smart.ai.CloudProtocol
import com.buzbuz.smartautoclicker.core.smart.ai.VisionDetectionResult
import com.buzbuz.smartautoclicker.core.smart.ai.VisionModel
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.CloudVisionModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * On-device [VisionModel] backed by a local llama.cpp server (the GGUF model + mmproj projector validated for
 * gemma-4-e2b). llama-server exposes an OpenAI-compatible /v1/chat/completions endpoint with vision support, so this
 * delegates to a [CloudVisionModel] configured for [CloudProtocol.OPENAI_COMPATIBLE] pointed at the local server.
 *
 * This keeps a single request/parsing code path for both cloud and local. Empirically, image encoding on the S22U CPU
 * is ~145 s/frame, so the local model is suited to occasional "if X appears" conditions rather than a real-time agent
 * loop; lowering [AiConfig.maxImageDimensionPx] via the AI image-resolution setting reduces this cost.
 *
 * @param serverConfig describes how to reach the local llama-server (host/port) and which model id to request.
 * @param ioDispatcher dispatcher for the blocking HTTP work.
 */
class LocalVisionModel(
    serverConfig: LocalServerConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VisionModel {

    private val delegate = CloudVisionModel(
        config = AiConfig(
            protocol = CloudProtocol.OPENAI_COMPATIBLE,
            baseUrl = "http://${serverConfig.host}:${serverConfig.port}/v1",
            apiKey = "local",
            model = serverConfig.modelId,
            requestTimeoutMs = serverConfig.requestTimeoutMs,
            maxImageDimensionPx = serverConfig.maxImageDimensionPx,
        ),
        ioDispatcher = ioDispatcher,
    )

    override suspend fun isAvailable(): Boolean = delegate.isAvailable()

    override suspend fun detect(frame: Bitmap, prompt: String, area: Rect?): VisionDetectionResult =
        delegate.detect(frame, prompt, area)

    override suspend fun decideAction(frame: Bitmap, goal: String, history: List<AgentStep>): AgentAction =
        delegate.decideAction(frame, goal, history)
}

/**
 * Connection details for a running local llama-server multimodal instance.
 *
 * @param host loopback host the server is bound to.
 * @param port server port.
 * @param modelId model id to send in requests (matches the served model).
 * @param requestTimeoutMs per-request timeout; local CPU inference is slow, so this is generous.
 * @param maxImageDimensionPx longest-edge cap for sent frames (driven by the AI image-resolution setting).
 */
data class LocalServerConfig(
    val host: String = "127.0.0.1",
    val port: Int = 8080,
    val modelId: String = "gemma-4-e2b",
    val requestTimeoutMs: Int = 600_000,
    val maxImageDimensionPx: Int = 768,
)
