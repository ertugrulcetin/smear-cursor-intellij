package com.smearcursor.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ColorPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBSlider
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import kotlin.math.roundToInt

/**
 * Settings UI panel for Smear Cursor configuration.
 */
class SmearCursorConfigurable : Configurable {

    private var mainPanel: JPanel? = null

    // General settings
    private val enabledCheckbox = JBCheckBox("Enable Smear Cursor")
    private val smearWhileTypingCheckbox = JBCheckBox("Smear effect while typing")
    private val smearBetweenWindowsCheckbox = JBCheckBox("Smear between windows")
    private val smearBetweenNeighborLinesCheckbox = JBCheckBox("Smear between neighbor lines")
    private val smearHorizontallyCheckbox = JBCheckBox("Smear horizontally")
    private val smearVerticallyCheckbox = JBCheckBox("Smear vertically")
    private val smearDiagonallyCheckbox = JBCheckBox("Smear diagonally")

    // Animation sliders
    private val stiffnessSlider = createSlider(0, 100, 60)
    private val trailingStiffnessSlider = createSlider(0, 100, 45)
    private val dampingSlider = createSlider(0, 100, 85)
    private val anticipationSlider = createSlider(0, 50, 20)
    private val maxLengthSlider = createSlider(1, 50, 25)
    private val timeIntervalSlider = createSlider(5, 50, 17)

    // Particle settings
    private val particlesEnabledCheckbox = JBCheckBox("Enable particles")
    private val particleCountSlider = createSlider(10, 200, 100)
    private val particleLifetimeSlider = createSlider(100, 1000, 300)

    // Color settings
    private val useEditorColorCheckbox = JBCheckBox("Use editor cursor color")
    private val colorPanel = ColorPanel()

    private fun createSlider(min: Int, max: Int, value: Int): JBSlider {
        return JBSlider(min, max, value).apply {
            paintTicks = true
            paintLabels = true
            majorTickSpacing = (max - min) / 4
            minorTickSpacing = (max - min) / 10
        }
    }

    override fun getDisplayName(): String = "Smear Cursor"

