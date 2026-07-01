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
 * Supplies the [VisionModel] to use for AI conditions/actions, built from the current [AiConfig].
 *
 * Implementations resolve the user's configured backend (cloud Gemini/OpenAI-compatible or local llama-server) and
 * return a ready [VisionModel], or null when AI is not configured. Kept as an interface so the processing engine can
 * depend on it without knowing about configuration storage.
 */
interface VisionModelProvider {
    /** @return the configured [VisionModel], or null if AI is not configured/available. */
    fun getVisionModel(): VisionModel?

    /**
     * Probe the given [config] end-to-end (sends a tiny image) and return a human-readable pass/fail message.
     * Never throws. Used by the AI settings "test connection" action.
     */
    suspend fun testConnection(config: AiConfig): String
}
