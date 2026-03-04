package com.example.aiaccounting.ui.animation

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 动画工具类
 * 提供常用的动画效果
 */

/**
 * 淡入淡出动画
 */
@Composable
fun FadeInOutAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 缩放动画
 */
@Composable
fun ScaleAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(300, easing = EaseOutQuart)
        ),
        exit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(300, easing = EaseInQuart)
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 滑动动画
 */
@Composable
fun SlideInAnimation(
    visible: Boolean,
    direction: SlideDirection = SlideDirection.UP,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val enter = when (direction) {
        SlideDirection.UP -> slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = EaseOutQuart)
        )
        SlideDirection.DOWN -> slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = EaseOutQuart)
        )
        SlideDirection.LEFT -> slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(300, easing = EaseOutQuart)
        )
        SlideDirection.RIGHT -> slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(300, easing = EaseOutQuart)
        )
    }

    val exit = when (direction) {
        SlideDirection.UP -> slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300, easing = EaseInQuart)
        )
        SlideDirection.DOWN -> slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = EaseInQuart)
        )
        SlideDirection.LEFT -> slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300, easing = EaseInQuart)
        )
        SlideDirection.RIGHT -> slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(300, easing = EaseInQuart)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 列表项动画
 */
@Composable
fun <T> AnimatedListItem(
    item: T,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val delay = index * 50

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = tween(300, delayMillis = delay, easing = EaseOutQuart)
        ) + fadeIn(
            animationSpec = tween(300, delayMillis = delay)
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 脉冲动画
 */
@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (scale: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.scale(scale)
    ) {
        content(scale)
    }
}

/**
 * 摇晃动画
 */
@Composable
fun ShakeAnimation(
    shake: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(shake) {
        if (shake) {
            shakeAnim.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0f at 0
                    (-10f) at 50
                    10f at 100
                    (-10f) at 150
                    10f at 200
                    (-10f) at 250
                    10f at 300
                    (-5f) at 350
                    5f at 400
                    0f at 500
                }
            )
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = shakeAnim.value
        }
    ) {
        content()
    }
}

/**
 * 数字滚动动画
 */
@Composable
fun AnimatedNumber(
    number: Double,
    modifier: Modifier = Modifier,
    format: (Double) -> String = { it.toString() }
) {
    val animatedNumber by animateFloatAsState(
        targetValue = number.toFloat(),
        animationSpec = tween(500, easing = EaseOutQuart)
    )

    Text(
        text = format(animatedNumber.toDouble()),
        modifier = modifier
    )
}

/**
 * 展开/收起动画
 */
@Composable
fun ExpandCollapseAnimation(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = tween(300, easing = EaseOutQuart)
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = shrinkVertically(
            animationSpec = tween(300, easing = EaseInQuart)
        ) + fadeOut(
            animationSpec = tween(300)
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * 页面切换动画
 */
@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun PageTransition(
    currentPage: Int,
    targetPage: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val direction = if (targetPage > currentPage) 1 else -1

    AnimatedContent(
        targetState = targetPage,
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { direction * it },
                animationSpec = tween(300, easing = EaseOutQuart)
            ) + fadeIn(
                animationSpec = tween(300)
            ) with
            slideOutHorizontally(
                targetOffsetX = { -direction * it },
                animationSpec = tween(300, easing = EaseInQuart)
            ) + fadeOut(
                animationSpec = tween(300)
            )
        },
        modifier = modifier
    ) { _ ->
        content()
    }
}

/**
 * 加载动画
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationZ = rotation
            }
    ) {
        CircularProgressIndicator()
    }
}

/**
 * 呼吸动画
 */
@Composable
fun BreathingAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (alpha: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.alpha(alpha)
    ) {
        content(alpha)
    }
}

/**
 * 弹跳动画
 */
@Composable
fun BounceAnimation(
    modifier: Modifier = Modifier,
    content: @Composable (offset: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseOutBounce),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationY = offset
        }
    ) {
        content(offset)
    }
}

/**
 * 滑动方向枚举
 */
enum class SlideDirection {
    UP, DOWN, LEFT, RIGHT
}

// 自定义缓动函数
private val EaseOutQuart = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private val EaseInQuart = CubicBezierEasing(0.5f, 0f, 0.75f, 1f)
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
private val EaseOutBounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
