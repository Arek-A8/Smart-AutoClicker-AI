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
 * Minimal request/response models for an OpenAI-compatible /v1/chat/completions endpoint with vision support.
 * Only the fields actually used by [CloudVisionModel] are modelled.
 */

@Serializable
internal data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int = 512,
    val temperature: Float = 0f,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
)

@Serializable
internal data class ResponseFormat(
    val type: String = "json_object",
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: List<ContentPart>,
)

@Serializable
internal data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
) {
    companion object {
        fun text(value: String): ContentPart = ContentPart(type = "text", text = value)
        fun image(dataUrl: String): ContentPart = ContentPart(type = "image_url", imageUrl = ImageUrl(dataUrl))
    }
}

@Serializable
internal data class ImageUrl(
    val url: String,
)

@Serializable
internal data class ChatResponse(
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
internal data class ChatChoice(
    val message: ChatResponseMessage? = null,
)

@Serializable
internal data class ChatResponseMessage(
    val content: String? = null,
)

/**
 * Structured payload we instruct the model to return for a [VisionModel.detect] call.
 *
 * @param found whether the described target is present.
 * @param confidence model confidence in [0, 1].
 * @param box optional [x, y, width, height] of the target in pixels of the (possibly downscaled) frame.
 */
@Serializable
internal data class DetectPayload(
    val found: Boolean = false,
    val confidence: Float = 0f,
    val box: List<Int>? = null,
)

/**
 * Structured payload we instruct the model to return for a [VisionModel.decideAction] call.
 *
 * @param action one of: tap, swipe, wait, done, fail.
 * @param x / y tap coordinates (action == tap), in pixels of the (possibly downscaled) frame.
 * @param fromX / fromY / toX / toY swipe coordinates (action == swipe).
 * @param durationMs duration for swipe / wait.
 * @param reason optional rationale (used for done / fail and for history notes).
 */
@Serializable
internal data class ActionPayload(
    val action: String = "fail",
    val x: Int? = null,
    val y: Int? = null,
    val fromX: Int? = null,
    val fromY: Int? = null,
    val toX: Int? = null,
    val toY: Int? = null,
    val durationMs: Long? = null,
    val reason: String? = null,
)
