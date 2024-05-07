package com.f0x1d.logfox.viewmodel.crashes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.database.AppDatabase
import com.f0x1d.logfox.database.entity.AppCrash
import com.f0x1d.logfox.di.viewmodel.CrashId
import com.f0x1d.logfox.extensions.io.output.exportToZip
import com.f0x1d.logfox.extensions.io.output.putZipEntry
import com.f0x1d.logfox.model.deviceData
import com.f0x1d.logfox.repository.logging.CrashesRepository
import com.f0x1d.logfox.utils.DateTimeFormatter
import com.f0x1d.logfox.utils.preferences.AppPreferences
import com.f0x1d.logfox.viewmodel.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * CrashDetailsViewModel 类负责处理与崩溃详情相关的逻辑。
 * 它通过 Hilt 进行依赖注入，并管理崩溃数据的展示和导出。
 */
@HiltViewModel
class CrashDetailsViewModel @Inject constructor(
    @CrashId val crashId: Long, // 崩溃的唯一标识符
    val dateTimeFormatter: DateTimeFormatter, // 日期时间格式化器
    private val database: AppDatabase, // 应用数据库
    private val crashesRepository: CrashesRepository, // 崩溃信息仓库
    private val appPreferences: AppPreferences, // 应用偏好设置
    application: Application // 应用实例
): BaseViewModel(application) { // 基类 ViewModel，提供一些基础功能

    companion object {
        const val EVENT_TYPE_COPY_LINK = "copy_link" // 事件类型：复制链接
    }

    // 通过数据库获取崩溃详情，并将其转换为 LiveData 对象进行观察
    val crash = database.appCrashDao().get(crashId)
        .map {
            when (it) {
                null -> null
                else -> runCatching {
                    it to it.logFile?.readText() // 尝试读取崩溃日志文本
                }.getOrNull() // 捕获异常并获取结果
            }
        }
        .distinctUntilChanged() // 确保数据唯一
        .flowOn(Dispatchers.IO) // 在 IO 线程上运行
        .asLiveData() // 转换为 LiveData 对象

    /**
     * 将崩溃信息导出为 ZIP 文件。
     * @param uri 导出文件的 URI
     */
    fun exportCrashToZip(uri: Uri) = launchCatching(Dispatchers.IO) {
        val (appCrash, crashLog) = crash.value ?: return@launchCatching

        ctx.contentResolver.openOutputStream(uri)?.use {
            it.exportToZip { // 将数据写入 ZIP 文件
                // 根据偏好设置，可能包含设备信息文件
                if (appPreferences.includeDeviceInfoInArchives) putZipEntry(
                    name = "device.txt",
                    content = deviceData.encodeToByteArray(),
                )

                // 包含崩溃日志文件（如果存在）
                if (crashLog != null) putZipEntry(
                    name = "crash.log",
                    content = crashLog.encodeToByteArray(),
                )

                // 包含崩溃的 dump 文件（如果存在）
                appCrash.logDumpFile?.let { logDumpFile ->
                    putZipEntry(
                        name = "dump.log",
                        file = logDumpFile,
                    )
                }
            }
        }
    }

    /**
     * 从仓库中删除指定的崩溃信息。
     * @param appCrash 要删除的崩溃信息对象
     */
    fun deleteCrash(appCrash: AppCrash) = crashesRepository.delete(appCrash)
}
