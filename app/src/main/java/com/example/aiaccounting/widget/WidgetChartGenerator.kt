package com.example.aiaccounting.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * 小组件图表生成器
 * 生成简单的趋势图和饼图 Bitmap
 * 修复：添加空数据处理、坐标轴标签、更好的视觉效果
 */
object WidgetChartGenerator {

    /**
     * 生成7日趋势图
     * @param data 7天的支出数据
     * @return Base64编码的PNG图片
     */
    fun generateTrendChart(context: Context, data: List<Float>): String? {
        if (data.isEmpty()) {
            Log.w("WidgetChartGenerator", "趋势图数据为空")
            return null
        }

        // 如果所有数据都是0，也生成图表（显示为平线）
        val hasData = data.any { it > 0 }
        
        val width = 400
        val height = 200
        var bitmap: Bitmap? = null
        
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 背景 - 使用深色背景匹配小组件主题
            canvas.drawColor(Color.parseColor("#FF2D2D44"))

            // 计算数据范围
            val maxValue = if (hasData) data.maxOrNull() ?: 1f else 100f
            val minValue = 0f
            val range = (maxValue - minValue).coerceAtLeast(1f)

            // 绘制坐标轴
            val axisPaint = Paint().apply {
                color = Color.parseColor("#80FFFFFF")
                strokeWidth = 1f
                isAntiAlias = true
            }

            val padding = 40f
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            // 绘制Y轴
            canvas.drawLine(padding, padding, padding, height - padding, axisPaint)
            // 绘制X轴
            canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint)

            // 绘制Y轴刻度标签
            val textPaint = Paint().apply {
                color = Color.parseColor("#B3FFFFFF")
                textSize = 16f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            
            // 绘制最大值标签
            canvas.drawText("¥${maxValue.toInt()}", padding - 5f, padding + 5f, textPaint)
            // 绘制0值标签
            canvas.drawText("0", padding - 5f, height - padding + 5f, textPaint)

            // 绘制折线
            val linePaint = Paint().apply {
                color = Color.parseColor("#7C4DFF")
                strokeWidth = 4f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }

            val fillPaint = Paint().apply {
                color = Color.parseColor("#407C4DFF")
                style = Paint.Style.FILL
            }

            // 绘制数据点和连线
            val points = mutableListOf<Pair<Float, Float>>()
            data.forEachIndexed { index, value ->
                val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
                val y = padding + chartHeight - ((value - minValue) / range) * chartHeight
                points.add(x to y)
            }

            // 绘制填充区域
            if (points.size > 1) {
                val path = android.graphics.Path()
                path.moveTo(points.first().first, height - padding)
                points.forEach { path.lineTo(it.first, it.second) }
                path.lineTo(points.last().first, height - padding)
                path.close()
                canvas.drawPath(path, fillPaint)
            }

            // 绘制折线
            for (i in 0 until points.size - 1) {
                canvas.drawLine(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, linePaint)
            }

            // 绘制数据点
            val pointPaint = Paint().apply {
                color = Color.parseColor("#7C4DFF")
                style = Paint.Style.FILL
            }
            val pointStrokePaint = Paint().apply {
                color = Color.WHITE
                strokeWidth = 2f
                style = Paint.Style.STROKE
            }
            
            points.forEach { point ->
                canvas.drawCircle(point.first, point.second, 6f, pointPaint)
                canvas.drawCircle(point.first, point.second, 6f, pointStrokePaint)
            }

            // 绘制X轴日期标签（显示最近7天）
            val dayLabels = listOf("-6", "-5", "-4", "-3", "-2", "-1", "今天")
            val labelPaint = Paint().apply {
                color = Color.parseColor("#B3FFFFFF")
                textSize = 14f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            
            points.forEachIndexed { index, point ->
                if (index < dayLabels.size) {
                    canvas.drawText(dayLabels[index], point.first, height - 10f, labelPaint)
                }
            }

            // 转换为Base64
            return bitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("WidgetChartGenerator", "生成趋势图失败: ${e.message}", e)
            bitmap?.recycle()
            return null
        }
    }

