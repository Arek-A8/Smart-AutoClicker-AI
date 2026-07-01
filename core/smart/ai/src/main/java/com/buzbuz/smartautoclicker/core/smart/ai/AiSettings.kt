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

import android.content.Context
import androidx.core.content.edit

import dagger.hilt.android.qualifiers.ApplicationContext

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistence for [AiConfig], backed by SharedPreferences.
 *
 * Self-contained in the AI module to avoid coupling to the app's settings storage. The API key is stored on-device
 * only and never logged. This is a minimal store for the first AI slice; it can later be migrated to the shared
 * DataStore-based settings if desired.
 */
@Singleton
class AiSettings @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    fun getConfig(): AiConfig = AiConfig(
        backend = runCatching { VisionBackend.valueOf(prefs.getString(KEY_BACKEND, null) ?: "") }
            .getOrDefault(VisionBackend.LOCAL),
        protocol = runCatching { CloudProtocol.valueOf(prefs.getString(KEY_PROTOCOL, null) ?: "") }
            .getOrDefault(CloudProtocol.GEMINI_NATIVE),
        baseUrl = prefs.getString(KEY_BASE_URL, "") ?: "",
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        model = prefs.getString(KEY_MODEL, "") ?: "",
        maxImageDimensionPx = prefs.getInt(KEY_MAX_IMAGE_DIM, DEFAULT_MAX_IMAGE_DIM),
    )

    fun setConfig(config: AiConfig) {
        prefs.edit {
            putString(KEY_BACKEND, config.backend.name)
            putString(KEY_PROTOCOL, config.protocol.name)
            putString(KEY_BASE_URL, config.baseUrl)
            putString(KEY_API_KEY, config.apiKey)
            putString(KEY_MODEL, config.model)
            putInt(KEY_MAX_IMAGE_DIM, config.maxImageDimensionPx)
        }
    }

    /** The AI image resolution (longest-edge cap) used to downscale frames before sending to the model. */
    fun getImageResolution(): Int = prefs.getInt(KEY_MAX_IMAGE_DIM, DEFAULT_MAX_IMAGE_DIM)

    fun setImageResolution(px: Int) {
        prefs.edit { putInt(KEY_MAX_IMAGE_DIM, px.coerceIn(MIN_IMAGE_DIM, MAX_IMAGE_DIM)) }
    }

    companion object {
        private const val PREFS_NAME = "ai_settings"
        private const val KEY_BACKEND = "backend"
        private const val KEY_PROTOCOL = "protocol"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_MAX_IMAGE_DIM = "max_image_dim"

        const val MIN_IMAGE_DIM = 256
        const val MAX_IMAGE_DIM = 2048
        const val DEFAULT_MAX_IMAGE_DIM = 1024
    }
}
