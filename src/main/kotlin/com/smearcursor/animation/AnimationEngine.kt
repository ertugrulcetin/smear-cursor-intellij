package com.smearcursor.animation

import com.smearcursor.settings.SmearCursorSettings
import java.awt.Point
import java.awt.geom.Point2D
import kotlin.math.*

/**
 * Animation engine implementing spring physics for cursor movement.
 * This is the core animation system that mirrors animation.lua from the Neovim plugin.
 */
class AnimationEngine {

    companion object {
        private const val BASE_TIME_INTERVAL = 17.0 // Base timing in milliseconds (60 FPS)
    }

    // Animation state
    private var animating = false
    private var previousTime = 0L
    private var lag = 0.0

    // Cursor position tracking (in pixel coordinates)
    private var targetPosition = doubleArrayOf(0.0, 0.0)
    
    // Quad corners: represents the smear shape
    // Corner indices: 0=top-left, 1=top-right, 2=bottom-right, 3=bottom-left
    private val currentCorners = Array(4) { doubleArrayOf(0.0, 0.0) }
    private val targetCorners = Array(4) { doubleArrayOf(0.0, 0.0) }
    private val velocityCorners = Array(4) { doubleArrayOf(0.0, 0.0) }
    private val stiffnesses = doubleArrayOf(0.0, 0.0, 0.0, 0.0)

    // Particle system
    private val particles = mutableListOf<Particle>()
    private var previousCenter = doubleArrayOf(0.0, 0.0)

    // Cursor dimensions (in pixels)
    private var cursorWidth = 8.0
    private var cursorHeight = 16.0

    /**
     * Data class representing a particle in the particle system.
     */
    data class Particle(
        var position: DoubleArray,
        var velocity: DoubleArray,
        var lifetime: Double
    )

    /**
     * Animation frame result containing all rendering data.
     */
    data class AnimationFrame(
        val corners: Array<DoubleArray>,
        val particles: List<Particle>,
        val targetPosition: DoubleArray,
        val isAnimating: Boolean,
        val headIndex: Int,
        val tailIndex: Int,
        val gradientOrigin: DoubleArray,
        val gradientDirection: DoubleArray
    )

    /**
     * Initialize the animation engine with cursor dimensions.
     */
    fun initialize(width: Double, height: Double) {
        cursorWidth = width
        cursorHeight = height
    }

    /**
     * Set corners based on cursor position and dimensions.
     */
    private fun setCorners(corners: Array<DoubleArray>, x: Double, y: Double) {
        // Top-left
        corners[0][0] = y
        corners[0][1] = x
        // Top-right
        corners[1][0] = y
        corners[1][1] = x + cursorWidth
        // Bottom-right
        corners[2][0] = y + cursorHeight
        corners[2][1] = x + cursorWidth
        // Bottom-left
        corners[3][0] = y + cursorHeight
        corners[3][1] = x
    }

    /**
     * Reset all velocities to zero.
     */
    private fun resetVelocity() {
        for (i in 0..3) {
            velocityCorners[i][0] = 0.0
            velocityCorners[i][1] = 0.0
        }
    }

    /**
     * Set initial velocity based on anticipation (opposite to movement direction).
     */
    private fun setInitialVelocity() {
        val settings = SmearCursorSettings.getInstance()
        for (i in 0..3) {
            for (j in 0..1) {
                velocityCorners[i][j] = (currentCorners[i][j] - targetCorners[i][j]) * settings.anticipation
            }
        }
    }

    /**
     * Get the center point of a set of corners.
     */
    private fun getCenter(corners: Array<DoubleArray>): DoubleArray {
        return doubleArrayOf(
            (corners[0][0] + corners[1][0] + corners[2][0] + corners[3][0]) / 4.0,
            (corners[0][1] + corners[1][1] + corners[2][1] + corners[3][1]) / 4.0
        )
    }

    /**
     * Jump cursor immediately to new position without animation.
     */
    fun jump(x: Double, y: Double) {
        targetPosition = doubleArrayOf(y, x)
        setCorners(targetCorners, x, y)
        setCorners(currentCorners, x, y)
        previousCenter = getCenter(currentCorners)
        resetVelocity()
        particles.clear()
        animating = false
        previousTime = 0L
        lag = 0.0
    }

