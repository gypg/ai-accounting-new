package com.example.aiaccounting.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.aiaccounting.ui.viewmodel.ButlerListItem
import com.example.aiaccounting.ui.viewmodel.ButlerMarketEvent
import com.example.aiaccounting.ui.viewmodel.ButlerMarketViewModel
import com.example.aiaccounting.ui.theme.Elevation
import com.example.aiaccounting.ui.theme.Motion
import com.example.aiaccounting.ui.theme.Shapes
import com.example.aiaccounting.ui.theme.Size
import com.example.aiaccounting.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File

/**
 * 管家市场页面 — 内置 + 自定义管家混排列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerMarketScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (butlerId: String?) -> Unit,
    viewModel: ButlerMarketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val butlerList by viewModel.butlerList.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 导入 JSON 文件选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val json = context.contentResolver.openInputStream(it)
                    ?.bufferedReader()?.readText() ?: return@let
                viewModel.importButlerFromJson(json)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("读取文件失败: ${e.message}") }
            }
        }
    }

    // 收集事件
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ButlerMarketEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(event.message)
                is ButlerMarketEvent.NavigateToEditor ->
                    onNavigateToEditor(event.butlerId)
                is ButlerMarketEvent.NavigateBack ->
                    onNavigateBack()
                is ButlerMarketEvent.ShareJson -> {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_TEXT, event.json)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "分享管家配置"))
                }
            }
        }
    }

    // 删除确认对话框
    uiState.showDeleteConfirm?.let { butlerId ->
        val name = butlerList.find { it.id == butlerId }?.name ?: "管家"
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = { Text("确认删除") },
            text = { Text("确定要删除「$name」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDelete() }) { Text("取消") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("管家市场", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 导入按钮
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导入管家")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建管家")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (butlerList.isEmpty() && !uiState.isLoading) {
            ButlerMarketEmptyState(
                onCreate = { onNavigateToEditor(null) },
                onImport = { importLauncher.launch("application/json") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        horizontal = Spacing.screenHorizontal,
                        vertical = Spacing.screenVertical
                    )
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    horizontal = Spacing.screenHorizontal,
                    vertical = Spacing.screenVertical
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.listItemGap)
            ) {
                // 内置管家区域
                val builtInItems = butlerList.filterIsInstance<ButlerListItem.BuiltIn>()
                if (builtInItems.isNotEmpty()) {
                    item(key = "header_builtin") {
                        SectionHeader("内置管家")
                    }
                    items(builtInItems, key = { it.id }) { item ->
                        ButlerMarketCard(
                            item = item,
                            isSelected = item.id == uiState.selectedButlerId,
                            onSelect = { viewModel.selectButler(item.id) },
                            onEdit = null,
                            onDuplicate = null,
                            onDelete = null,
                            onExport = null
                        )
                    }
                }

                // 自定义管家区域
                val customItems = butlerList.filterIsInstance<ButlerListItem.Custom>()
                if (customItems.isNotEmpty()) {
                    item(key = "header_custom") {
                        SectionHeader("自定义管家", Modifier.padding(top = Spacing.sectionGap))
                    }
                    items(customItems, key = { it.id }) { item ->
                        ButlerMarketCard(
                            item = item,
                            isSelected = item.id == uiState.selectedButlerId,
                            onSelect = { viewModel.selectButler(item.id) },
                            onEdit = { onNavigateToEditor(item.id) },
                            onDuplicate = { viewModel.duplicateButler(item.id) },
                            onDelete = { viewModel.requestDelete(item.id) },
                            onExport = { viewModel.exportButler(item.id) }
                        )
                    }
                }

                // 底部空间给 FAB 让位
                item(key = "spacer_bottom") {
                    Spacer(modifier = Modifier.height(Size.bottomNavHeight))
                }
            }
        }
    }
}

@Composable
private fun ButlerMarketEmptyState(
    onCreate: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(Size.iconXl),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "还没有管家",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "创建一个专属管家，或导入已有配置。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Button(
                onClick = onCreate,
                shape = Shapes.button,
                modifier = Modifier.height(Size.buttonHeight)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(Size.iconSm)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("创建管家")
            }

            OutlinedButton(
                onClick = onImport,
                shape = Shapes.button,
                modifier = Modifier.height(Size.buttonHeight)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(Size.iconSm)
                )
                Spacer(modifier = Modifier.width(Spacing.xs))
                Text("导入")
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        modifier = modifier.padding(bottom = Spacing.xs)
    )
}

/**
 * 管家卡片 — 统一支持内置和自定义
 */
@Composable
private fun ButlerMarketCard(
    item: ButlerListItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)?,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onExport: (() -> Unit)?
) {
    var showMenu by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(
            durationMillis = Motion.durationFast,
            easing = Motion.easeInOut
        ),
        label = "butlerCardContainer"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(
            durationMillis = Motion.durationFast,
            easing = Motion.easeInOut
        ),
        label = "butlerCardBorder"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) Elevation.md else Elevation.none,
        animationSpec = tween(
            durationMillis = Motion.durationFast,
            easing = Motion.easeInOut
        ),
        label = "butlerCardElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = Motion.durationMedium,
                    easing = Motion.easeInOut
                )
            ),
        shape = Shapes.card,
        border = if (borderWidth > 0.dp) {
            androidx.compose.foundation.BorderStroke(
                borderWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box {
                ButlerAvatar(item = item, size = 56)

                if (isSelected) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-2).dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = Elevation.xs
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(2.dp)
                                .size(Size.iconXs)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // 信息
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (item.isBuiltIn) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Surface(
                            shape = Shapes.tag,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                Size.divider,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                        ) {
                            Text(
                                text = "内置",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(
                                    horizontal = Spacing.xs,
                                    vertical = Spacing.xxxs
                                )
                            )
                        }
                    }
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xxxs)
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Spacing.xs)
                )
            }

            // 自定义管家才显示菜单
            if (!item.isBuiltIn) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        onEdit?.let {
                            DropdownMenuItem(
                                text = { Text("编辑") },
                                onClick = { showMenu = false; it() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                        }
                        onDuplicate?.let {
                            DropdownMenuItem(
                                text = { Text("复制") },
                                onClick = { showMenu = false; it() },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                            )
                        }
                        onExport?.let {
                            DropdownMenuItem(
                                text = { Text("导出/分享") },
                                onClick = { showMenu = false; it() },
                                leadingIcon = { Icon(Icons.Default.Share, null) }
                            )
                        }
                        onDelete?.let {
                            DropdownMenuItem(
                                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; it() },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 管家头像渲染 — 支持 R.drawable / 本地路径 / 默认图标
 */
@Composable
fun ButlerAvatar(item: ButlerListItem, size: Int) {
    val model: Any? = when (item) {
        is ButlerListItem.BuiltIn -> item.butler.avatarResId
        is ButlerListItem.Custom -> {
            val entity = item.entity
            when (entity.avatarType) {
                "LOCAL_PATH" -> {
                    val file = File(entity.avatarValue)
                    if (file.exists()) file else null
                }
                "RESOURCE" -> null // 自定义暂无 resource
                else -> null
            }
        }
    }

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = item.name,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = item.name,
                modifier = Modifier.size((size * 0.5f).dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
