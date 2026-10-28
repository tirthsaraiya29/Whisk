package com.whisk.ui.components.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * State holder for iOS-inspired fluid jelly physics.
 * Manages dual leading/trailing edge spring animations for volume-preserving
 * stretch & squash deformation and gesture drag tracking.
 */
@Stable
class JellyLensState(
    initialIndex: Int,
    tabWidthPx: Float
) {
    var targetIndex by mutableIntStateOf(initialIndex)
    var tabWidthPx by mutableFloatStateOf(tabWidthPx)

    // Dual Edge Animatables (Pixels)
    val leftEdgeAnim: Animatable<Float, AnimationVector1D> = Animatable(initialIndex * tabWidthPx)
    val rightEdgeAnim: Animatable<Float, AnimationVector1D> = Animatable((initialIndex + 1) * tabWidthPx)

    // Current derived geometric state
    val leftEdge: Float get() = leftEdgeAnim.value
    val rightEdge: Float get() = rightEdgeAnim.value
    val currentWidth: Float get() = (rightEdge - leftEdge).coerceAtLeast(10f)

    // Center position offset for the lens container
    val lensOffsetPx: Float get() = leftEdge

    // Liquid volume preservation squash & stretch scaling
    val scaleX: Float
        get() {
            val nominal = if (tabWidthPx > 0f) tabWidthPx else 1f
            val rawScale = currentWidth / nominal
            return rawScale.coerceIn(0.65f, 1.6f)
        }

    val scaleY: Float
        get() {
            val sx = scaleX
            if (sx <= 0f) return 1f
            val rawScale = 1.0f / sqrt(sx)
            return rawScale.coerceIn(0.65f, 1.25f)
        }

    /**
     * Animate to target index using asymmetrical spring physics.
     * The leading edge springs fast while the trailing edge lags behind to elongate the jelly blob.
     */
    suspend fun animateToTab(newIndex: Int, widthPx: Float) {
        if (widthPx <= 0f) return
        tabWidthPx = widthPx
        val currentIndex = targetIndex
        targetIndex = newIndex

        val targetLeft = newIndex * widthPx
        val targetRight = (newIndex + 1) * widthPx

        val movingRight = newIndex >= currentIndex

        // Leading edge: Bouncy spring for overshoot & speed
        val leadingSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        // Trailing edge: Slower spring to create liquid drag / stretch
        val trailingSpring = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        coroutineScope {
            if (movingRight) {
                // Right edge is leading, Left edge is trailing
                launch { rightEdgeAnim.animateTo(targetRight, animationSpec = leadingSpring) }
                launch { leftEdgeAnim.animateTo(targetLeft, animationSpec = trailingSpring) }
            } else {
                // Left edge is leading, Right edge is trailing
                launch { leftEdgeAnim.animateTo(targetLeft, animationSpec = leadingSpring) }
                launch { rightEdgeAnim.animateTo(targetRight, animationSpec = trailingSpring) }
            }
        }
    }

    /**
     * Instantly snap during drag gesture, stretching according to drag velocity/delta.
     */
    suspend fun snapDrag(dragDeltaPx: Float, maxIndex: Int) {
        if (tabWidthPx <= 0f) return
        val currentLeft = leftEdgeAnim.value + dragDeltaPx
        val currentRight = rightEdgeAnim.value + dragDeltaPx * 1.25f // Leading drag stretch

        val minBound = -tabWidthPx * 0.2f
        val maxBound = (maxIndex + 1.2f) * tabWidthPx

        val clampedLeft = currentLeft.coerceIn(minBound, maxBound)
        val clampedRight = currentRight.coerceIn(minBound + 20f, maxBound + tabWidthPx)

        coroutineScope {
            launch { leftEdgeAnim.snapTo(clampedLeft) }
            launch { rightEdgeAnim.snapTo(clampedRight) }
        }
    }
}

@Composable
fun rememberJellyLensState(
    initialIndex: Int,
    tabWidthPx: Float
): JellyLensState {
    val state = remember { JellyLensState(initialIndex, tabWidthPx) }
    LaunchedEffect(initialIndex, tabWidthPx) {
        if (tabWidthPx > 0f) {
            state.animateToTab(initialIndex, tabWidthPx)
        }
    }
    return state
}

/**
 * Applies fluid jelly volume stretch & squash transformation to a composable.
 */
fun Modifier.jellyTransform(state: JellyLensState): Modifier = this.graphicsLayer {
    scaleX = state.scaleX
    scaleY = state.scaleY
    transformOrigin = TransformOrigin(0.5f, 0.5f)
}

/**
 * Applies fluid jelly press reaction (elastic scale down and spring recoil).
 */
@Composable
fun Modifier.jellyPressEffect(
    interactionSource: InteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "JellyPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}


