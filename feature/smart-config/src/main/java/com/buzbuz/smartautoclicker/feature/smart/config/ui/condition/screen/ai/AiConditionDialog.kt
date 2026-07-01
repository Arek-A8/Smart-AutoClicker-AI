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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.ai

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.OnConditionConfigCompleteListener

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Minimal configuration dialog for an [com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition.Ai].
 *
 * Built programmatically (no generated ViewBinding) to keep the AI-condition feature self-contained: a natural-language
 * prompt field, a name, a "should appear / should not appear" switch, and a confidence threshold slider.
 */
class AiConditionDialog(
    private val listener: OnConditionConfigCompleteListener,
    private val editHandle: AiConditionEditHandle,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    private lateinit var promptField: EditText
    private lateinit var nameField: EditText
    private lateinit var shouldAppearSwitch: MaterialSwitch
    private lateinit var thresholdSlider: Slider

    override fun onCreateView(): ViewGroup {
        val edited = editHandle.getEditedAiCondition()

        val pad = dp(16)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(context).apply { setText(R.string.generic_name) })
        nameField = EditText(context).apply { setText(edited?.name ?: "") }
        root.addView(nameField)

        root.addView(TextView(context).apply { text = "AI prompt (what to look for)" })
        promptField = EditText(context).apply {
            setText(edited?.prompt ?: "")
            hint = "e.g. the green Start button"
        }
        root.addView(promptField)

        shouldAppearSwitch = MaterialSwitch(context).apply {
            text = "Fulfilled when target is present"
            isChecked = edited?.shouldBeDetected ?: true
        }
        root.addView(shouldAppearSwitch)

        root.addView(TextView(context).apply { text = "Confidence threshold (%)" })
        thresholdSlider = Slider(context).apply {
            valueFrom = 0f
            valueTo = 100f
            value = (edited?.threshold ?: 60).toFloat().coerceIn(0f, 100f)
        }
        root.addView(thresholdSlider)

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(Button(context).apply {
            setText(R.string.generic_delete)
            setOnClickListener { listener.onDeleteClicked(); back() }
        })
        buttons.addView(Button(context).apply {
            text = "Save"
            setOnClickListener {
                editHandle.updateEditedAiCondition(
                    prompt = promptField.text.toString(),
                    name = nameField.text.toString(),
                    threshold = thresholdSlider.value.toInt(),
                    shouldBeDetected = shouldAppearSwitch.isChecked,
                )
                listener.onConfirmClicked()
                back()
            }
        })
        root.addView(buttons)

        return root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) = Unit

    override fun back() {
        listener.onDismissClicked()
        super.back()
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()
}

/** Narrow interface exposing only the AI-edit operations this dialog needs from the brief view model. */
interface AiConditionEditHandle {
    fun getEditedAiCondition(): com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition.Ai?
    fun updateEditedAiCondition(prompt: String, name: String, threshold: Int, shouldBeDetected: Boolean)
}
