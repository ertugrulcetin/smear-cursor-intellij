package com.smearcursor.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.awt.Color

/**
 * Persistent settings for the Smear Cursor plugin.
 * All configuration options mirrored from the Neovim plugin.
 */
@State(
    name = "SmearCursorSettings",
    storages = [Storage("SmearCursorSettings.xml")]
)
class SmearCursorSettings : PersistentStateComponent<SmearCursorSettings> {

    // General configuration
    var enabled: Boolean = true
    var smearWhileTyping: Boolean = false
    var smearBetweenWindows: Boolean = true
    var smearBetweenNeighborLines: Boolean = true
    var minHorizontalDistanceSmear: Int = 0
    var minVerticalDistanceSmear: Int = 0
    var smearHorizontally: Boolean = true
    var smearVertically: Boolean = true
    var smearDiagonally: Boolean = true

    // Animation timing
    var timeInterval: Int = 17 // milliseconds (approximately 60 FPS)
    var delayEventToSmear: Int = 1 // milliseconds

    // Smear dynamics configuration
    var stiffness: Double = 0.6 // How fast the smear's head moves towards target (0-1)
    var trailingStiffness: Double = 0.45 // How fast the smear's tail moves towards target (0-1)
    var anticipation: Double = 0.2 // Initial velocity factor opposite to target
    var damping: Double = 0.85 // Velocity reduction over time (0-1)
    var trailingExponent: Double = 3.0 // Controls middle points closer to head or tail
    var distanceStopAnimating: Double = 0.1 // Stop when within this distance

    // Insert mode specific settings
    var stiffnessInsertMode: Double = 0.5
    var trailingStiffnessInsertMode: Double = 0.5
    var dampingInsertMode: Double = 0.9
    var trailingExponentInsertMode: Double = 1.0

    // Visual settings
    var colorLevels: Int = 16 // Number of gradient steps
    var gamma: Double = 2.2 // For color blending
    var gradientExponent: Double = 1.0 // For longitudinal gradient
    var maxLength: Int = 25 // Maximum smear length in characters

    // Color settings (stored as RGB integers)
    var cursorColorRgb: Int = Color(208, 208, 208).rgb // Default cursor color
    var useEditorCursorColor: Boolean = true // Use the editor's cursor color

    // Particle configuration
    var particlesEnabled: Boolean = false
    var particleMaxNum: Int = 100
    var particleSpread: Double = 0.5
    var particlesPerSecond: Int = 200
    var particlesPerLength: Double = 1.0
    var particleMaxLifetime: Int = 300 // milliseconds
    var particleMaxInitialVelocity: Double = 10.0
    var particleDamping: Double = 0.2
    var particleGravity: Double = 20.0

    companion object {
        @JvmStatic
        fun getInstance(): SmearCursorSettings {
            return ApplicationManager.getApplication().getService(SmearCursorSettings::class.java)
        }
    }

    override fun getState(): SmearCursorSettings = this

    override fun loadState(state: SmearCursorSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getCursorColor(): Color = Color(cursorColorRgb)

    fun setCursorColor(color: Color) {
        cursorColorRgb = color.rgb
    }
}
