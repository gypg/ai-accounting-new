package com.example.aiaccounting.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 个人中心 ViewModel
 */
@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {
    // 后续可以添加用户统计数据的获取逻辑
}
