package com.lamario.t9fold.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lamario.t9fold.LauncherState
import com.lamario.t9fold.system.LocalPhoneStatus
import com.lamario.t9fold.system.rememberPhoneStatus
import com.lamario.t9fold.widgets.WIDGET_PAGE_COUNT
import com.lamario.t9fold.widgets.WidgetHostController
import com.lamario.t9fold.widgets.WidgetStore

/** Remembers which screen we were last drawn on, across activity re-creation, to spot an unfold. */
private object FoldMemory {
    var lastWasMain: Boolean? = null
}

@Composable
fun LauncherRoot(state: LauncherState, store: WidgetStore, widgets: WidgetHostController) {
    val status by rememberPhoneStatus()
    CompositionLocalProvider(
        LocalPhoneStatus provides status,
        LocalSkin provides state.prefs.phoneStyle.skin,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Retro.Body)) {
            // The Fold 5 cover screen is ~340dp wide; the inner screen is well over 600dp.
            val isMain = maxWidth >= 600.dp
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                if (isMain) {
                    InsideLayout(state, store, widgets)
                } else {
                    CoverLayout(state, store, widgets)
                }
            }
            UnfoldAnimation(isMain, state)

            state.widgetPickerPage?.let { page ->
                WidgetPicker(
                    widgets = widgets,
                    onRetro = {
                        widgets.addRetroWidget(page, it)
                        state.widgetPickerPage = null
                    },
                    onSystem = {
                        state.widgetPickerPage = null
                        widgets.addSystemWidget(page, it)
                    },
                    onDismiss = { state.widgetPickerPage = null },
                )
            }
        }
    }
}

/** Cover screen: swipe between the phone and the widget page. */
@Composable
private fun CoverLayout(state: LauncherState, store: WidgetStore, widgets: WidgetHostController) {
    val pager = rememberPagerState { 1 + WIDGET_PAGE_COUNT }
    LaunchedEffect(state.homeSignal) {
        if (state.homeSignal > 0) pager.animateScrollToPage(0)
    }
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            userScrollEnabled = false,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize().pagerSwipe(pager),
        ) { page ->
            if (page == 0) {
                PhoneBody(state, mirrored = false)
            } else {
                WidgetPage(page - 1, state, store, widgets)
            }
        }
        PageDots(pager, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
    }
}

/**
 * Unfolded: like a flip phone opened flat. The left half is the cover phone flipped face down
 * (mirrored). The right half opens on the office-phone speed keys; swipe for the widget page,
 * the same one the cover screen shows.
 */
@Composable
private fun InsideLayout(state: LauncherState, store: WidgetStore, widgets: WidgetHostController) {
    val pager = rememberPagerState { 1 + WIDGET_PAGE_COUNT }
    LaunchedEffect(state.homeSignal) {
        if (state.homeSignal > 0) pager.animateScrollToPage(0)
    }
    Row(Modifier.fillMaxSize()) {
        PhoneBody(state, mirrored = state.prefs.mirrorInside, modifier = Modifier.weight(1f).fillMaxHeight())
        Box(Modifier.weight(1f).fillMaxHeight()) {
            HorizontalPager(
                state = pager,
                userScrollEnabled = false,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize().pagerSwipe(pager),
            ) { page ->
                if (page == 0) {
                    DeskKeysPanel(state)
                } else {
                    WidgetPage(page - 1, state, store, widgets)
                }
            }
            PageDots(pager, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
        }
    }
}

/**
 * On unfold, the cover phone swings open from the hinge like a faceplate and lands, face down,
 * as the left half.
 */
@Composable
private fun BoxScope.UnfoldAnimation(isMain: Boolean, state: LauncherState) {
    val play = remember(isMain) {
        isMain && FoldMemory.lastWasMain == false && state.prefs.unfoldAnimation
    }
    val progress = remember(isMain) { Animatable(if (play) 0f else 1f) }
    SideEffect { FoldMemory.lastWasMain = isMain }
    LaunchedEffect(progress) {
        if (play) progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val p = progress.value
    if (p >= 1f) return
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f * (1f - p))))
    val angle = -180f * p
    if (angle > -90f) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationY = angle
                    cameraDistance = 14f * density
                },
        ) {
            PhoneBody(state, mirrored = false)
        }
    }
    // Swallow touches until the animation is done.
    Box(
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass = PointerEventPass.Initial).consume()
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            }
        },
    )
}
