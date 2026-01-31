package com.smearcursor.render

import com.intellij.openapi.editor.Editor
import com.intellij.ui.JBColor
import com.smearcursor.animation.AnimationEngine
import com.smearcursor.settings.SmearCursorSettings
import com.smearcursor.util.ColorUtils
import java.awt.*
import java.awt.geom.GeneralPath
import java.awt.geom.Path2D
import kotlin.math.*

/**
 * Renderer for the smear cursor effect.
 * Draws the animated cursor trail and particles on the editor.
 */
class SmearCursorRenderer {

    private var cachedGradient: List<Color>? = null
    private var lastCursorColor: Color? = null
    private var lastBackgroundColor: Color? = null
    
    // Cache stroke for performance
    private val outlineStroke = BasicStroke(1.0f)

    /**
     * Render the animation frame to the graphics context.
     */
    fun render(
        g: Graphics2D,
        frame: AnimationEngine.AnimationFrame,
        editor: Editor,
        cursorWidth: Double,
        cursorHeight: Double
    ) {
        val settings = SmearCursorSettings.getInstance()

        // Get colors
        val backgroundColor = getBackgroundColor(editor)
        val cursorColor = getCursorColor(editor)

        // Update gradient cache if colors changed
        if (cursorColor != lastCursorColor || backgroundColor != lastBackgroundColor) {
            cachedGradient = ColorUtils.generateGradient(
                backgroundColor,
                cursorColor,
                settings.colorLevels,
                settings.gamma
            )
            lastCursorColor = cursorColor
            lastBackgroundColor = backgroundColor
        }

        // Draw the smear quad with gradient
        drawSmearQuad(g, frame, cursorColor, backgroundColor)

        // Draw particles if enabled
        if (settings.particlesEnabled && frame.particles.isNotEmpty()) {
            drawParticles(g, frame.particles, cursorColor, settings)
        }
    }

    /**
     * Draw the main smear quad shape with gradient coloring.
     */
    private fun drawSmearQuad(
        g: Graphics2D,
        frame: AnimationEngine.AnimationFrame,
        cursorColor: Color,
        backgroundColor: Color
    ) {
        val settings = SmearCursorSettings.getInstance()
        val corners = frame.corners

        // Create the quad path
        val path = GeneralPath(Path2D.WIND_EVEN_ODD)
        path.moveTo(corners[0][1], corners[0][0])
        path.lineTo(corners[1][1], corners[1][0])
        path.lineTo(corners[2][1], corners[2][0])
        path.lineTo(corners[3][1], corners[3][0])
        path.closePath()

        // Calculate gradient paint
        val headCorner = corners[frame.headIndex]
        val tailCorner = corners[frame.tailIndex]

        val gradient = GradientPaint(
            tailCorner[1].toFloat(), tailCorner[0].toFloat(), 
            ColorUtils.withAlpha(cursorColor, 0.3),
            headCorner[1].toFloat(), headCorner[0].toFloat(), 
            cursorColor
        )

        // Fill with gradient
        val originalPaint = g.paint
        g.paint = gradient
        g.fill(path)

        // Optionally draw outline for better visibility
        g.color = ColorUtils.withAlpha(cursorColor, 0.8)
        g.stroke = outlineStroke
        g.draw(path)

        g.paint = originalPaint
    }

    /**
     * Draw individual sub-segments of the smear with varying opacity.
     * This provides finer control over the gradient appearance.
     */
    private fun drawSmearSegmented(
        g: Graphics2D,
        frame: AnimationEngine.AnimationFrame,
        cursorColor: Color,
        backgroundColor: Color
    ) {
        val settings = SmearCursorSettings.getInstance()
        val corners = frame.corners
        val gradient = cachedGradient ?: return

        // Calculate the bounding box
        val minX = corners.minOf { it[1] }
        val maxX = corners.maxOf { it[1] }
        val minY = corners.minOf { it[0] }
        val maxY = corners.maxOf { it[0] }

        val width = maxX - minX
        val height = maxY - minY

        if (width <= 0 || height <= 0) return

        // Draw multiple layers with decreasing opacity
        val numLayers = settings.colorLevels
        for (layer in 0 until numLayers) {
            val t = layer.toDouble() / numLayers
            val layerCorners = interpolateCorners(frame.targetPosition, corners, t)
            
            val path = GeneralPath(Path2D.WIND_EVEN_ODD)
            path.moveTo(layerCorners[0][1], layerCorners[0][0])
            path.lineTo(layerCorners[1][1], layerCorners[1][0])
            path.lineTo(layerCorners[2][1], layerCorners[2][0])
            path.lineTo(layerCorners[3][1], layerCorners[3][0])
            path.closePath()

            val colorIndex = ((1.0 - t) * (gradient.size - 1)).toInt().coerceIn(0, gradient.size - 1)
            val alpha = ((1.0 - t).pow(settings.gradientExponent) * 255).toInt().coerceIn(0, 255)
            
            g.color = ColorUtils.withAlpha(gradient[colorIndex], alpha)
            g.fill(path)
        }
    }

    /**
     * Interpolate between target position corners and current corners.
     */
    private fun interpolateCorners(
        targetPosition: DoubleArray,
        currentCorners: Array<DoubleArray>,
        t: Double
    ): Array<DoubleArray> {
        return Array(4) { i ->
            doubleArrayOf(
                targetPosition[0] + t * (currentCorners[i][0] - targetPosition[0]),
                targetPosition[1] + t * (currentCorners[i][1] - targetPosition[1])
            )
        }
    }

    /**
     * Draw particles with fading effect based on lifetime.
     */
    private fun drawParticles(
        g: Graphics2D,
        particles: List<AnimationEngine.Particle>,
        cursorColor: Color,
        settings: SmearCursorSettings
    ) {
        for (particle in particles) {
            val lifetimeRatio = particle.lifetime / settings.particleMaxLifetime
            val alpha = (lifetimeRatio * 255).toInt().coerceIn(0, 255)
            val size = (3 + lifetimeRatio * 4).toInt()

            g.color = ColorUtils.withAlpha(cursorColor, alpha)
            g.fillOval(
                (particle.position[1] - size / 2).toInt(),
                (particle.position[0] - size / 2).toInt(),
                size,
                size
            )
        }
    }

    /**
     * Get the editor's background color.
     */
    private fun getBackgroundColor(editor: Editor): Color {
        return editor.colorsScheme.defaultBackground ?: JBColor.background()
    }

    /**
     * Get the cursor color based on settings.
     */
    private fun getCursorColor(editor: Editor): Color {
        val settings = SmearCursorSettings.getInstance()
        return if (settings.useEditorCursorColor) {
            editor.colorsScheme.getColor(com.intellij.openapi.editor.colors.EditorColors.CARET_COLOR)
                ?: settings.getCursorColor()
        } else {
            settings.getCursorColor()
        }
    }

    /**
     * Clear the cached gradient (call when colors change).
     */
    fun clearCache() {
        cachedGradient = null
        lastCursorColor = null
        lastBackgroundColor = null
    }
}
