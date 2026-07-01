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

/**
 * A protocol-specific cloud chat transport. Given an instruction, a user message and one JPEG image (base64), it
 * performs the request and returns the model's raw text output (from which the caller extracts JSON).
 */
internal interface CloudTransport {
    fun request(systemText: String, userText: String, imageBase64Jpeg: String): String
}

/** Raised when a cloud request fails or returns an unusable response. Message must never include the API key. */
internal class CloudRequestException(message: String) : Exception(message)
