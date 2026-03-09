package com.smearcursor.render

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.smearcursor.animation.AnimationEngine
import com.smearcursor.settings.SmearCursorSettings
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.Timer

/**
 * Overlay component that renders the smear cursor effect on top of the editor.
 * This component is added to the editor's layered pane and handles animation timing.
 */
class SmearCursorOverlay(private val editor: Editor) : JComponent(), CaretListener, DocumentListener {

    private val animationEngine = AnimationEngine()
    private val renderer = SmearCursorRenderer()
    private var animationTimer: Timer? = null
    private var lastCaretPosition: Point? = null
    private var enabled = true

    // Cursor dimensions
    private var cursorWidth = 2.0
    private var cursorHeight = 16.0

    // Tracks whether the caret moved due to a document change (typing/deletion)
    @Volatile
    private var documentJustChanged = false

    init {
        isOpaque = false
        isVisible = true
        
        // Make this component completely mouse-transparent
        // This allows clicks to pass through to the editor below
        isFocusable = false
        
        // Initialize cursor dimensions from editor
        updateCursorDimensions()
        
        // Initialize animation engine
        val caretPos = getCaretScreenPosition()
        if (caretPos != null) {
            animationEngine.initialize(cursorWidth, cursorHeight)
            animationEngine.jump(caretPos.x.toDouble(), caretPos.y.toDouble())
            lastCaretPosition = caretPos
        }

        // Add caret listener and document listener
        editor.caretModel.addCaretListener(this)
        editor.document.addDocumentListener(this)

        // Create animation timer with coalescing for better performance
        animationTimer = Timer(SmearCursorSettings.getInstance().timeInterval) {
            if (enabled && animationEngine.isAnimating()) {
                repaint()
            }
        }
        animationTimer?.isRepeats = true
        animationTimer?.isCoalesce = true // Coalesce multiple pending events
    }

    /**
     * Update cursor dimensions based on editor font metrics.
     */
    private fun updateCursorDimensions() {
        val fontMetrics = editor.contentComponent.getFontMetrics(editor.colorsScheme.getFont(null))
        cursorHeight = fontMetrics.height.toDouble()
        cursorWidth = maxOf(2.0, fontMetrics.charWidth('M').toDouble() / 4.0)
        animationEngine.initialize(cursorWidth, cursorHeight)
    }

    /**
     * Get the current caret position relative to the overlay's coordinate system.
     * In IntelliJ 2025+, visualPositionToXY returns viewport-relative coordinates,
     * so we don't need to subtract the scroll offset.
     */
    private fun getCaretScreenPosition(): Point? {
        try {
            if (!editor.contentComponent.isShowing || !this.isShowing) {
                return null
            }
            
            val caret = editor.caretModel.currentCaret
            val visualPosition = caret.visualPosition
            
            // Get position - visualPositionToXY returns viewport-relative coordinates
            val point = editor.visualPositionToXY(visualPosition)
            
            // Check if caret is in visible area
            if (point.y < 0 || point.y > height || point.x < 0 || point.x > width) {
                return null
            }
            
            return point
        } catch (e: Exception) {
            return null
        }
    }

    // DocumentListener: detect when text is being typed/deleted
    override fun documentChanged(event: DocumentEvent) {
        documentJustChanged = true
    }

