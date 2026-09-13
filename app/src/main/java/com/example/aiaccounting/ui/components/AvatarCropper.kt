package com.example.aiaccounting.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 头像裁剪器
 * 支持缩放、拖动、裁剪预览
 */
@Composable
fun AvatarCropper(
    imageUri: Uri,
    onCropComplete: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var isLoading by remember { mutableStateOf(false) }
    var cropProgress by remember { mutableStateOf(0f) }
    
    // 裁剪区域大小（正方形）
    val cropSize = 280.dp
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = "裁剪头像",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 裁剪区域
        Box(
            modifier = Modifier
                .size(cropSize)
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            // 图片层
            CropImageView(
                imageUri = imageUri,
                scale = scale,
                offset = offset,
                onTransform = { newScale, newOffset ->
                    scale = newScale
                    offset = newOffset
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // 遮罩层
            CropOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 缩放控制
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { scale = max(0.5f, scale - 0.1f) }) {
                Icon(Icons.Default.ZoomOut, contentDescription = "缩小")
            }
            
            Slider(
                value = scale,
                onValueChange = { scale = it },
                valueRange = 0.5f..3f,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = { scale = min(3f, scale + 0.1f) }) {
                Icon(Icons.Default.ZoomIn, contentDescription = "放大")
            }
        }
        
        // 重置按钮
        TextButton(
            onClick = {
                scale = 1f
                offset = Offset.Zero
            }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("重置")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("取消")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    // 执行裁剪
                    cropImage(context, imageUri, scale, offset) { result ->
                        isLoading = false
                        result?.let { onCropComplete(it) }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("确认")
                }
            }
        }
        
        // 进度指示器
        if (isLoading) {
            LinearProgressIndicator(
                progress = { cropProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

/**
 * 可变换的图片视图
 */
@Composable
private fun CropImageView(
    imageUri: Uri,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f)
                    val newOffset = Offset(
                        offset.x + pan.x,
                        offset.y + pan.y
                    )
                    onTransform(newScale, newOffset)
                }
            }
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = "待裁剪图片",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

/**
 * 裁剪遮罩层
 */
@Composable
private fun CropOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // 裁剪区域（中心正方形）
        val cropSize = min(canvasWidth, canvasHeight) * 0.8f
        val cropLeft = (canvasWidth - cropSize) / 2
        val cropTop = (canvasHeight - cropSize) / 2
        val cropRect = Rect(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize)
        
        // 绘制半透明遮罩
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )
        
        // 清除裁剪区域
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            blendMode = BlendMode.Clear
        )
        
        // 绘制裁剪边框
        drawRect(
            color = Color.White,
            topLeft = Offset(cropRect.left, cropRect.top),
            size = Size(cropRect.width, cropRect.height),
            style = Stroke(width = 3.dp.toPx())
        )
        
        // 绘制网格线
        val gridColor = Color.White.copy(alpha = 0.5f)
        val strokeWidth = 1.dp.toPx()
        
        // 垂直线
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left + cropRect.width / 3, cropRect.top),
            end = Offset(cropRect.left + cropRect.width / 3, cropRect.bottom),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left + cropRect.width * 2 / 3, cropRect.top),
            end = Offset(cropRect.left + cropRect.width * 2 / 3, cropRect.bottom),
            strokeWidth = strokeWidth
        )
        
        // 水平线
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left, cropRect.top + cropRect.height / 3),
            end = Offset(cropRect.right, cropRect.top + cropRect.height / 3),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridColor,
            start = Offset(cropRect.left, cropRect.top + cropRect.height * 2 / 3),
            end = Offset(cropRect.right, cropRect.top + cropRect.height * 2 / 3),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * 执行图片裁剪
 */
private fun cropImage(
    context: android.content.Context,
    imageUri: Uri,
    scale: Float,
    offset: Offset,
    onComplete: (String?) -> Unit
) {
    try {
        // 加载原图
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        if (originalBitmap == null) {
            onComplete(null)
            return
        }
        
        // 计算裁剪参数
        val size = min(originalBitmap.width, originalBitmap.height)
        val cropSize = (size / scale).toInt()
        val cropX = ((originalBitmap.width - cropSize) / 2 - offset.x).toInt()
            .coerceIn(0, originalBitmap.width - cropSize)
        val cropY = ((originalBitmap.height - cropSize) / 2 - offset.y).toInt()
            .coerceIn(0, originalBitmap.height - cropSize)
        
        // 执行裁剪
        val croppedBitmap = Bitmap.createBitmap(
            originalBitmap,
            cropX,
            cropY,
            cropSize,
            cropSize
        )
        
        // 缩放为目标大小（512x512）
        val targetSize = 512
        val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, targetSize, targetSize, true)
        
        // 保存到本地
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        // 清理
        if (originalBitmap != croppedBitmap) {
            originalBitmap.recycle()
        }
        croppedBitmap.recycle()
        
        onComplete(file.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}

/**
 * 头像预览组件
 */
@Composable
fun AvatarPreview(
    avatarPath: String?,
    size: androidx.compose.ui.unit.Dp = 120.dp,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (avatarPath != null) {
            AsyncImage(
                model = avatarPath,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "默认头像",
                modifier = Modifier.size(size * 0.5f),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // 编辑指示器 - 仅在未设置头像时显示相机图标
        if (onClick != null && avatarPath == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "设置头像",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.3f)
                )
            }
        }
    }
}
