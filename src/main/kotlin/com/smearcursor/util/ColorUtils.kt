package com.smearcursor.util

import com.intellij.ui.JBColor
import com.smearcursor.settings.SmearCursorSettings
import java.awt.Color
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Color utilities for gradient generation and color interpolation.
 * Mirrors the color.lua functionality from the Neovim plugin.
 */
object ColorUtils {

    /**
     * Interpolate between two colors using gamma-corrected blending.
     */
    fun interpolateColors(color1: Color, color2: Color, t: Double, gamma: Double = 2.2): Color {
        // Gamma-correct the colors for perceptually uniform blending
        val r1 = (color1.red / 255.0).pow(gamma)
        val g1 = (color1.green / 255.0).pow(gamma)
        val b1 = (color1.blue / 255.0).pow(gamma)

        val r2 = (color2.red / 255.0).pow(gamma)
        val g2 = (color2.green / 255.0).pow(gamma)
        val b2 = (color2.blue / 255.0).pow(gamma)

        // Linear interpolation in gamma space
        val r = (r1 + t * (r2 - r1)).pow(1.0 / gamma)
        val g = (g1 + t * (g2 - g1)).pow(1.0 / gamma)
        val b = (b1 + t * (b2 - b1)).pow(1.0 / gamma)

        return Color(
            (r * 255).roundToInt().coerceIn(0, 255),
            (g * 255).roundToInt().coerceIn(0, 255),
            (b * 255).roundToInt().coerceIn(0, 255)
        )
    }

    /**
     * Generate a gradient of colors from background to cursor color.
     */
    fun generateGradient(
        backgroundColor: Color,
        cursorColor: Color,
        levels: Int,
        gamma: Double = 2.2
    ): List<Color> {
        return (1..levels).map { level ->
            val t = level.toDouble() / levels
            interpolateColors(backgroundColor, cursorColor, t, gamma)
        }
    }

    /**
     * Get gradient color at a specific level (0-1).
     */
    fun getGradientColor(
        backgroundColor: Color,
        cursorColor: Color,
        level: Double,
        gamma: Double = 2.2
    ): Color {
        val t = level.coerceIn(0.0, 1.0)
        return interpolateColors(backgroundColor, cursorColor, t, gamma)
    }

    /**
     * Create a color with modified alpha.
     */
    fun withAlpha(color: Color, alpha: Int): Color {
        return Color(color.red, color.green, color.blue, alpha.coerceIn(0, 255))
    }

    /**
     * Create a color with modified alpha (0-1 scale).
     */
    fun withAlpha(color: Color, alpha: Double): Color {
        return withAlpha(color, (alpha * 255).roundToInt())
    }

    /**
     * Get the default background color (editor background).
     */
    fun getDefaultBackgroundColor(): Color {
        return JBColor.background()
    }

    /**
     * Get the cursor color from settings or editor defaults.
     */
    fun getCursorColor(): Color {
        val settings = SmearCursorSettings.getInstance()
        return if (settings.useEditorCursorColor) {
            // Try to get editor caret color, fall back to settings
            JBColor.foreground()
        } else {
            settings.getCursorColor()
        }
    }

    /**
     * Compute gradient shade based on position along the smear.
     */
    fun computeGradientShade(
        position: DoubleArray,
        gradientOrigin: DoubleArray,
        gradientDirection: DoubleArray,
        exponent: Double = 1.0
    ): Double {
        if (gradientDirection[0] == 0.0 && gradientDirection[1] == 0.0) return 1.0

        val dy = position[0] - gradientOrigin[0]
        val dx = position[1] - gradientOrigin[1]
        val projection = dy * gradientDirection[0] + dx * gradientDirection[1]
        val normalizedProjection = projection.coerceIn(0.0, 1.0)

        return (1.0 - normalizedProjection).pow(exponent)
    }
}
