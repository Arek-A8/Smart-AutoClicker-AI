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

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Downscale [this] so that its longest edge is at most [maxDimensionPx], preserving aspect ratio. Returns the original
 * bitmap if it already fits. Used to bound upload latency and token cost before sending a frame to a cloud model.
 */
internal fun Bitmap.downscaledTo(maxDimensionPx: Int): Bitmap {
    val longestEdge = maxOf(width, height)
    if (longestEdge <= maxDimensionPx) return this

    val scale = maxDimensionPx.toFloat() / longestEdge.toFloat()
    val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
}

/**
 * Encode [this] as raw base64 JPEG bytes (no data-URL prefix), suitable for Gemini's inline_data part.
 *
 * @param quality JPEG quality in [0, 100].
 */
internal fun Bitmap.toJpegBase64(quality: Int = 85): String {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

/**
 * Encode [this] as a base64 JPEG data URL suitable for an OpenAI-compatible image_url content part.
 *
 * @param quality JPEG quality in [0, 100].
 */
internal fun Bitmap.toJpegDataUrl(quality: Int = 85): String =
    "data:image/jpeg;base64,${toJpegBase64(quality)}"
