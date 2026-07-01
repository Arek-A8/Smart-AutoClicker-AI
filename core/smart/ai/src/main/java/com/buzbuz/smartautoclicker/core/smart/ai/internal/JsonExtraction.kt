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
 * Extract the JSON answer object from [raw] model output.
 *
 * Vision models frequently wrap the requested JSON in prose or a chain-of-thought block despite instructions to the
 * contrary (observed with Gemma e2b's "<|channel>thought" preamble). Crucially, that reasoning text often echoes the
 * requested JSON *schema* (e.g. `{"action":"tap","x":<int>,...}`) BEFORE the real answer, so returning the first
 * balanced object would return the schema echo (no real coordinates) rather than the answer.
 *
 * This scans all balanced top-level objects (accounting for braces inside strings) and returns the last one that
 * contains any of [preferKeys]; if none match, it returns the last balanced object; if there are none, null.
 */
internal fun extractJsonObject(raw: String, preferKeys: List<String> = emptyList()): String? {
    val objects = balancedTopLevelObjects(raw)
    if (objects.isEmpty()) return null
    if (preferKeys.isNotEmpty()) {
        objects.lastOrNull { obj -> preferKeys.any { key -> obj.contains("\"$key\"") } }
            ?.let { return it }
    }
    return objects.last()
}

/** All balanced top-level `{...}` substrings in [raw], in order, ignoring braces inside JSON strings. */
private fun balancedTopLevelObjects(raw: String): List<String> {
    val results = mutableListOf<String>()
    var start = -1
    var depth = 0
    var inString = false
    var escaped = false
    for (i in raw.indices) {
        val c = raw[i]
        when {
            escaped -> escaped = false
            c == '\\' && inString -> escaped = true
            c == '"' -> inString = !inString
            inString -> {}
            c == '{' -> {
                if (depth == 0) start = i
                depth++
            }
            c == '}' && depth > 0 -> {
                depth--
                if (depth == 0 && start >= 0) {
                    results.add(raw.substring(start, i + 1))
                    start = -1
                }
            }
        }
    }
    return results
}
