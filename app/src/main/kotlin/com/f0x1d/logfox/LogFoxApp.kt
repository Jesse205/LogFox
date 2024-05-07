package com.f0x1d.logfox

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.f0x1d.logfox.extensions.context.applyTheme
import com.f0x1d.logfox.extensions.context.notificationManagerCompat
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope

/**
 * LogFoxApp 的主应用程序类。
 * 初始化应用程序特定的设置和配置。
 */
@HiltAndroidApp
class LogFoxApp: Application() {

    companion object {
        // 通知渠道ID
        const val LOGGING_STATUS_CHANNEL_ID = "logging"
        const val CRASHES_CHANNEL_ID = "crashes"
        const val RECORDING_STATUS_CHANNEL_ID = "recording"

        // 应用程序范围的协程作用域，用于应用级操作
        val applicationScope = MainScope()

        // 应用程序类的单例实例引用
        lateinit var instance: LogFoxApp
    }

    /**
     * 当应用程序启动时调用，在创建任何活动、服务或接收器对象（内容提供者除外）之前。
     */
    override fun onCreate() {
        super.onCreate()
        instance = this // 初始化单例实例

        // 应用应用程序主题
        applyTheme()

        // 如果可用，将动态颜色应用于应用程序的活动
        DynamicColors.applyToActivitiesIfAvailable(this)

        // 配置通知渠道
        notificationManagerCompat.apply {
            // 创建日志状态通知渠道
            val loggingStatusChannel = NotificationChannelCompat.Builder(
                LOGGING_STATUS_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_MIN
            )
                .setName(getString(R.string.logging_status)) // 设置渠道名称
                .setShowBadge(false) // 对此渠道禁用徽章计数
                .build()

            // 创建崩溃通知渠道
            val crashesChannel = NotificationChannelCompat.Builder(
                CRASHES_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_HIGH
            )
                .setName(getString(R.string.crashes)) // 设置渠道名称
                .setLightsEnabled(true) // 为此渠道启用闪光灯
                .setVibrationEnabled(true) // 为此渠道启用震动
                .build()

            // 创建录制状态通知渠道
            val recordingStatusChannel = NotificationChannelCompat.Builder(
                RECORDING_STATUS_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
                .setName(getString(R.string.recording_status)) // 设置渠道名称
                .setLightsEnabled(false) // 对此渠道禁用LED灯
                .setVibrationEnabled(false) // 对此渠道禁用震动
                .setSound(null, null) // 为此渠道设置无声音
                .build()

            // 使用通知管理器注册所有通知渠道
            createNotificationChannelsCompat(
                listOf(
                    loggingStatusChannel,
                    crashesChannel,
                    recordingStatusChannel
                )
            )
        }
    }
}
