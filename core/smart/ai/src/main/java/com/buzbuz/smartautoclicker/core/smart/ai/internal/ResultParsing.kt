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
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.ActionPayload
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.DetectPayload
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.cloudJson

/**
 * Parse a raw model output into a [VisionDetectionResult], scaling pixel coordinates from the sent (downscaled) image
 * space back to the original frame.
 *
 * @param raw the model's textual output (may contain surrounding prose).
 * @param scaleX original_frame_width / sent_image_width.
 * @param scaleY original_frame_height / sent_image_height.
 */
internal fun parseDetection(raw: String, scaleX: Float, scaleY: Float): VisionDetectionResult {
    val json = extractFirstJsonObject(raw) ?: return VisionDetectionResult.NOT_FOUND
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
    val json = extractFirstJsonObject(raw) ?: return AgentAction.Fail("No JSON in model output")
    val p = runCatching { cloudJson.decodeFromString<ActionPayload>(json) }.getOrNull()
        ?: return AgentAction.Fail("Unparseable action JSON")

    fun sx(v: Int?) = ((v ?: 0) * scaleX).toInt()
    fun sy(v: Int?) = ((v ?: 0) * scaleY).toInt()

    return when (p.action.lowercase()) {
        "tap" -> AgentAction.Tap(sx(p.x), sy(p.y))
        "swipe" -> AgentAction.Swipe(
            sx(p.fromX), sy(p.fromY), sx(p.toX), sy(p.toY),
            (p.durationMs ?: 300L).coerceAtLeast(1L),
        )
        "wait" -> AgentAction.Wait((p.durationMs ?: 500L).coerceAtLeast(0L))
        "done" -> AgentAction.Done(p.reason)
        else -> AgentAction.Fail(p.reason)
    }
}
