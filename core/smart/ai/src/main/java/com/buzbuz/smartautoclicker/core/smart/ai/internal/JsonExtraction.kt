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

/**
 * Extract the first balanced top-level JSON object from [raw] model output.
 *
 * Vision models frequently wrap the requested JSON in prose or a chain-of-thought block despite instructions to the
 * contrary (observed with Gemma e2b's "<|channel>thought" preamble). This scans for the first '{' and returns the
 * substring up to its matching '}', accounting for braces inside strings. Returns null if no balanced object is found.
 */
internal fun extractFirstJsonObject(raw: String): String? {
    val start = raw.indexOf('{')
    if (start < 0) return null

    var depth = 0
    var inString = false
    var escaped = false
    for (i in start until raw.length) {
        val c = raw[i]
        when {
            escaped -> escaped = false
            c == '\\' && inString -> escaped = true
            c == '"' -> inString = !inString
            !inString && c == '{' -> depth++
            !inString && c == '}' -> {
                depth--
                if (depth == 0) return raw.substring(start, i + 1)
            }
        }
    }
    return null
}
