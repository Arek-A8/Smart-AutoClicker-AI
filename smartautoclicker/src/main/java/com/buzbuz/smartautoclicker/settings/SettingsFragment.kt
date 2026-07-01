/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.settings

import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

import com.buzbuz.smartautoclicker.core.smart.ai.AiConfig
import com.buzbuz.smartautoclicker.core.smart.ai.ModelListResult
import com.buzbuz.smartautoclicker.core.smart.ai.VisionBackend
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle

import com.buzbuz.smartautoclicker.databinding.FragmentSettingsBinding

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var viewBinding: FragmentSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.fieldShowScenarioFilters.apply {
            setTitle(requireContext().getString(R.string.field_show_scenario_filters_ui_title))
            setDescription(requireContext().getString(R.string.field_show_scenario_filters_ui_desc))
            setOnClickListener(viewModel::toggleScenarioFiltersUi)
        }

        viewBinding.fieldLegacyActionsUi.apply {
            setTitle(requireContext().getString(R.string.field_legacy_action_ui_title))
            setDescription(requireContext().getString(R.string.field_legacy_action_ui_desc))
            setOnClickListener(viewModel::toggleLegacyActionUi)
        }

        viewBinding.fieldLegacyNotificationUi.apply {
            setTitle(requireContext().getString(R.string.field_legacy_notification_ui_title))
            setDescription(requireContext().getString(R.string.field_legacy_notification_ui_desc))
            setOnClickListener(viewModel::toggleLegacyNotificationUi)
        }

        viewBinding.fieldForceEntireScreen.apply {
            setTitle(requireContext().getString(R.string.field_force_entire_screen_title))
            setDescription(requireContext().getString(R.string.field_force_entire_screen_desc))
            setOnClickListener(viewModel::toggleForceEntireScreenCapture)
        }

        viewBinding.fieldInputBlockWorkaround.apply {
            setTitle(requireContext().getString(R.string.field_input_block_workaround_title))
            setDescription(requireContext().getString(R.string.field_input_block_workaround_desc))
            setOnClickListener(viewModel::toggleInputBlockWorkaround)
        }

        viewBinding.fieldPrivacySettings.apply {
            setTitle(requireContext().getString(R.string.field_privacy))
            setOnClickListener { viewModel.showPrivacySettings(requireActivity()) }
        }

        viewBinding.fieldRemoveAds.apply {
            setTitle(requireContext().getString(R.string.field_remove_ads))
            setOnClickListener { viewModel.showPurchaseActivity(requireActivity()) }
        }

        viewBinding.fieldTroubleshooting.apply {
            setTitle(requireContext().getString(R.string.field_troubleshooting))
            setOnClickListener { viewModel.showTroubleshootingDialog(requireActivity()) }
        }

        viewBinding.fieldAiConfig.apply {
            setTitle(requireContext().getString(R.string.field_ai_config_title))
            setDescription(requireContext().getString(R.string.field_ai_config_desc))
            setOnClickListener { showAiConfigDialog() }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isScenarioFiltersUiEnabled.collect(viewBinding.fieldShowScenarioFilters::setChecked) }
                launch { viewModel.isLegacyActionUiEnabled.collect(viewBinding.fieldLegacyActionsUi::setChecked) }
                launch { viewModel.isLegacyNotificationUiEnabled.collect(viewBinding.fieldLegacyNotificationUi::setChecked) }
                launch { viewModel.isEntireScreenCaptureForced.collect(viewBinding.fieldForceEntireScreen::setChecked) }
                launch { viewModel.isInputWorkaroundEnabled.collect(viewBinding.fieldInputBlockWorkaround::setChecked) }
                launch { viewModel.shouldShowInputBlockWorkaround.collect(::updateInputBlockWorkaroundVisibility) }
                launch { viewModel.shouldShowEntireScreenCapture.collect(::updateForceEntireScreenVisibility) }
                launch { viewModel.shouldShowPrivacySettings.collect(::updatePrivacySettingsVisibility) }
                launch { viewModel.shouldShowPurchase.collect(::updateRemoveAdsVisibility) }
            }
        }
    }

    private fun showAiConfigDialog() {
        val ctx = requireContext()
        val config = viewModel.getAiConfig()

        fun dp(v: Int) = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics,
        ).toInt()

        val pad = dp(20)
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, dp(8), pad, 0)
        }

        val useLocalSwitch = MaterialSwitch(ctx).apply {
            text = ctx.getString(R.string.field_ai_config_use_local)
            isChecked = config.backend == VisionBackend.LOCAL
        }
        container.addView(useLocalSwitch)

        container.addView(TextView(ctx).apply { text = ctx.getString(R.string.field_ai_config_base_url) })
        val baseUrlField = EditText(ctx).apply {
            setText(config.baseUrl)
            hint = "https://generativelanguage.googleapis.com"
            inputType = InputType.TYPE_TEXT_VARIATION_URI
        }
        container.addView(baseUrlField)

        container.addView(TextView(ctx).apply { text = ctx.getString(R.string.field_ai_config_api_key) })
        val apiKeyField = EditText(ctx).apply {
            setText(config.apiKey)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(apiKeyField)

        container.addView(TextView(ctx).apply { text = ctx.getString(R.string.field_ai_config_model) })
        val modelField = EditText(ctx).apply {
            setText(config.model)
            hint = "gemini-3.1-flash-lite"
        }
        container.addView(modelField)

        fun currentConfig(): AiConfig = config.copy(
            backend = if (useLocalSwitch.isChecked) VisionBackend.LOCAL else VisionBackend.CLOUD,
            baseUrl = baseUrlField.text.toString().trim(),
            apiKey = apiKeyField.text.toString().trim(),
            model = modelField.text.toString().trim(),
        )

        val fetchModelsButton = Button(ctx).apply {
            text = ctx.getString(R.string.field_ai_config_fetch_models)
            setOnClickListener { fetchAndPickModel(currentConfig()) { picked -> modelField.setText(picked) } }
        }
        container.addView(fetchModelsButton)

        container.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.field_ai_config_local_hint)
            setPadding(0, dp(8), 0, 0)
        })

        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.field_ai_config_title)
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.setAiConfig(currentConfig())
            }
            .setNeutralButton(R.string.field_ai_config_test) { _, _ ->
                // Persist first so the test uses exactly what the user entered, then probe the endpoint.
                val cfg = currentConfig()
                viewModel.setAiConfig(cfg)
                runAiConnectionTest(cfg)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun runAiConnectionTest(config: AiConfig) {
        val ctx = requireContext()
        val progress = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.field_ai_config_test)
            .setMessage(getString(R.string.field_ai_config_testing))
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val result = viewModel.testAiConnection(config)
            progress.dismiss()
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.field_ai_config_test)
                .setMessage(result)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun fetchAndPickModel(config: AiConfig, onPicked: (String) -> Unit) {
        val ctx = requireContext()
        val progress = MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.field_ai_config_fetch_models)
            .setMessage(getString(R.string.field_ai_config_fetching_models))
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val result = viewModel.listAiModels(config)
            progress.dismiss()
            when (result) {
                is ModelListResult.Success -> {
                    val ids = result.modelIds.toTypedArray()
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.field_ai_config_pick_model)
                        .setItems(ids) { _, which -> onPicked(ids[which]) }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                is ModelListResult.Failure -> {
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(R.string.field_ai_config_fetch_models)
                        .setMessage(getString(R.string.field_ai_config_fetch_models_failed, result.reason))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun updateForceEntireScreenVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerForceEntireScreen.visibility = View.VISIBLE
            viewBinding.fieldForceEntireScreen.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerForceEntireScreen.visibility = View.GONE
            viewBinding.fieldForceEntireScreen.root.visibility = View.GONE
        }
    }

    private fun updateInputBlockWorkaroundVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerInputBlockWorkaround.visibility = View.VISIBLE
            viewBinding.fieldInputBlockWorkaround.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerInputBlockWorkaround.visibility = View.GONE
            viewBinding.fieldInputBlockWorkaround.root.visibility = View.GONE
        }
    }

    private fun updatePrivacySettingsVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerPrivacySettings.visibility = View.VISIBLE
            viewBinding.fieldPrivacySettings.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerPrivacySettings.visibility = View.GONE
            viewBinding.fieldPrivacySettings.root.visibility = View.GONE
        }
    }

    private fun updateRemoveAdsVisibility(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            viewBinding.dividerRemoveAds.visibility = View.VISIBLE
            viewBinding.fieldRemoveAds.root.visibility = View.VISIBLE
        } else {
            viewBinding.dividerRemoveAds.visibility = View.GONE
            viewBinding.fieldRemoveAds.root.visibility = View.GONE
        }
    }
}