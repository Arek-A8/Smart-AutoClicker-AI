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

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.smart.ai.AgentAction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for [parseAction] and [parseDetection], covering the vision-model output quirks that caused 0,0 misclicks. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ResultParsingTests {

    // --- parseAction: the reasoning-echo bug (the 95% 0,0 case) ---

    @Test
    fun tap_ignoresSchemaEchoAndUsesRealAnswer() {
        // Reasoning models echo the schema object BEFORE the real answer. Must pick the real one, not 0,0.
        val raw = """
            I need to tap the button. The format is {"action":"tap","x":<int>,"y":<int>}.
            Looking at the screen, the button is here.
            {"action":"tap","x":120,"y":340,"reason":"the login button"}
        """.trimIndent()
        val action = parseAction(raw, scaleX = 1f, scaleY = 1f)
        assertEquals(AgentAction.Tap(120, 340), action)
    }

    @Test
    fun tap_scalesCoordinatesBackToOriginalFrame() {
        val raw = """{"action":"tap","x":100,"y":50}"""
        val action = parseAction(raw, scaleX = 2f, scaleY = 3f)
        assertEquals(AgentAction.Tap(200, 150), action)
    }

    // --- parseAction: lenient numeric parsing (float / string coords) ---

    @Test
    fun tap_acceptsFloatCoordinates() {
        val raw = """{"action":"tap","x":512.0,"y":384.6}"""
        val action = parseAction(raw, scaleX = 1f, scaleY = 1f)
        assertEquals(AgentAction.Tap(512, 384), action)
    }

    @Test
    fun tap_acceptsStringCoordinates() {
        val raw = """{"action":"tap","x":"640","y":"480"}"""
        val action = parseAction(raw, scaleX = 1f, scaleY = 1f)
        assertEquals(AgentAction.Tap(640, 480), action)
    }

    // --- parseAction: missing coords must FAIL, never 0,0 ---

    @Test
    fun tap_withoutCoordinatesFailsInsteadOfZeroZero() {
        val raw = """{"action":"tap","reason":"unsure"}"""
        val action = parseAction(raw, scaleX = 1f, scaleY = 1f)
        assertTrue("expected Fail but was $action", action is AgentAction.Fail)
    }

    @Test
    fun swipe_withMissingCoordinateFails() {
        val raw = """{"action":"swipe","fromX":10,"fromY":20,"toX":30}"""
        val action = parseAction(raw, scaleX = 1f, scaleY = 1f)
        assertTrue("expected Fail but was $action", action is AgentAction.Fail)
    }

    @Test
    fun swipe_validIsParsedAndScaled() {
        val raw = """{"action":"swipe","fromX":10,"fromY":20,"toX":30,"toY":40,"durationMs":500}"""
        val action = parseAction(raw, scaleX = 2f, scaleY = 2f)
        assertEquals(AgentAction.Swipe(20, 40, 60, 80, 500L), action)
    }

    @Test
    fun noJsonFails() {
        val action = parseAction("I cannot see anything actionable.", scaleX = 1f, scaleY = 1f)
        assertTrue(action is AgentAction.Fail)
    }

    @Test
    fun done_isParsed() {
        val action = parseAction("""{"action":"done","reason":"goal reached"}""", scaleX = 1f, scaleY = 1f)
        assertEquals(AgentAction.Done("goal reached"), action)
    }

    // --- parseDetection ---

    @Test
    fun detect_ignoresSchemaEchoAndScalesBox() {
        val raw = """
            The schema is {"found": <bool>, "box": [<x>,<y>,<w>,<h>]}.
            {"found":true,"confidence":0.9,"box":[10,20,30,40]}
        """.trimIndent()
        val result = parseDetection(raw, scaleX = 2f, scaleY = 2f)
        assertTrue(result.found)
        assertEquals(20, result.location?.left)
        assertEquals(40, result.location?.top)
        assertEquals(80, result.location?.right)   // (10+30)*2
        assertEquals(120, result.location?.bottom) // (20+40)*2
    }

    @Test
    fun detect_notFoundWhenNoJson() {
        val result = parseDetection("nope", scaleX = 1f, scaleY = 1f)
        assertTrue(!result.found)
    }
}
