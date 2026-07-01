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

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Result of an HTTP call: the status code and the raw response body (which may be an error body). */
internal data class HttpResult(val code: Int, val body: String) {
    val isSuccess: Boolean get() = code in 200..299
}

/**
 * Minimal JSON POST over [HttpURLConnection], avoiding any extra dependency. Blocking; callers must invoke from an
 * IO dispatcher.
 *
 * @param url full request URL.
 * @param jsonBody request body, already serialized JSON.
 * @param headers additional request headers (e.g. Authorization).
 * @param timeoutMs connect and read timeout.
 */
internal fun httpPostJson(
    url: String,
    jsonBody: String,
    headers: Map<String, String>,
    timeoutMs: Int,
): HttpResult {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = timeoutMs
        readTimeout = timeoutMs
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        headers.forEach { (k, v) -> setRequestProperty(k, v) }
    }
    try {
        connection.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
        return HttpResult(code, body)
    } catch (e: IOException) {
        throw e
    } finally {
        connection.disconnect()
    }
}