    /**
     * 生成支出分类饼图
     * @param categories 分类名称列表
     * @param values 分类金额列表
     * @return Base64编码的PNG图片
     */
    fun generatePieChart(context: Context, categories: List<String>, values: List<Float>): String? {
        if (categories.isEmpty() || values.isEmpty()) {
            Log.w("WidgetChartGenerator", "饼图数据为空")
            return null
        }

        val total = values.sum()
        if (total == 0f) {
            Log.w("WidgetChartGenerator", "饼图总金额为0")
            return null
        }

        val width = 300
        val height = 300
        var bitmap: Bitmap? = null
        
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 背景
            canvas.drawColor(Color.parseColor("#FF2D2D44"))

            // 使用更丰富的颜色
            val colors = listOf(
                Color.parseColor("#FF5252"),  // 红色
                Color.parseColor("#448AFF"),  // 蓝色
                Color.parseColor("#69F0AE"),  // 绿色
                Color.parseColor("#FFD740"),  // 黄色
                Color.parseColor("#7C4DFF"),  // 紫色
                Color.parseColor("#FF4081"),  // 粉色
                Color.parseColor("#00BCD4"),  // 青色
                Color.parseColor("#FF9800")   // 橙色
            )

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
            }

            val rect = RectF(50f, 50f, 250f, 250f)
            var startAngle = 0f

            values.forEachIndexed { index, value ->
                val sweepAngle = (value / total) * 360f
                paint.color = colors[index % colors.size]
                canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
                startAngle += sweepAngle
            }

            // 绘制中心圆（甜甜圈效果）
            val centerPaint = Paint().apply {
                color = Color.parseColor("#FF2D2D44")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(150f, 150f, 70f, centerPaint)

            // 绘制总金额
            val totalPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("¥${total.toInt()}", 150f, 145f, totalPaint)
            
            // 绘制"总支出"标签
            val labelPaint = Paint().apply {
                color = Color.parseColor("#B3FFFFFF")
                textSize = 14f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("总支出", 150f, 165f, labelPaint)

            // 绘制图例（右侧）
            val legendPaint = Paint().apply {
                textSize = 12f
                isAntiAlias = true
            }
            val legendBoxPaint = Paint().apply {
                style = Paint.Style.FILL
            }
            
            val legendX = 260f
            var legendY = 60f
            
            categories.take(4).forEachIndexed { index, category ->
                // 绘制颜色方块
                legendBoxPaint.color = colors[index % colors.size]
                canvas.drawRect(legendX, legendY - 10f, legendX + 12f, legendY + 2f, legendBoxPaint)
                
                // 绘制分类名称（截断长名称）
                val displayName = if (category.length > 4) category.substring(0, 4) + ".." else category
                legendPaint.color = Color.parseColor("#B3FFFFFF")
                canvas.drawText(displayName, legendX + 18f, legendY, legendPaint)
                
                // 绘制百分比
                val percentage = ((values[index] / total) * 100).toInt()
                canvas.drawText("${percentage}%", legendX + 18f, legendY + 14f, legendPaint)
                
                legendY += 35f
            }

            return bitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("WidgetChartGenerator", "生成饼图失败: ${e.message}", e)
            bitmap?.recycle()
            return null
        }
    }

    /**
     * 生成空数据提示图
     */
    fun generateEmptyChart(context: Context, message: String = "暂无数据"): String? {
        val width = 400
        val height = 200
        var bitmap: Bitmap? = null
        
        try {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 背景
            canvas.drawColor(Color.parseColor("#FF2D2D44"))

            // 绘制提示文字
            val textPaint = Paint().apply {
                color = Color.parseColor("#80FFFFFF")
                textSize = 24f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(message, width / 2f, height / 2f, textPaint)

            return bitmapToBase64(bitmap)
        } catch (e: Exception) {
            Log.e("WidgetChartGenerator", "生成空数据图表失败: ${e.message}", e)
            bitmap?.recycle()
            return null
        }
    }

    /**
     * 将Bitmap转换为Base64字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        val byteArray = outputStream.toByteArray()
        bitmap.recycle() // 及时回收Bitmap
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * 从Base64字符串解码Bitmap
     */
    fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) {
            Log.w("WidgetChartGenerator", "Base64字符串为空")
            return null
        }
        
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("WidgetChartGenerator", "解码Bitmap失败: ${e.message}", e)
            null
        }
    }
}
