package app.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.launcher.model.LauncherSettings
import app.launcher.model.SwipeDirection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Phase 4 gestures (#26, #29, #88, #89, #90, #91).
 *
 * Double-tap vs triple-tap needs a short wait: the second tap fires
 * a pending double-tap job, and a third tap inside the window
 * cancels it and fires the triple-tap instead. The window is the
 * SYSTEM double-tap timeout (ViewConfiguration, 300 ms — rule 56),
 * not a hand-rolled value.
 *
 * Multi-finger gestures run in one awaitEachGesture loop: two
 * pointers down together track their distance (pinch in/out,
 * #88), their shared displacement (two-finger swipe, #90) and a
 * quick two-tap (two-finger double-tap, #90). Two-pointer moves
 * are consumed here, so a two-finger swipe runs the mapped action
 * and does NOT also scroll a pager page; a pinch that already
 * fired does not also register as a two-finger tap.
 *
 * Single-finger swipe up / down stays in HomeScreen (the existing
 * detectVerticalDragGestures), and long-press on empty space stays
 * on combinedClickable — this file only adds what was missing.
 */
private val TAP_WINDOW_MS: Long
    get() = android.view.ViewConfiguration.getDoubleTapTimeout().toLong()
private const val TWO_FINGER_TAP_WINDOW_MS = 500L
private const val PINCH_IN_RATIO = 0.75f
private const val PINCH_OUT_RATIO = 1.33f
private const val SWIPE_MIN_PX = 120f

/** Double-tap / triple-tap on empty home space (#26, #89, #91).
 *  The offset of the last tap is passed through so the caller can
 *  hit-test it against the icons: a double-tap on an app icon must
 *  not also fire the mapped action (its default is Lock device).
 *  Tap counting is done here, not via detectTapGestures'
 *  onDoubleTap — that callback only fires per tap PAIR, so a plain
 *  three-tap would never reach the triple-tap branch. */
@Composable
fun Modifier.doubleTripleTap(
    enabled: Boolean,
    onDoubleTap: (Offset) -> Unit,
    onTripleTap: (Offset) -> Unit,
): Modifier {
    if (!enabled) return this
    val scope = rememberCoroutineScope()
    var pendingJob by remember { mutableStateOf<Job?>(null) }
    var tapCount by remember { mutableStateOf(0) }
    var lastOffset by remember { mutableStateOf(Offset.Zero) }
    return this.pointerInput(onDoubleTap, onTripleTap) {
        detectTapGestures(
            onTap = { offset ->
                tapCount++
                lastOffset = offset
                if (pendingJob == null) {
                    pendingJob = scope.launch {
                        delay(TAP_WINDOW_MS)
                        val count = tapCount
                        val pos = lastOffset
                        tapCount = 0
                        pendingJob = null
                        when {
                            count == 2 -> onDoubleTap(pos)
                            count >= 3 -> onTripleTap(pos)
                        }
                    }
                }
            },
        )
    }
}

/** Holds the two-finger tap timing across gestures. */
private class TwoFingerTapState {
    var lastTapMillis: Long = 0L
}

/**
 * Multi-finger gestures (#29, #88, #90): pinch in/out, two-finger
 * swipe, two-finger double-tap. Fires each action at most once per
 * gesture.
 */
@Composable
fun Modifier.multiFingerGestures(
    enabled: Boolean,
    onPinchIn: () -> Unit,
    onPinchOut: () -> Unit,
    onTwoFingerSwipe: (SwipeDirection) -> Unit,
    onTwoFingerDoubleTap: () -> Unit,
): Modifier {
    if (!enabled) return this
    val tapState = remember { TwoFingerTapState() }
    return this.pointerInput(onPinchIn, onPinchOut, onTwoFingerSwipe, onTwoFingerDoubleTap) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val downAt = System.currentTimeMillis()
            val startPositions = HashMap<Long, Offset>()
            startPositions[firstDown.id.value] = firstDown.position
            var maxPointers = 1
            var pinchInFired = false
            var pinchOutFired = false
            var startDistance = 0f
            var centroidStart = firstDown.position
            var centroidNow = firstDown.position
            var gestureDone = false
            while (!gestureDone) {
                val event = awaitPointerEvent()
                val down = event.changes.filter { it.pressed }
                maxPointers = maxOf(maxPointers, down.size)
                for (change in event.changes) {
                    when (event.type) {
                        PointerEventType.Press -> {
                            startPositions[change.id.value] = change.position
                            if (down.size == 2) {
                                val points = down.map { it.position }
                                startDistance = (points[0] - points[1]).getDistance()
                                centroidStart = Offset(
                                    (points[0].x + points[1].x) / 2f,
                                    (points[0].y + points[1].y) / 2f,
                                )
                                centroidNow = centroidStart
                            }
                        }
                        PointerEventType.Move -> {
                            if (down.size == 2) {
                                // Consume the two-pointer move so the
                                // pager underneath does not also
                                // scroll a page for the same gesture.
                                for (change in event.changes) change.consume()
                                val points = down.map { it.position }
                                val distance = (points[0] - points[1]).getDistance()
                                if (startDistance > 0f) {
                                    val ratio = distance / startDistance
                                    if (!pinchInFired && ratio < PINCH_IN_RATIO) {
                                        pinchInFired = true
                                        onPinchIn()
                                    } else if (!pinchOutFired && ratio > PINCH_OUT_RATIO) {
                                        pinchOutFired = true
                                        onPinchOut()
                                    }
                                }
                                centroidNow = Offset(
                                    (points[0].x + points[1].x) / 2f,
                                    (points[0].y + points[1].y) / 2f,
                                )
                            }
                        }
                        PointerEventType.Release -> {
                            if (down.isEmpty()) {
                                val duration = System.currentTimeMillis() - downAt
                                if (maxPointers == 2) {
                                    val delta = centroidNow - centroidStart
                                    val moved = delta.getDistance()
                                    // A pinch that already fired is not
                                    // also a two-finger tap.
                                    if (moved < 40f && duration < 400L &&
                                        !pinchInFired && !pinchOutFired
                                    ) {
                                        // Two-finger tap: two in quick
                                        // succession is a double-tap.
                                        val now = System.currentTimeMillis()
                                        if (now - tapState.lastTapMillis < TWO_FINGER_TAP_WINDOW_MS) {
                                            tapState.lastTapMillis = 0L
                                            onTwoFingerDoubleTap()
                                        } else {
                                            tapState.lastTapMillis = now
                                        }
                                    } else if (moved > SWIPE_MIN_PX) {
                                        val direction = if (abs(delta.y) > abs(delta.x)) {
                                            if (delta.y < 0) SwipeDirection.UP else SwipeDirection.DOWN
                                        } else {
                                            if (delta.x < 0) SwipeDirection.LEFT else SwipeDirection.RIGHT
                                        }
                                        onTwoFingerSwipe(direction)
                                    }
                                }
                                gestureDone = true
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

/**
 * #31 / #128 / #129 notification badge, drawn over an icon — rules
 * 49/50, decision F9: DOT ONLY. One small dot (6dp), top-trailing
 * (top-right in LTR, mirrored in RTL via Alignment.TopEnd), one
 * colour (primary), one shape, pinned identically on every icon.
 * NO numbers on icons, NO 18dp badge, NO error-red (error is for
 * errors only, rule 8). The numeric count lives ONLY in the
 * long-press menu (HomeViewModel.badgeCountFor). The dot clears
 * when the app is opened (the ViewModel records the open time and
 * the caller filters the counts). A folder shows a dot if any child
 * is badged (FolderTile takes the same dot).
 *
 * `largeIcon` (dynamic-badge artwork, #128) and the badge style /
 * position / size settings are accepted for signature compat and
 * IGNORED — the dot is the only badge on any surface. Per-app off
 * is honoured by the caller filtering `count` to 0.
 */
@Composable
fun androidx.compose.foundation.layout.BoxScope.NotificationBadge(
    count: Int,
    largeIcon: android.graphics.Bitmap?,
    settings: LauncherSettings,
    modifier: Modifier = Modifier,
) {
    if (!settings.badgesEnabled || count <= 0) return
    Box(
        modifier = modifier
            .align(Alignment.TopEnd)
            .size(6.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}
