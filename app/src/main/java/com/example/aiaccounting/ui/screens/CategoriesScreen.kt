package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aiaccounting.data.local.entity.Category
import com.example.aiaccounting.data.local.entity.TransactionType
import com.example.aiaccounting.ui.components.ColorSelector
import com.example.aiaccounting.ui.components.FlowRow
import com.example.aiaccounting.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()
    val expenseCategories by viewModel.expenseCategories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "分类管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加分类")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab切换
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("支出") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("收入") }
                )
            }

            // 分类列表
            val displayCategories = if (selectedTab == 0) {
                expenseCategories.filter { it.parentId == null }
            } else {
                incomeCategories.filter { it.parentId == null }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayCategories) { category ->
                    val subCategories = categories.filter { it.parentId == category.id }
                    CategoryCard(
                        category = category,
                        subCategories = subCategories,
                        onEdit = {
                            editingCategory = category
                            showEditDialog = true
                        },
                        onDelete = {
                            categoryToDelete = category
                            showDeleteConfirm = true
                        },
                        onAddSubCategory = {
                            editingCategory = category
                            showAddDialog = true
                        }
                    )
                }
            }
        }
    }

    // 添加分类对话框
    if (showAddDialog) {
        AddCategoryDialog(
            parentCategory = editingCategory,
            onDismiss = { 
                showAddDialog = false
                editingCategory = null
            },
            onConfirm = { name, type, icon, color, parentId ->
                viewModel.createCategory(name, type, icon, color, parentId)
                showAddDialog = false
                editingCategory = null
            }
        )
    }

    // 编辑分类对话框
    if (showEditDialog && editingCategory != null) {
        EditCategoryDialog(
            category = editingCategory!!,
            onDismiss = { 
                showEditDialog = false
                editingCategory = null
            },
            onConfirm = { category ->
                viewModel.updateCategory(category)
                showEditDialog = false
                editingCategory = null
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                categoryToDelete = null
            },
            title = { Text("确认删除") },
            text = { 
                val hasSubCategories = categories.any { it.parentId == categoryToDelete!!.id }
                if (hasSubCategories) {
                    Text("分类\"${categoryToDelete!!.name}\"下有子分类，删除后将同时删除所有子分类。此操作不可撤销。")
                } else {
                    Text("确定要删除分类\"${categoryToDelete!!.name}\"吗？此操作不可撤销。")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(categoryToDelete!!)
                        showDeleteConfirm = false
                        categoryToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    categoryToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun CategoryCard(
    category: Category,
    subCategories: List<Category>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddSubCategory: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // 主分类
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类图标
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(category.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 分类信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (subCategories.isNotEmpty()) {
                        Text(
                            text = "${subCategories.size}个子分类",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // 展开/收起图标
                if (subCategories.isNotEmpty()) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // 操作按钮
                Row {
                    IconButton(onClick = onAddSubCategory) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加子分类",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 子分类列表
            if (expanded && subCategories.isNotEmpty()) {
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                subCategories.forEach { subCategory ->
                    SubCategoryItem(
                        category = subCategory,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun SubCategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .padding(start = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 连接线
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // 子分类图标
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 子分类名称
        Text(
            text = category.name,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )

        // 操作按钮
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddCategoryDialog(
    parentCategory: Category? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, TransactionType, String, String, Long?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(parentCategory?.type ?: TransactionType.EXPENSE) }
    var selectedIcon by remember { mutableStateOf(CategoryIcons[0]) }
    var selectedColor by remember { mutableStateOf(CategoryColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (parentCategory != null) "添加子分类到${parentCategory.name}" else "添加分类") 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 分类名称
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 分类类型（仅在顶级分类时显示）
                if (parentCategory == null) {
                    Text("分类类型", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    TransactionTypeSelector(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it }
                    )
                }

                // 图标选择
                Text("分类图标", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                IconSelector(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )

                // 颜色选择
                Text("分类颜色", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    colors = CategoryColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            selectedType,
                            selectedIcon,
                            selectedColor,
                            parentCategory?.id
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedIcon by remember { mutableStateOf(category.icon) }
    var selectedColor by remember { mutableStateOf(category.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分类") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("分类图标", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                IconSelector(
                    selectedIcon = selectedIcon,
                    onIconSelected = { selectedIcon = it }
                )

                Text("分类颜色", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it },
                    colors = CategoryColors
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            category.copy(
                                name = name,
                                icon = selectedIcon,
                                color = selectedColor
                            )
                        )
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text(type.displayName) },
                leadingIcon = if (selectedType == type) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconSelector(
    selectedIcon: String,
    onIconSelected: (String) -> Unit
) {
    Column {
        // 使用LazyVerticalGrid风格的布局
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 6
        ) {
            CategoryIcons.forEach { icon ->
                val isSelected = selectedIcon == icon
                IconItem(
                    icon = icon,
                    isSelected = isSelected,
                    onClick = { onIconSelected(icon) }
                )
            }
        }
    }
}

@Composable
private fun IconItem(
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface
        )
    }
}

val CategoryIcons = listOf(
    "🍔", "🚗", "🏠", "🎮", "👔", "💊", "📚", "🎁",
    "💰", "💳", "📱", "💻", "✈️", "🐶", "👶", "🎨",
    "🎵", "🏃", "💄", "🔧", "📝", "🛒", "☕", "🍺"
)

val CategoryColors = listOf(
    "#FF6B9FFF",  // 蓝色
    "#FF4CAF50",  // 绿色
    "#FFF44336",  // 红色
    "#FFFF9800",  // 橙色
    "#FF9C27B0",  // 紫色
    "#FF00BCD4",  // 青色
    "#FF795548",  // 棕色
    "#FF607D8B"   // 蓝灰色
)

val TransactionType.displayName: String
    get() = when (this) {
        TransactionType.INCOME -> "收入"
        TransactionType.EXPENSE -> "支出"
        TransactionType.TRANSFER -> "转账"
    }
