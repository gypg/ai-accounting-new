package com.example.aiaccounting.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 小组件图表生成器
 * 生成简单的趋势图和饼图 Bitmap
 */
object WidgetChartGenerator {

    /**
     * 生成7日趋势图
     * @param data 7天的支出数据
     * @return Base64编码的PNG图片
     */
    fun generateTrendChart(context: Context, data: List<Float>): String? {
        if (data.isEmpty()) return null

        val width = 400
        val height = 200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景
        canvas.drawColor(Color.TRANSPARENT)

        // 计算数据范围
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = 0f
        val range = maxValue - minValue

        // 绘制折线
        val paint = Paint().apply {
            color = Color.parseColor("#7C4DFF")
            strokeWidth = 4f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        val fillPaint = Paint().apply {
            color = Color.parseColor("#207C4DFF")
            style = Paint.Style.FILL
        }

        val padding = 40f
        val chartWidth = width - padding * 2
        val chartHeight = height - padding * 2

        // 绘制数据点和连线
        val points = mutableListOf<Pair<Float, Float>>()
        data.forEachIndexed { index, value ->
            val x = padding + (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * chartWidth
            val y = padding + chartHeight - ((value - minValue) / range.coerceAtLeast(1f)) * chartHeight
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
            canvas.drawLine(points[i].first, points[i].second, points[i + 1].first, points[i + 1].second, paint)
        }

        // 绘制数据点
        val pointPaint = Paint().apply {
            color = Color.parseColor("#7C4DFF")
            style = Paint.Style.FILL
        }
        points.forEach {
            canvas.drawCircle(it.first, it.second, 6f, pointPaint)
        }

        // 转换为Base64
        return bitmapToBase64(bitmap)
    }

    /**
     * 生成支出分类饼图
     * @param categories 分类名称列表
     * @param values 分类金额列表
     * @return Base64编码的PNG图片
     */
    fun generatePieChart(context: Context, categories: List<String>, values: List<Float>): String? {
        if (categories.isEmpty() || values.isEmpty()) return null

        val width = 300
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 背景
        canvas.drawColor(Color.TRANSPARENT)

        val total = values.sum()
        if (total == 0f) return null

        val colors = listOf(
            Color.parseColor("#FF5252"),
            Color.parseColor("#448AFF"),
            Color.parseColor("#69F0AE"),
            Color.parseColor("#FFD740"),
            Color.parseColor("#7C4DFF"),
            Color.parseColor("#FF4081"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#FF9800")
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
        canvas.drawCircle(150f, 150f, 60f, centerPaint)

        // 绘制总金额
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("¥${total.toInt()}", 150f, 155f, textPaint)

        return bitmapToBase64(bitmap)
    }

    /**
     * 将Bitmap转换为Base64字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * 从Base64字符串解码Bitmap
     */
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
