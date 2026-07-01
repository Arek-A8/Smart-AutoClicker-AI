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

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Abstraction over a vision-capable model used to add AI behaviour to the detection engine.
 *
 * Two distinct capabilities are exposed:
 *  - [detect]: perception. Answer whether a natural-language described target is present on a frame, optionally
 *    returning its location. This backs the AI condition type ("if AI sees X, do Y").
 *  - [decideAction]: actuation planning. Given a goal and the current frame, decide the next gesture to perform.
 *    This backs the goal-driven agent.
 *
 * Implementations may be backed by a remote service ([com.buzbuz.smartautoclicker.core.smart.ai.cloud.CloudVisionModel])
 * or an on-device model ([com.buzbuz.smartautoclicker.core.smart.ai.local.LocalVisionModel]). All calls are suspending
 * and may be slow (network / inference); callers must drive them from a background dispatcher and account for latency
 * in their processing loop.
 */
interface VisionModel {

    /** True if the model is configured and ready to serve requests. */
    suspend fun isAvailable(): Boolean

    /**
     * Detect whether the [prompt]-described target is present in [frame].
     *
     * @param frame the current screen content.
     * @param prompt natural-language description of what to look for, e.g. "the green Start button".
     * @param area optional region of [frame] to restrict the search to. Null searches the whole frame.
     *
     * @return a [VisionDetectionResult] describing whether the target was found, the confidence, and its location.
     */
    suspend fun detect(frame: Bitmap, prompt: String, area: Rect? = null): VisionDetectionResult

    /**
     * Decide the next [AgentAction] to perform toward [goal] based on the current [frame].
     *
     * @param frame the current screen content.
     * @param goal natural-language description of the objective, e.g. "collect the daily reward and close popups".
     * @param history the ordered list of steps already performed this session, most recent last. Allows the model to
     * avoid loops and understand progress.
     *
     * @return the next [AgentAction] to perform. [AgentAction.Done] / [AgentAction.Fail] end the session.
     */
    suspend fun decideAction(frame: Bitmap, goal: String, history: List<AgentStep>): AgentAction
}

/**
 * Result of a [VisionModel.detect] call.
 *
 * @param found whether the described target was detected.
 * @param confidence model confidence in [0f, 1f].
 * @param location bounding box of the target in frame coordinates, or null if not found / not localizable.
 */
data class VisionDetectionResult(
    val found: Boolean,
    val confidence: Float,
    val location: Rect? = null,
) {
    companion object {
        val NOT_FOUND: VisionDetectionResult = VisionDetectionResult(found = false, confidence = 0f, location = null)
    }
}

/**
 * A single step performed by the agent, used as history context for subsequent [VisionModel.decideAction] calls.
 *
 * @param action the action that was decided.
 * @param note optional model-provided rationale for the action.
 */
data class AgentStep(
    val action: AgentAction,
    val note: String? = null,
)

/**
 * An action the agent can perform. Spatial actions are expressed in frame coordinates; the caller is responsible for
 * translating them into a platform gesture.
 */
sealed interface AgentAction {
    /** Tap at ([x], [y]). */
    data class Tap(val x: Int, val y: Int) : AgentAction
    /** Swipe from ([fromX], [fromY]) to ([toX], [toY]) over [durationMs]. */
    data class Swipe(
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int,
        val durationMs: Long,
    ) : AgentAction
    /** Wait [durationMs] before capturing the next frame (e.g. waiting for an animation). */
    data class Wait(val durationMs: Long) : AgentAction
    /** The goal has been achieved; the agent session should stop successfully. */
    data class Done(val reason: String? = null) : AgentAction
    /** The goal cannot be achieved; the agent session should stop with a failure. */
    data class Fail(val reason: String? = null) : AgentAction
}