    override fun caretPositionChanged(event: CaretEvent) {
        if (!enabled) return

        val settings = SmearCursorSettings.getInstance()
        if (!settings.enabled) return

        // Capture and reset the typing flag
        val isTypingChange = documentJustChanged
        documentJustChanged = false

        SwingUtilities.invokeLater {
            val newPosition = getCaretScreenPosition() ?: return@invokeLater
            val oldPosition = lastCaretPosition

            // If smear-while-typing is disabled and this caret move was caused by a document change, skip animation
            val suppressForTyping = isTypingChange && !settings.smearWhileTyping

            if (oldPosition != null && !suppressForTyping) {
                val dx = kotlin.math.abs(newPosition.x - oldPosition.x)
                val dy = kotlin.math.abs(newPosition.y - oldPosition.y)

                // Check if movement is significant enough
                if (dx > cursorWidth / 2 || dy > cursorHeight / 2) {
                    // Check direction restrictions
                    val shouldAnimate = when {
                        !settings.smearHorizontally && dy <= cursorHeight / 2 -> false
                        !settings.smearVertically && dx <= cursorWidth / 2 -> false
                        !settings.smearDiagonally && dy > cursorHeight / 2 && dx > cursorWidth / 2 -> false
                        !settings.smearBetweenNeighborLines && dy <= cursorHeight * 1.5 -> false
                        else -> true
                    }

                    if (shouldAnimate) {
                        animationEngine.animateTo(newPosition.x.toDouble(), newPosition.y.toDouble())
                        startAnimation()
                    } else {
                        animationEngine.jump(newPosition.x.toDouble(), newPosition.y.toDouble())
                    }
                } else {
                    // Small movement, just jump
                    animationEngine.jump(newPosition.x.toDouble(), newPosition.y.toDouble())
                }
            } else {
                animationEngine.jump(newPosition.x.toDouble(), newPosition.y.toDouble())
            }

            lastCaretPosition = newPosition
            repaint() // Always repaint on caret move
        }
    }

    /**
     * Start the animation timer.
     */
    private fun startAnimation() {
        if (animationTimer?.isRunning != true) {
            animationTimer?.start()
        }
        repaint()
    }

    /**
     * Stop the animation timer.
     */
    private fun stopAnimation() {
        animationTimer?.stop()
        animationEngine.stopAnimation()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (!enabled || !SmearCursorSettings.getInstance().enabled) return

        val g2d = g.create() as Graphics2D
        try {
            // Set rendering hints for performance
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON
            )
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_RENDERING,
                java.awt.RenderingHints.VALUE_RENDER_SPEED
            )
            
            // Update animation and get current frame
            val frame = animationEngine.update()

            if (frame != null && frame.isAnimating) {
                renderer.render(g2d, frame, editor, cursorWidth, cursorHeight)
                
                // Update timer interval in case settings changed
                animationTimer?.delay = animationEngine.getFrameInterval()
            } else {
                // Animation finished
                animationTimer?.stop()
            }
        } finally {
            g2d.dispose()
        }
    }

    /**
     * Enable or disable the overlay.
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.enabled = enabled
        if (!enabled) {
            stopAnimation()
            repaint()
        }
    }

    /**
     * Make the overlay completely mouse-transparent.
     * By always returning false, all mouse events pass through to the editor below.
     * This ensures Cmd+Click (Go to Declaration) and other mouse interactions work normally.
     */
    override fun contains(x: Int, y: Int): Boolean = false

    /**
     * Check if the overlay is enabled.
     */
    fun isOverlayEnabled(): Boolean = enabled

    /**
     * Clean up resources when the overlay is no longer needed.
     */
    fun dispose() {
        animationTimer?.stop()
        animationTimer = null
        editor.caretModel.removeCaretListener(this)
        editor.document.removeDocumentListener(this)
    }

    /**
     * Handle editor scroll events by updating the caret position.
     */
    fun onScroll() {
        // When scrolling, the caret's screen position changes even if logical position doesn't
        // We need to jump (not animate) to the new position to avoid weird trails
        val newPosition = getCaretScreenPosition()
        if (newPosition != null) {
            animationEngine.jump(newPosition.x.toDouble(), newPosition.y.toDouble())
            lastCaretPosition = newPosition
        } else {
            // Caret not visible, stop animation
            animationEngine.stopAnimation()
            lastCaretPosition = null
        }
        repaint()
    }

    /**
     * Force refresh the cursor dimensions (call when font changes).
     */
    fun refreshDimensions() {
        updateCursorDimensions()
        val caretPos = getCaretScreenPosition()
        if (caretPos != null) {
            animationEngine.jump(caretPos.x.toDouble(), caretPos.y.toDouble())
            lastCaretPosition = caretPos
        }
    }
}
