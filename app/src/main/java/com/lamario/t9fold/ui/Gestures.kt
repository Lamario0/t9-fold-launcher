package com.lamario.t9fold.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sign

/*
 * Drags are watched in the Initial pass, i.e. before the children see them. That lets a swipe
 * that starts on a key, or on a real Android widget (which would otherwise swallow the touch),
 * still turn the page or pull down the notification shade. Once a drag is claimed its events are
 * consumed, which cancels the key press or the widget's own touch handling underneath.
 */
private suspend fun PointerInputScope.detectInterceptedDrag(
    horizontal: Boolean,
    canClaim: (delta: Float) -> Boolean,
    onDrag: (delta: Float) -> Unit,
    onEnd: (velocity: Float) -> Unit,
) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var claimed = false
        var total = Offset.Zero
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            // An outer detector already took this drag.
            if (!claimed && change.isConsumed) break
            tracker.addPosition(change.uptimeMillis, change.position)
            if (!change.pressed) {
                if (claimed) {
                    val v = tracker.calculateVelocity()
                    onEnd(if (horizontal) v.x else v.y)
                }
                break
            }
            val delta = change.positionChangeIgnoreConsumed()
            if (claimed) {
                onDrag(if (horizontal) delta.x else delta.y)
                change.consume()
                continue
            }
            total += delta
            val main = if (horizontal) total.x else total.y
            val cross = if (horizontal) total.y else total.x
            if (abs(main) > slop && abs(main) > abs(cross) * 1.2f) {
                if (!canClaim(main)) break
                claimed = true
                onDrag(main - sign(main) * slop)
                change.consume()
            } else if (abs(cross) > slop) {
                break
            }
        }
    }
}

/** Horizontal paging driven by us rather than the pager, so swipes over widgets work too. */
@Composable
fun Modifier.pagerSwipe(pager: PagerState): Modifier {
    val scope = rememberCoroutineScope()
    val flingThreshold = with(LocalDensity.current) { 400.dp.toPx() }
    return pointerInput(pager) {
        detectInterceptedDrag(
            horizontal = true,
            canClaim = { true },
            onDrag = { dx -> pager.dispatchRawDelta(-dx) },
            onEnd = { v ->
                val position = pager.currentPage + pager.currentPageOffsetFraction
                val target = when {
                    v < -flingThreshold -> ceil(position)
                    v > flingThreshold -> floor(position)
                    else -> position.roundToInt().toFloat()
                }.toInt().coerceIn(0, pager.pageCount - 1)
                scope.launch { pager.animateScrollToPage(target) }
            },
        )
    }
}

/** Swipe down to open the notification shade. */
@Composable
fun Modifier.swipeDownToExpand(onExpand: () -> Unit): Modifier {
    val expand by rememberUpdatedState(onExpand)
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    return pointerInput(Unit) {
        var pulled = 0f
        detectInterceptedDrag(
            horizontal = false,
            canClaim = { dy ->
                pulled = 0f
                dy > 0
            },
            onDrag = { dy ->
                pulled += dy
                if (pulled > threshold) {
                    pulled = Float.NEGATIVE_INFINITY
                    expand()
                }
            },
            onEnd = {},
        )
    }
}

/** Vertical scrolling for a widget page; pulling down while at the top opens the notification shade. */
@Composable
fun Modifier.widgetPageScroll(scroll: ScrollState, onExpand: () -> Unit): Modifier {
    val expand by rememberUpdatedState(onExpand)
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    return pointerInput(scroll) {
        var pulling = false
        var pulled = 0f
        detectInterceptedDrag(
            horizontal = false,
            canClaim = { dy ->
                pulling = dy > 0 && scroll.value == 0
                pulled = 0f
                pulling || scroll.maxValue > 0
            },
            onDrag = { dy ->
                if (pulling) {
                    pulled += dy
                    if (pulled > threshold) {
                        pulled = Float.NEGATIVE_INFINITY
                        expand()
                    }
                } else {
                    scroll.dispatchRawDelta(-dy)
                }
            },
            onEnd = { v ->
                if (!pulling) scope.launch { scroll.animateScrollBy(-v * 0.25f, tween(450, easing = LinearOutSlowInEasing)) }
            },
        )
    }
}

/** For scrolling lists: dragging down past the top of the list opens the notification shade. */
@Composable
fun rememberPullDownConnection(onExpand: () -> Unit): NestedScrollConnection {
    val expand by rememberUpdatedState(onExpand)
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    return remember(threshold) {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > threshold) {
                        pulled = Float.NEGATIVE_INFINITY
                        expand()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }
}
