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
package com.buzbuz.smartautoclicker.core.smart.ai.di

import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import com.buzbuz.smartautoclicker.core.smart.ai.AiSettings
import com.buzbuz.smartautoclicker.core.smart.ai.CloudProtocol
import com.buzbuz.smartautoclicker.core.smart.ai.VisionBackend
import com.buzbuz.smartautoclicker.core.smart.ai.VisionModel
import com.buzbuz.smartautoclicker.core.smart.ai.VisionModelProvider
import com.buzbuz.smartautoclicker.core.smart.ai.cloud.CloudVisionModel
import com.buzbuz.smartautoclicker.core.smart.ai.local.LocalServerConfig
import com.buzbuz.smartautoclicker.core.smart.ai.local.LocalVisionModel

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AiModule {

    @Provides
    @Singleton
    fun provideVisionModelProvider(settings: AiSettings): VisionModelProvider =
        object : VisionModelProvider {
            override fun getVisionModel(): VisionModel? {
                val config: AiConfig = settings.getConfig()
                if (!config.isComplete()) return null
                return when (config.backend) {
                    VisionBackend.CLOUD -> CloudVisionModel(config)
                    VisionBackend.LOCAL -> LocalVisionModel(
                        LocalServerConfig(maxImageDimensionPx = config.maxImageDimensionPx),
                    )
                }
            }

            override suspend fun testConnection(config: AiConfig): String {
                // Build a CloudVisionModel directly against the resolved endpoint so we exercise the real HTTP path.
                val probeConfig = when (config.backend) {
                    VisionBackend.CLOUD -> config
                    VisionBackend.LOCAL -> config.copy(
                        protocol = CloudProtocol.OPENAI_COMPATIBLE,
                        baseUrl = LocalServerConfig().let { "http://${it.host}:${it.port}/v1" },
                        apiKey = "local",
                        requestTimeoutMs = LocalServerConfig().requestTimeoutMs,
                    )
                }
                return CloudVisionModel(probeConfig).testConnection()
            }
        }
}
