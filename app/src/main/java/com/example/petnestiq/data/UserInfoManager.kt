package com.example.petnestiq.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// 用户信息数据类
data class UserInfo(
    val nickname: String,
    val petBreed: String,
    val petAge: String,
    val avatarResourceId: Int = com.example.petnestiq.R.drawable.cat,
    val avatarUri: String? = null,
    val savedAvatarPath: String? = null
)

// 用户信息管理器
class UserInfoManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: UserInfoManager? = null

        private const val PREFS_NAME = "user_info_prefs"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_PET_BREED = "pet_breed"
        private const val KEY_PET_AGE = "pet_age"
        private const val KEY_AVATAR_RESOURCE_ID = "avatar_resource_id"
        private const val KEY_AVATAR_URI = "avatar_uri"
        private const val KEY_SAVED_AVATAR_PATH = "saved_avatar_path"
        private const val AVATAR_FILENAME = "user_avatar.jpg"

        fun getInstance(context: Context): UserInfoManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserInfoManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // 使用StateFlow管理用户信息状态
    private val _userInfo = MutableStateFlow(loadUserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    // 从SharedPreferences加载用户信息
    private fun loadUserInfo(): UserInfo {
        val savedPath = prefs.getString(KEY_SAVED_AVATAR_PATH, null)
        val validSavedPath = if (savedPath != null && File(savedPath).exists()) {
            savedPath
        } else {
            null
        }

        return UserInfo(
            nickname = prefs.getString(KEY_NICKNAME, "铲屎官") ?: "铲屎官",
            petBreed = prefs.getString(KEY_PET_BREED, "英短") ?: "英短",
            petAge = prefs.getString(KEY_PET_AGE, "2岁") ?: "2岁",
            avatarResourceId = prefs.getInt(KEY_AVATAR_RESOURCE_ID, com.example.petnestiq.R.drawable.cat),
            avatarUri = prefs.getString(KEY_AVATAR_URI, null),
            savedAvatarPath = validSavedPath
        )
    }

    // 保存用户信息到SharedPreferences
    private fun saveUserInfo(userInfo: UserInfo) {
        prefs.edit().apply {
            putString(KEY_NICKNAME, userInfo.nickname)
            putString(KEY_PET_BREED, userInfo.petBreed)
            putString(KEY_PET_AGE, userInfo.petAge)
            putInt(KEY_AVATAR_RESOURCE_ID, userInfo.avatarResourceId)
            putString(KEY_AVATAR_URI, userInfo.avatarUri)
            putString(KEY_SAVED_AVATAR_PATH, userInfo.savedAvatarPath)
            apply()
        }
    }

    // 保存头像应用内部存储
    private fun saveAvatarToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // 压缩图片
                val compressedBitmap = compressBitmap(bitmap, 512, 512)

                val avatarFile = File(context.filesDir, AVATAR_FILENAME)
                val outputStream = FileOutputStream(avatarFile)
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                compressedBitmap.recycle()

                avatarFile.absolutePath
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // 压缩图片
    private fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

        return if (ratio < 1) {
            val newWidth = (width * ratio).toInt()
            val newHeight = (height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    // 更新用户信息
    fun updateUserInfo(newUserInfo: UserInfo) {
        _userInfo.value = newUserInfo
        saveUserInfo(newUserInfo)
    }

    // 更新昵称
    fun updateNickname(nickname: String) {
        val newUserInfo = _userInfo.value.copy(nickname = nickname)
        _userInfo.value = newUserInfo
        saveUserInfo(newUserInfo)
    }

    // 更新宠物品种
    fun updatePetBreed(breed: String) {
        val newUserInfo = _userInfo.value.copy(petBreed = breed)
        _userInfo.value = newUserInfo
        saveUserInfo(newUserInfo)
    }

    // 更新宠物年龄
    fun updatePetAge(age: String) {
        val newUserInfo = _userInfo.value.copy(petAge = age)
        _userInfo.value = newUserInfo
        saveUserInfo(newUserInfo)
    }

    // 更新头像（资源ID）
    fun updateAvatar(resourceId: Int) {
        val newUserInfo = _userInfo.value.copy(
            avatarResourceId = resourceId,
            avatarUri = null, // 清除URI
            savedAvatarPath = null // 清除保存的路径
        )
        _userInfo.value = newUserInfo
        saveUserInfo(newUserInfo)

        // 删除旧的头像文件
        deleteOldAvatarFile()
    }

    // 更新头像（URI）- 同时保存到内部存储
    fun updateAvatarUri(uri: String) {
        try {
            val savedPath = saveAvatarToInternalStorage(Uri.parse(uri))
            val newUserInfo = _userInfo.value.copy(
                avatarUri = uri,
                savedAvatarPath = savedPath
            )
            _userInfo.value = newUserInfo
            saveUserInfo(newUserInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果保存失败，至少保存URI
            val newUserInfo = _userInfo.value.copy(avatarUri = uri)
            _userInfo.value = newUserInfo
            saveUserInfo(newUserInfo)
        }
    }

    // 删除旧的头像文件
    private fun deleteOldAvatarFile() {
        try {
            val avatarFile = File(context.filesDir, AVATAR_FILENAME)
            if (avatarFile.exists()) {
                avatarFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 添加新的方法：直接从URI更新并保存头像
    fun updateAvatarFromUri(uri: Uri) {
        try {
            val savedPath = saveAvatarToInternalStorage(uri)
            val newUserInfo = _userInfo.value.copy(
                avatarUri = uri.toString(),
                savedAvatarPath = savedPath
            )
            _userInfo.value = newUserInfo
            saveUserInfo(newUserInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果保存失败，至少保存URI
            val newUserInfo = _userInfo.value.copy(avatarUri = uri.toString())
            _userInfo.value = newUserInfo
            saveUserInfo(newUserInfo)
        }
    }

    // 获取宠物信息格式化字符串
    fun getPetInfoDisplay(): String {
        val info = _userInfo.value
        return "${info.petBreed} | ${info.petAge}"
    }

    // 判断是否使用自定义头像
    fun isUsingCustomAvatar(): Boolean {
        return _userInfo.value.savedAvatarPath != null || _userInfo.value.avatarUri != null
    }

    // 获取头像显示路径（优先使用保存的路径）
    fun getAvatarDisplayPath(): String? {
        val userInfo = _userInfo.value
        return userInfo.savedAvatarPath ?: userInfo.avatarUri
    }
}