    /**
     * Start animation towards a new target position.
     */
    fun animateTo(x: Double, y: Double) {
        val settings = SmearCursorSettings.getInstance()
        
        // Check minimum distances
        val currentX = currentCorners[0][1]
        val currentY = currentCorners[0][0]
        val dx = abs(x - currentX)
        val dy = abs(y - currentY)

        if (dy < settings.minVerticalDistanceSmear * cursorHeight && 
            dx < settings.minHorizontalDistanceSmear * cursorWidth) {
            jump(x, y)
            return
        }

        // Check direction restrictions
        if (!settings.smearHorizontally && dy <= cursorHeight / 2) {
            jump(x, y)
            return
        }
        if (!settings.smearVertically && dx <= cursorWidth / 2) {
            jump(x, y)
            return
        }
        if (!settings.smearDiagonally && dy > cursorHeight / 2 && dx > cursorWidth / 2) {
            jump(x, y)
            return
        }

        targetPosition = doubleArrayOf(y, x)
        setCorners(targetCorners, x, y)
        setStiffnesses()
        
        if (!animating) {
            setInitialVelocity()
        }
        
        animating = true
    }

    /**
     * Calculate stiffness values for each corner based on distance from target.
     */
    private fun setStiffnesses() {
        val settings = SmearCursorSettings.getInstance()
        val targetCenter = getCenter(targetCorners)
        val distances = DoubleArray(4)
        var minDistance = Double.MAX_VALUE
        var maxDistance = 0.0

        val headStiffness = settings.stiffness
        val trailingStiffness = settings.trailingStiffness
        val trailingExponent = settings.trailingExponent

        for (i in 0..3) {
            val distance = sqrt(
                (currentCorners[i][0] - targetCenter[0]).pow(2.0) +
                (currentCorners[i][1] - targetCenter[1]).pow(2.0)
            )
            minDistance = min(minDistance, distance)
            maxDistance = max(maxDistance, distance)
            distances[i] = distance
        }

        if (maxDistance == minDistance) {
            for (i in 0..3) {
                stiffnesses[i] = headStiffness
            }
            return
        }

        for (i in 0..3) {
            val x = (distances[i] - minDistance) / (maxDistance - minDistance)
            val stiffness = headStiffness + (trailingStiffness - headStiffness) * x.pow(trailingExponent)
            stiffnesses[i] = min(1.0, stiffness)
        }
    }

    /**
     * Perform one animation update step.
     * Returns the current animation frame data for rendering.
     */
    fun update(): AnimationFrame? {
        if (!animating) return null

        val settings = SmearCursorSettings.getInstance()
        val currentTime = System.nanoTime() / 1_000_000L
        
        val timeInterval = if (previousTime == 0L) {
            previousTime = currentTime
            BASE_TIME_INTERVAL
        } else {
            val elapsed = (currentTime - previousTime).toDouble()
            previousTime = currentTime
            elapsed
        }

        // Calculate physics
        val speedCorrection = timeInterval / BASE_TIME_INTERVAL
        val damping = settings.damping
        val velocityConservationFactor = exp(ln(1.0 - damping) * speedCorrection)
        val dampingCorrectionFactor = 1.0 / (1.0 + 2.5 * velocityConservationFactor)

        var distanceHeadToTargetSquared = Double.MAX_VALUE
        var distanceTailToTargetSquared = 0.0
        var indexHead = 0
        var indexTail = 0

        // Update each corner
        for (i in 0..3) {
            val distanceSquared = (currentCorners[i][0] - targetCorners[i][0]).pow(2.0) +
                                  (currentCorners[i][1] - targetCorners[i][1]).pow(2.0)
            
            val stiffness = 1.0 - exp(ln(1.0 - stiffnesses[i] * dampingCorrectionFactor) * speedCorrection)

            if (distanceSquared < distanceHeadToTargetSquared) {
                distanceHeadToTargetSquared = distanceSquared
                indexHead = i
            }
            if (distanceSquared > distanceTailToTargetSquared) {
                distanceTailToTargetSquared = distanceSquared
                indexTail = i
            }

            for (j in 0..1) {
                velocityCorners[i][j] += (targetCorners[i][j] - currentCorners[i][j]) * stiffness
                currentCorners[i][j] += velocityCorners[i][j]
                velocityCorners[i][j] *= velocityConservationFactor
            }
        }

        // Limit smear length
        var smearLength = 0.0
        for (i in 0..3) {
            if (i != indexHead) {
                val distance = sqrt(
                    (currentCorners[i][0] - currentCorners[indexHead][0]).pow(2.0) +
                    (currentCorners[i][1] - currentCorners[indexHead][1]).pow(2.0)
                )
                smearLength = max(smearLength, distance)
            }
        }

        val maxLength = settings.maxLength * cursorWidth
        if (smearLength > maxLength) {
            val factor = maxLength / smearLength
            for (i in 0..3) {
                if (i != indexHead) {
                    for (j in 0..1) {
                        currentCorners[i][j] = currentCorners[indexHead][j] +
                                (currentCorners[i][j] - currentCorners[indexHead][j]) * factor
                    }
                }
            }
        }

        // Update particles
        if (settings.particlesEnabled) {
            updateParticles(timeInterval)
        }

        // Check if animation should stop
        var maxDistance = 0.0
        var maxVelocity = 0.0
        for (i in 0..3) {
            val distance = sqrt(
                (currentCorners[i][0] - targetCorners[i][0]).pow(2.0) +
                (currentCorners[i][1] - targetCorners[i][1]).pow(2.0)
            )
            val velocity = sqrt(velocityCorners[i][0].pow(2.0) + velocityCorners[i][1].pow(2.0))
            maxDistance = max(maxDistance, distance)
            maxVelocity = max(maxVelocity, velocity)
        }

        val stopThreshold = settings.distanceStopAnimating * cursorWidth
        if (maxDistance <= stopThreshold && maxVelocity <= stopThreshold && particles.isEmpty()) {
            setCorners(currentCorners, targetPosition[1], targetPosition[0])
            resetVelocity()
            animating = false
            previousTime = 0L
            lag = 0.0
        }

        // Calculate gradient direction
        val gradientOrigin = doubleArrayOf(currentCorners[indexHead][0], currentCorners[indexHead][1])
        val gradientDirection = doubleArrayOf(
            currentCorners[indexTail][0] - currentCorners[indexHead][0],
            currentCorners[indexTail][1] - currentCorners[indexHead][1]
        )
        val gradientLengthSquared = gradientDirection[0].pow(2.0) + gradientDirection[1].pow(2.0)
        if (gradientLengthSquared > 1) {
            gradientDirection[0] /= gradientLengthSquared
            gradientDirection[1] /= gradientLengthSquared
        } else {
            gradientDirection[0] = 0.0
            gradientDirection[1] = 0.0
        }

        return AnimationFrame(
            corners = currentCorners.map { it.clone() }.toTypedArray(),
            particles = particles.map { it.copy(position = it.position.clone(), velocity = it.velocity.clone()) },
            targetPosition = targetPosition.clone(),
            isAnimating = animating,
            headIndex = indexHead,
            tailIndex = indexTail,
            gradientOrigin = gradientOrigin,
            gradientDirection = gradientDirection
        )
    }

