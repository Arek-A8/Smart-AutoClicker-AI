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

import com.buzbuz.smartautoclicker.core.smart.ai.AgentStep

/**
 * Shared prompt construction for all [com.buzbuz.smartautoclicker.core.smart.ai.VisionModel] implementations.
 *
 * Coordinate contract (established empirically against Gemma e2b and consistent across tested models): the model has
 * no fixed innate coordinate grid — it reports coordinates in whatever pixel space the prompt declares. Therefore we
 * always tell the model the exact pixel [width] x [height] of the image we send, and require it to answer in that
 * same pixel space. The caller is then responsible for scaling those pixels back to the original frame size.
 */
internal object Prompts {

    /** Instruction shared by all calls: no prose, no reasoning, JSON only. */
    private const val OUTPUT_DISCIPLINE =
        "Do not explain. Do not output reasoning or any text other than the single JSON object requested."

    fun detectSystem(width: Int, height: Int): String =
        "You are a precise UI grounding model inspecting a screenshot that is exactly $width pixels wide and " +
        "$height pixels tall. The origin (0,0) is the top-left corner; x increases rightward to $width, y increases " +
        "downward to $height. All coordinates you output must be integer pixels in this space. $OUTPUT_DISCIPLINE"

    fun detectUser(target: String): String =
        "Determine whether the following target is visible in the screenshot: \"$target\". " +
        "Respond with ONLY this JSON object: " +
        "{\"found\": <true|false>, \"confidence\": <0.0-1.0>, \"box\": [<x>, <y>, <width>, <height>]}. " +
        "The box is the target's bounding rectangle in pixels; use null for box if not found."

    fun agentSystem(width: Int, height: Int): String =
        "You are an autonomous agent controlling an Android device by looking at screenshots and choosing one input " +
        "action at a time. The screenshot is exactly $width pixels wide and $height pixels tall, origin top-left, x to " +
        "$width, y to $height. All coordinates must be integer pixels in this space. $OUTPUT_DISCIPLINE"

    fun agentUser(goal: String, history: List<AgentStep>): String = buildString {
        append("Goal: \"").append(goal).append("\".\n")
        if (history.isNotEmpty()) {
            append("Actions already performed (oldest first):\n")
            history.forEachIndexed { index, step ->
                append(index + 1).append(". ").append(step.action::class.simpleName)
                step.note?.let { append(" — ").append(it) }
                append('\n')
            }
        }
        append(
            "Decide the single next action that best progresses toward the goal. " +
            "Respond with ONLY one JSON object of one of these shapes:\n" +
            "{\"action\":\"tap\",\"x\":<int>,\"y\":<int>,\"reason\":<string>}\n" +
            "{\"action\":\"swipe\",\"fromX\":<int>,\"fromY\":<int>,\"toX\":<int>,\"toY\":<int>,\"durationMs\":<int>,\"reason\":<string>}\n" +
            "{\"action\":\"wait\",\"durationMs\":<int>,\"reason\":<string>}\n" +
            "{\"action\":\"done\",\"reason\":<string>}\n" +
            "{\"action\":\"fail\",\"reason\":<string>}"
        )
    }
}
