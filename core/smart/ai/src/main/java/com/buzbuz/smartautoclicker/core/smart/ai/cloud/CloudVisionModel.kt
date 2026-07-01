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
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
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
            try {
                val sent = frame.downscaledTo(config.maxImageDimensionPx)
                val (scaleX, scaleY) = sent.scaleBackTo(frame)
                Log.i(TAG, "detect(\"$prompt\") sending ${sent.width}x${sent.height} to ${config.backend}")
                val raw = transport.request(
                    systemText = Prompts.detectSystem(sent.width, sent.height),
                    userText = Prompts.detectUser(prompt),
                    imageBase64Jpeg = sent.toJpegBase64(),
                )
                parseDetection(raw, scaleX, scaleY).also { Log.i(TAG, "detect result: $it") }
            } catch (t: Throwable) {
                Log.e(TAG, "detect failed: ${t.message}", t)
                VisionDetectionResult.NOT_FOUND
            }
        }

    /**
     * Send a tiny probe image to verify connectivity and that the endpoint returns usable text. Returns a
     * human-readable result string (never throws) for display in the AI settings "test connection" action.
     */
    suspend fun testConnection(): String = withContext(ioDispatcher) {
        try {
            val probe = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
            val raw = transport.request(
                systemText = Prompts.detectSystem(64, 64),
                userText = Prompts.detectUser("a red square"),
                imageBase64Jpeg = probe.toJpegBase64(),
            )
            "OK - endpoint reachable.\nModel replied:\n${raw.take(300)}"
        } catch (t: Throwable) {
            Log.e(TAG, "testConnection failed", t)
            "FAILED: ${t.message ?: t.javaClass.simpleName}"
        }
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

    private companion object {
        const val TAG = "AiCloudVisionModel"
    }
}
