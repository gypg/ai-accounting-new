package com.example.aiaccounting.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design Token — 全局统一的设计变量
 * 所有 Screen/Component 应引用此文件中的常量，禁止硬编码 dp/ms 值
 */

// ==================== 间距 (Spacing) ====================
object Spacing {
    val xxxs: Dp = 2.dp
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp // 加大留白，Exaggerated Minimalism
    val xl: Dp = 32.dp
    val xxl: Dp = 48.dp
    val xxxl: Dp = 64.dp // 增加更大的间距尺度

    /** 页面内边距 - 增加负空间，呼吸感增强 */
    val screenHorizontal: Dp = 24.dp
    val screenVertical: Dp = 24.dp

    /** 卡片内边距 */
    val cardPadding: Dp = 20.dp // 从16提升至20，让内容更松弛

    /** 列表项间距 */
    val listItemGap: Dp = 12.dp // 从8提升至12

    /** 区块间距 */
    val sectionGap: Dp = 32.dp // 从16大幅提升至32，区分信息层级
}

// ==================== 圆角 (Radius) ====================
object Radius {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp // 增大圆角，从12 -> 16
    val lg: Dp = 24.dp // 从16 -> 24
    val xl: Dp = 32.dp // 从24 -> 32
    val full: Dp = 999.dp
}

object Shapes {
    val card = RoundedCornerShape(Radius.md)
    val button = RoundedCornerShape(Radius.sm)
    val chip = RoundedCornerShape(Radius.xl)
    val dialog = RoundedCornerShape(Radius.lg)
    val bottomSheet = RoundedCornerShape(topStart = Radius.lg, topEnd = Radius.lg)
    val input = RoundedCornerShape(Radius.sm)
    val tag = RoundedCornerShape(Radius.xs)
}

// ==================== 阴影 (Elevation) ====================
object Elevation {
    val none: Dp = 0.dp
    val xs: Dp = 1.dp
    val sm: Dp = 2.dp
    val md: Dp = 4.dp
    val lg: Dp = 8.dp
    val xl: Dp = 16.dp

    val card: Dp = sm
    val dialog: Dp = lg
    val bottomSheet: Dp = xl
    val fab: Dp = md
    val appBar: Dp = none
}

// ==================== 动效 (Motion) ====================
object Motion {
    /** 快速交互反馈 (按钮点击、开关切换) */
    const val durationFast: Int = 150

    /** 标准过渡 (卡片展开、页面切换) */
    const val durationMedium: Int = 300

    /** 慢速动画 (复杂页面转场、图表绘制) */
    const val durationSlow: Int = 500

    /** Material 3 标准缓动曲线 */
    val easeInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val easeOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val easeIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
}

// ==================== 尺寸 (Size) ====================
object Size {
    /** 图标尺寸 */
    val iconXs: Dp = 16.dp
    val iconSm: Dp = 20.dp
    val iconMd: Dp = 24.dp
    val iconLg: Dp = 32.dp
    val iconXl: Dp = 48.dp

    /** 头像尺寸 */
    val avatarSm: Dp = 32.dp
    val avatarMd: Dp = 40.dp
    val avatarLg: Dp = 56.dp
    val avatarXl: Dp = 80.dp

    /** 按钮高度 */
    val buttonHeight: Dp = 48.dp
    val buttonHeightSm: Dp = 36.dp

    /** 底部导航栏高度 */
    val bottomNavHeight: Dp = 80.dp

    /** 分割线粗细 */
    val divider: Dp = 1.dp

    /** 最小触摸目标 (无障碍) */
    val minTouchTarget: Dp = 48.dp
}
