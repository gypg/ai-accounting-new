package com.example.aiaccounting.data.model

/**
 * 用户资料数据类
 */
data class UserProfile(
    val id: String = "",
    val userName: String = "",
    val nickName: String = "",
    val avatarUrl: String? = null,
    val gender: Gender = Gender.UNSPECIFIED,
    val birthDate: Long? = null,
    val phone: String = "",
    val email: String = "",
    val bio: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class Gender {
    MALE, FEMALE, UNSPECIFIED
}

/**
 * 账户安全设置
 */
data class SecuritySettings(
    val passwordLastChanged: Long? = null,
    val twoFactorEnabled: Boolean = false,
    val twoFactorMethod: TwoFactorMethod = TwoFactorMethod.NONE,
    val lastLoginTime: Long? = null,
    val lastLoginDevice: String? = null,
    val trustedDevices: List<TrustedDevice> = emptyList()
)

enum class TwoFactorMethod {
    NONE, SMS, EMAIL, AUTHENTICATOR
}

data class TrustedDevice(
    val deviceId: String,
    val deviceName: String,
    val lastUsed: Long,
    val isTrusted: Boolean
)

/**
 * 隐私设置
 */
data class PrivacySettings(
    val profileVisibility: VisibilityLevel = VisibilityLevel.PUBLIC,
    val activityVisibility: VisibilityLevel = VisibilityLevel.FRIENDS,
    val allowSearchByPhone: Boolean = true,
    val allowSearchByEmail: Boolean = false,
    val thirdPartyAuthorizations: List<ThirdPartyAuth> = emptyList(),
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val marketingEmails: Boolean = false
)

enum class VisibilityLevel {
    PUBLIC, FRIENDS, PRIVATE
}

data class ThirdPartyAuth(
    val provider: String,
    val isAuthorized: Boolean,
    val authorizedAt: Long
)

/**
 * 个性化设置
 */
data class PersonalizationSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val useCompactLayout: Boolean = false,
    val enableAnimations: Boolean = true,
    val quickActions: List<String> = emptyList()
)

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

enum class FontSize {
    SMALL, MEDIUM, LARGE
}
