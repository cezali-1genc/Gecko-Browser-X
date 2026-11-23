package com.example.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import kotlin.math.abs
import kotlin.math.roundToInt

class GeckoViewportController {
    internal var geckoView: GeckoView? = null

    fun captureScreenshot(onResult: (Bitmap?) -> Unit) {
        val view = geckoView
        if (view == null) {
            onResult(null)
            return
        }
        runCatching {
            view.capturePixels().accept(
                { bitmap -> onResult(bitmap) },
                { onResult(null) }
            )
        }.onFailure { onResult(null) }
    }
}

@Composable
fun GeckoViewport(
    tabId: String,
    geckoSession: GeckoSession?,
    controller: GeckoViewportController,
    isAtTop: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    edgeGesturesEnabled: Boolean,
    pullToRefreshEnabled: Boolean,
    onSwipeBack: () -> Unit,
    onSwipeForward: () -> Unit,
    onPullToRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (geckoSession == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("geckoview_viewport_pending"),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    var attachedView by remember { mutableStateOf<GeckoView?>(null) }
    var leftDragPx by remember { mutableFloatStateOf(0f) }
    var rightDragPx by remember { mutableFloatStateOf(0f) }
    var pullDownPx by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val edgeZonePx = with(density) { 28.dp.toPx() }
    val swipeThresholdPx = with(density) { 84.dp.toPx() }
    val pullThresholdPx = with(density) { 78.dp.toPx() }

    DisposableEffect(attachedView, tabId, geckoSession) {
        val view = attachedView
        controller.geckoView = view
        if (view != null && view.session !== geckoSession) {
            view.releaseSession()
            view.setSession(geckoSession)
        }
        onDispose {
            if (controller.geckoView === view) {
                controller.geckoView = null
            }
            if (view != null && view.session === geckoSession) {
                view.releaseSession()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(
                tabId,
                isAtTop,
                canGoBack,
                canGoForward,
                edgeGesturesEnabled,
                pullToRefreshEnabled
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startX = down.position.x
                    val startY = down.position.y
                    val width = size.width.toFloat()

                    val inLeftEdge = edgeGesturesEnabled && canGoBack && startX <= edgeZonePx
                    val inRightEdge = edgeGesturesEnabled && canGoForward && startX >= (width - edgeZonePx)
                    var trackingPull = pullToRefreshEnabled && isAtTop && !inLeftEdge && !inRightEdge

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (inLeftEdge && leftDragPx >= swipeThresholdPx) {
                                onSwipeBack()
                            } else if (inRightEdge && rightDragPx >= swipeThresholdPx) {
                                onSwipeForward()
                            } else if (trackingPull && pullDownPx >= pullThresholdPx) {
                                onPullToRefresh()
                            }
                            leftDragPx = 0f
                            rightDragPx = 0f
                            pullDownPx = 0f
                            break
                        }

                        val dx = change.position.x - startX
                        val dy = change.position.y - startY

                        if (inLeftEdge && dx > 12f && abs(dx) > abs(dy)) {
                            leftDragPx = dx.coerceIn(0f, swipeThresholdPx * 1.4f)
                        } else if (inRightEdge && dx < -12f && abs(dx) > abs(dy)) {
                            rightDragPx = (-dx).coerceIn(0f, swipeThresholdPx * 1.4f)
                        } else if (trackingPull) {
                            if (dy < -12f || (abs(dx) > abs(dy) && abs(dx) > 20f)) {
                                trackingPull = false
                                pullDownPx = 0f
                            } else if (dy > 16f && abs(dy) > abs(dx) * 1.4f) {
                                pullDownPx = (dy * 0.65f).coerceIn(0f, pullThresholdPx * 1.4f)
                            }
                        }
                    }
                }
            }
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("geckoview_viewport"),
            factory = { context ->
                GeckoView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isFocusable = true
                    isFocusableInTouchMode = true
                    setSession(geckoSession)
                    attachedView = this
                    controller.geckoView = this
                }
            },
            update = { view ->
                attachedView = view
                controller.geckoView = view
                if (view.session !== geckoSession) {
                    view.releaseSession()
                    view.setSession(geckoSession)
                }
            }
        )

        // Left Edge Back Indicator
        AnimatedVisibility(
            visible = leftDragPx > 14f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val ready = leftDragPx >= swipeThresholdPx
            Surface(
                modifier = Modifier
                    .offset { IntOffset((leftDragPx * 0.35f).roundToInt(), 0) }
                    .size(44.dp),
                shape = CircleShape,
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Right Edge Forward Indicator
        AnimatedVisibility(
            visible = rightDragPx > 14f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val ready = rightDragPx >= swipeThresholdPx
            Surface(
                modifier = Modifier
                    .offset { IntOffset(-(rightDragPx * 0.35f).roundToInt(), 0) }
                    .size(44.dp),
                shape = CircleShape,
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Pull-to-refresh top pill
        AnimatedVisibility(
            visible = pullDownPx > 14f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            val ready = pullDownPx >= pullThresholdPx
            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, (pullDownPx * 0.45f).roundToInt()) }
                    .size(44.dp),
                shape = CircleShape,
                color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = if (ready) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
