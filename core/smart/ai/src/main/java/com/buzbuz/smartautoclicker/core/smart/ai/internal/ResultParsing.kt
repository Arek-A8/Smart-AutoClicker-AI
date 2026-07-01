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
package com.buzbuz.smartautoclicker.core.smart.ai.internal

import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.smart.ai.AgentAction
import com.buzbuz.smartautoclicker.core.smart.ai.VisionDetectionResult
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.DetectPayload
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.cloudJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parse a raw model output into a [VisionDetectionResult], scaling pixel coordinates from the sent (downscaled) image
 * space back to the original frame.
 *
 * @param raw the model's textual output (may contain surrounding prose).
 * @param scaleX original_frame_width / sent_image_width.
 * @param scaleY original_frame_height / sent_image_height.
 */
internal fun parseDetection(raw: String, scaleX: Float, scaleY: Float): VisionDetectionResult {
    val json = extractJsonObject(raw, preferKeys = listOf("found", "box")) ?: return VisionDetectionResult.NOT_FOUND
    val payload = runCatching { cloudJson.decodeFromString<DetectPayload>(json) }.getOrNull()
        ?: return VisionDetectionResult.NOT_FOUND

    val box = payload.box?.takeIf { it.size == 4 }?.let { (x, y, w, h) ->
        Rect(
            (x * scaleX).toInt(),
            (y * scaleY).toInt(),
            ((x + w) * scaleX).toInt(),
            ((y + h) * scaleY).toInt(),
        )
    }
    return VisionDetectionResult(
        found = payload.found,
        confidence = payload.confidence.coerceIn(0f, 1f),
        location = box,
    )
}

/**
 * Parse a raw model output into an [AgentAction], scaling pixel coordinates from the sent (downscaled) image space
 * back to the original frame. Unparseable output yields [AgentAction.Fail].
 */
internal fun parseAction(raw: String, scaleX: Float, scaleY: Float): AgentAction {
    val json = extractJsonObject(raw, preferKeys = listOf("action")) ?: return AgentAction.Fail("No JSON in model output")
    val obj = runCatching { cloudJson.decodeFromString<JsonObject>(json) }.getOrNull()
        ?: return AgentAction.Fail("Unparseable action JSON")

    val action = obj.stringField("action")?.lowercase() ?: return AgentAction.Fail("Missing action")
    val reason = obj.stringField("reason")
    val durationMs = obj.numberField("durationMs")?.toLong()

    fun sx(v: Double) = (v * scaleX).toInt()
    fun sy(v: Double) = (v * scaleY).toInt()

    return when (action) {
        "tap" -> {
            val x = obj.numberField("x")
            val y = obj.numberField("y")
            if (x == null || y == null) AgentAction.Fail("tap without coordinates: $json")
            else AgentAction.Tap(sx(x), sy(y))
        }
        "swipe" -> {
            val fromX = obj.numberField("fromX")
            val fromY = obj.numberField("fromY")
            val toX = obj.numberField("toX")
            val toY = obj.numberField("toY")
            if (fromX == null || fromY == null || toX == null || toY == null)
                AgentAction.Fail("swipe without coordinates: $json")
            else AgentAction.Swipe(
                sx(fromX), sy(fromY), sx(toX), sy(toY),
                (durationMs ?: 300L).coerceAtLeast(1L),
            )
        }
        "wait" -> AgentAction.Wait((durationMs ?: 500L).coerceAtLeast(0L))
        "done" -> AgentAction.Done(reason)
        else -> AgentAction.Fail(reason)
    }
}

/** Read a string field, tolerating models that omit it. */
private fun JsonObject.stringField(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

/**
 * Read a numeric field leniently: accepts JSON numbers (int or float) and numeric strings, since vision models
 * frequently emit coordinates as floats ("x": 512.0) or strings ("x": "512"). Returns null if absent or non-numeric.
 */
private fun JsonObject.numberField(key: String): Double? =
    (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
