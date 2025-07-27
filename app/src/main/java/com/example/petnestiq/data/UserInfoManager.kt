package com.example.petnestiq.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 用户信息数据类
data class UserInfo(
    val nickname: String,
    val petBreed: String,
    val petAge: String,
    val avatarResourceId: Int = com.example.petnestiq.R.drawable.cat,
    val avatarUri: String? = null // 添加URI字段用于存储相册选择的图片
)

// 用户信息管理器（单例模式）
class UserInfoManager private constructor() {

    // 使用StateFlow管理用户信息状态
    private val _userInfo = MutableStateFlow(
        UserInfo(
            nickname = "铲屎官",
            petBreed = "英短",
            petAge = "2岁"
        )
    )
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: UserInfoManager? = null

        fun getInstance(): UserInfoManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserInfoManager().also { INSTANCE = it }
            }
        }
    }

    // 更新用户信息
    fun updateUserInfo(newUserInfo: UserInfo) {
        _userInfo.value = newUserInfo
    }

    // 更新昵称
    fun updateNickname(nickname: String) {
        _userInfo.value = _userInfo.value.copy(nickname = nickname)
    }

    // 更新宠物品种
    fun updatePetBreed(breed: String) {
        _userInfo.value = _userInfo.value.copy(petBreed = breed)
    }

    // 更新宠物年龄
    fun updatePetAge(age: String) {
        _userInfo.value = _userInfo.value.copy(petAge = age)
    }

    // 更新头像（资源ID）
    fun updateAvatar(resourceId: Int) {
        _userInfo.value = _userInfo.value.copy(
            avatarResourceId = resourceId,
            avatarUri = null // 清除URI，使用资源ID
        )
    }

    // 更新头像（URI）
    fun updateAvatarUri(uri: String) {
        _userInfo.value = _userInfo.value.copy(avatarUri = uri)
    }

    // 获取宠物信息格式化字符串
    fun getPetInfoDisplay(): String {
        val info = _userInfo.value
        return "${info.petBreed} | ${info.petAge}"
    }

    // 判断是否使用自定义头像
    fun isUsingCustomAvatar(): Boolean {
        return _userInfo.value.avatarUri != null
    }
}