    /**
     * Update particle positions and lifetimes.
     */
    private fun updateParticles(timeInterval: Double) {
        val settings = SmearCursorSettings.getInstance()
        val speedCorrection = timeInterval / BASE_TIME_INTERVAL
        val velocityConservationFactor = exp(ln(1.0 - settings.particleDamping) * speedCorrection)

        // Update existing particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.lifetime -= timeInterval

            if (particle.lifetime <= 0) {
                iterator.remove()
            } else {
                // Apply gravity and damping
                particle.velocity[0] = (particle.velocity[0] + settings.particleGravity * (settings.timeInterval / 1000.0)) * velocityConservationFactor
                particle.velocity[1] *= velocityConservationFactor

                // Update position
                particle.position[0] += particle.velocity[0] * (settings.timeInterval / 1000.0)
                particle.position[1] += particle.velocity[1] * (settings.timeInterval / 1000.0)
            }
        }

        // Add new particles based on cursor movement
        val center = getCenter(currentCorners)
        val movement = doubleArrayOf(
            center[0] - previousCenter[0],
            center[1] - previousCenter[1]
        )
        val movementMagnitude = sqrt(movement[0].pow(2.0) + movement[1].pow(2.0))

        if (movementMagnitude > cursorWidth * 0.1) {
            var numNewParticles = (settings.particlesPerSecond * (settings.timeInterval / 1000.0) +
                    movementMagnitude / cursorWidth * settings.particlesPerLength).toInt()
            numNewParticles = min(numNewParticles, settings.particleMaxNum - particles.size)

            for (i in 0 until numNewParticles) {
                val s = Math.random()
                val particlePosition = doubleArrayOf(
                    previousCenter[0] + s * movement[0] + (Math.random() - 0.5) * settings.particleSpread * cursorHeight,
                    previousCenter[1] + s * movement[1] + (Math.random() - 0.5) * settings.particleSpread * cursorWidth
                )

                val velocityMagnitude = settings.particleMaxInitialVelocity * cursorWidth * sqrt(Math.random())
                val velocityAngle = Math.random() * 2 * PI
                val particleVelocity = doubleArrayOf(
                    velocityMagnitude * cos(velocityAngle),
                    velocityMagnitude * sin(velocityAngle)
                )

                particles.add(Particle(
                    position = particlePosition,
                    velocity = particleVelocity,
                    lifetime = settings.particleMaxLifetime * Math.random()
                ))
            }
        }

        previousCenter = center
    }

    /**
     * Check if animation is currently running.
     */
    fun isAnimating(): Boolean = animating

    /**
     * Stop animation immediately.
     */
    fun stopAnimation() {
        animating = false
        previousTime = 0L
        lag = 0.0
        particles.clear()
    }

    /**
     * Get the time interval for the next frame.
     */
    fun getFrameInterval(): Int {
        return SmearCursorSettings.getInstance().timeInterval
    }
}
