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

import android.graphics.Bitmap
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.smart.ai.AgentAction
import com.buzbuz.smartautoclicker.core.smart.ai.AgentStep
import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import com.buzbuz.smartautoclicker.core.smart.ai.CloudProtocol
import com.buzbuz.smartautoclicker.core.smart.ai.VisionDetectionResult
import com.buzbuz.smartautoclicker.core.smart.ai.VisionModel
import com.buzbuz.smartautoclicker.core.smart.ai.internal.Prompts
import com.buzbuz.smartautoclicker.core.smart.ai.internal.downscaledTo
import com.buzbuz.smartautoclicker.core.smart.ai.internal.parseAction
import com.buzbuz.smartautoclicker.core.smart.ai.internal.parseDetection
import com.buzbuz.smartautoclicker.core.smart.ai.internal.toJpegBase64
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cloud-backed [VisionModel]. Downscales the frame to [AiConfig.maxImageDimensionPx], tells the model the exact pixel
 * dimensions of the image it is sent, then scales the returned pixel coordinates back to the original frame.
 *
 * @param config endpoint configuration (protocol, base URL, key, model).
 * @param ioDispatcher dispatcher for the blocking HTTP work.
 */
class CloudVisionModel(
    private val config: AiConfig,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VisionModel {

    private val transport: CloudTransport = when (config.protocol) {
        CloudProtocol.GEMINI_NATIVE -> GeminiTransport(config)
        CloudProtocol.OPENAI_COMPATIBLE -> OpenAiTransport(config)
    }

    override suspend fun isAvailable(): Boolean = config.isComplete()

    override suspend fun detect(frame: Bitmap, prompt: String, area: Rect?): VisionDetectionResult =
        withContext(ioDispatcher) {
            val sent = frame.downscaledTo(config.maxImageDimensionPx)
            val (scaleX, scaleY) = sent.scaleBackTo(frame)
            val raw = transport.request(
                systemText = Prompts.detectSystem(sent.width, sent.height),
                userText = Prompts.detectUser(prompt),
                imageBase64Jpeg = sent.toJpegBase64(),
            )
            parseDetection(raw, scaleX, scaleY)
        }

    override suspend fun decideAction(frame: Bitmap, goal: String, history: List<AgentStep>): AgentAction =
        withContext(ioDispatcher) {
            val sent = frame.downscaledTo(config.maxImageDimensionPx)
            val (scaleX, scaleY) = sent.scaleBackTo(frame)
            val raw = transport.request(
                systemText = Prompts.agentSystem(sent.width, sent.height),
                userText = Prompts.agentUser(goal, history),
                imageBase64Jpeg = sent.toJpegBase64(),
            )
            parseAction(raw, scaleX, scaleY)
        }

    /** Scale factors mapping coordinates in [this] (the sent image) back to [original]. */
    private fun Bitmap.scaleBackTo(original: Bitmap): Pair<Float, Float> =
        original.width.toFloat() / width.toFloat() to original.height.toFloat() / height.toFloat()
}