    override fun createComponent(): JComponent {
        val settings = SmearCursorSettings.getInstance()

        // Load current settings
        enabledCheckbox.isSelected = settings.enabled
        smearWhileTypingCheckbox.isSelected = settings.smearWhileTyping
        smearBetweenWindowsCheckbox.isSelected = settings.smearBetweenWindows
        smearBetweenNeighborLinesCheckbox.isSelected = settings.smearBetweenNeighborLines
        smearHorizontallyCheckbox.isSelected = settings.smearHorizontally
        smearVerticallyCheckbox.isSelected = settings.smearVertically
        smearDiagonallyCheckbox.isSelected = settings.smearDiagonally

        stiffnessSlider.value = (settings.stiffness * 100).roundToInt()
        trailingStiffnessSlider.value = (settings.trailingStiffness * 100).roundToInt()
        dampingSlider.value = (settings.damping * 100).roundToInt()
        anticipationSlider.value = (settings.anticipation * 100).roundToInt()
        maxLengthSlider.value = settings.maxLength
        timeIntervalSlider.value = settings.timeInterval

        particlesEnabledCheckbox.isSelected = settings.particlesEnabled
        particleCountSlider.value = settings.particleMaxNum
        particleLifetimeSlider.value = settings.particleMaxLifetime

        useEditorColorCheckbox.isSelected = settings.useEditorCursorColor
        colorPanel.selectedColor = settings.getCursorColor()

        // Build the settings panel
        val generalPanel = createTitledPanel("General Settings") {
            FormBuilder.createFormBuilder()
                .addComponent(enabledCheckbox)
                .addComponent(smearWhileTypingCheckbox)
                .addComponent(smearBetweenWindowsCheckbox)
                .addComponent(smearBetweenNeighborLinesCheckbox)
                .addComponent(smearHorizontallyCheckbox)
                .addComponent(smearVerticallyCheckbox)
                .addComponent(smearDiagonallyCheckbox)
                .panel
        }

        val animationPanel = createTitledPanel("Animation Settings") {
            FormBuilder.createFormBuilder()
                .addLabeledComponent(JBLabel("Stiffness (head speed):"), createSliderWithValue(stiffnessSlider, "%"))
                .addLabeledComponent(JBLabel("Trailing Stiffness (tail speed):"), createSliderWithValue(trailingStiffnessSlider, "%"))
                .addLabeledComponent(JBLabel("Damping (velocity reduction):"), createSliderWithValue(dampingSlider, "%"))
                .addLabeledComponent(JBLabel("Anticipation:"), createSliderWithValue(anticipationSlider, "%"))
                .addLabeledComponent(JBLabel("Max Length (characters):"), createSliderWithValue(maxLengthSlider, ""))
                .addLabeledComponent(JBLabel("Frame Interval (ms):"), createSliderWithValue(timeIntervalSlider, "ms"))
                .panel
        }

        val particlePanel = createTitledPanel("Particle Effects") {
            FormBuilder.createFormBuilder()
                .addComponent(particlesEnabledCheckbox)
                .addLabeledComponent(JBLabel("Max Particles:"), createSliderWithValue(particleCountSlider, ""))
                .addLabeledComponent(JBLabel("Particle Lifetime:"), createSliderWithValue(particleLifetimeSlider, "ms"))
                .panel
        }

        val colorSettingsPanel = createTitledPanel("Color Settings") {
            val colorRow = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JBLabel("Cursor Color:"))
                add(colorPanel)
            }
            FormBuilder.createFormBuilder()
                .addComponent(useEditorColorCheckbox)
                .addComponent(colorRow)
                .panel
        }

        val panel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(generalPanel)
            add(Box.createVerticalStrut(10))
            add(animationPanel)
            add(Box.createVerticalStrut(10))
            add(particlePanel)
            add(Box.createVerticalStrut(10))
            add(colorSettingsPanel)
            add(Box.createVerticalGlue())
        }
        mainPanel = panel

        return JBScrollPane(panel).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
    }

    private fun createTitledPanel(title: String, content: () -> JPanel): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(title)
            add(content(), BorderLayout.CENTER)
        }
    }

    private fun createSliderWithValue(slider: JBSlider, suffix: String): JPanel {
        val valueLabel = JBLabel("${slider.value}$suffix")
        slider.addChangeListener {
            valueLabel.text = "${slider.value}$suffix"
        }
        return JPanel(BorderLayout()).apply {
            add(slider, BorderLayout.CENTER)
            add(valueLabel, BorderLayout.EAST)
            valueLabel.preferredSize = Dimension(50, valueLabel.preferredSize.height)
        }
    }

    override fun isModified(): Boolean {
        val settings = SmearCursorSettings.getInstance()
        return enabledCheckbox.isSelected != settings.enabled ||
                smearWhileTypingCheckbox.isSelected != settings.smearWhileTyping ||
                smearBetweenWindowsCheckbox.isSelected != settings.smearBetweenWindows ||
                smearBetweenNeighborLinesCheckbox.isSelected != settings.smearBetweenNeighborLines ||
                smearHorizontallyCheckbox.isSelected != settings.smearHorizontally ||
                smearVerticallyCheckbox.isSelected != settings.smearVertically ||
                smearDiagonallyCheckbox.isSelected != settings.smearDiagonally ||
                stiffnessSlider.value != (settings.stiffness * 100).roundToInt() ||
                trailingStiffnessSlider.value != (settings.trailingStiffness * 100).roundToInt() ||
                dampingSlider.value != (settings.damping * 100).roundToInt() ||
                anticipationSlider.value != (settings.anticipation * 100).roundToInt() ||
                maxLengthSlider.value != settings.maxLength ||
                timeIntervalSlider.value != settings.timeInterval ||
                particlesEnabledCheckbox.isSelected != settings.particlesEnabled ||
                particleCountSlider.value != settings.particleMaxNum ||
                particleLifetimeSlider.value != settings.particleMaxLifetime ||
                useEditorColorCheckbox.isSelected != settings.useEditorCursorColor ||
                colorPanel.selectedColor?.rgb != settings.cursorColorRgb
    }

    override fun apply() {
        val settings = SmearCursorSettings.getInstance()
        settings.enabled = enabledCheckbox.isSelected
        settings.smearWhileTyping = smearWhileTypingCheckbox.isSelected
        settings.smearBetweenWindows = smearBetweenWindowsCheckbox.isSelected
        settings.smearBetweenNeighborLines = smearBetweenNeighborLinesCheckbox.isSelected
        settings.smearHorizontally = smearHorizontallyCheckbox.isSelected
        settings.smearVertically = smearVerticallyCheckbox.isSelected
        settings.smearDiagonally = smearDiagonallyCheckbox.isSelected

        settings.stiffness = stiffnessSlider.value / 100.0
        settings.trailingStiffness = trailingStiffnessSlider.value / 100.0
        settings.damping = dampingSlider.value / 100.0
        settings.anticipation = anticipationSlider.value / 100.0
        settings.maxLength = maxLengthSlider.value
        settings.timeInterval = timeIntervalSlider.value

        settings.particlesEnabled = particlesEnabledCheckbox.isSelected
        settings.particleMaxNum = particleCountSlider.value
        settings.particleMaxLifetime = particleLifetimeSlider.value

        settings.useEditorCursorColor = useEditorColorCheckbox.isSelected
        colorPanel.selectedColor?.let { settings.setCursorColor(it) }
    }

    override fun reset() {
        val settings = SmearCursorSettings.getInstance()
        enabledCheckbox.isSelected = settings.enabled
        smearWhileTypingCheckbox.isSelected = settings.smearWhileTyping
        smearBetweenWindowsCheckbox.isSelected = settings.smearBetweenWindows
        smearBetweenNeighborLinesCheckbox.isSelected = settings.smearBetweenNeighborLines
        smearHorizontallyCheckbox.isSelected = settings.smearHorizontally
        smearVerticallyCheckbox.isSelected = settings.smearVertically
        smearDiagonallyCheckbox.isSelected = settings.smearDiagonally

        stiffnessSlider.value = (settings.stiffness * 100).roundToInt()
        trailingStiffnessSlider.value = (settings.trailingStiffness * 100).roundToInt()
        dampingSlider.value = (settings.damping * 100).roundToInt()
        anticipationSlider.value = (settings.anticipation * 100).roundToInt()
        maxLengthSlider.value = settings.maxLength
        timeIntervalSlider.value = settings.timeInterval

        particlesEnabledCheckbox.isSelected = settings.particlesEnabled
        particleCountSlider.value = settings.particleMaxNum
        particleLifetimeSlider.value = settings.particleMaxLifetime

        useEditorColorCheckbox.isSelected = settings.useEditorCursorColor
        colorPanel.selectedColor = settings.getCursorColor()
    }
}

private class JBScrollPane(view: JComponent) : JScrollPane(view) {
    init {
        border = null
        horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED
        verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
    }
}
