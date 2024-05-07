package com.f0x1d.logfox.viewmodel.filters

import android.app.Application
import android.net.Uri
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.database.AppDatabase
import com.f0x1d.logfox.database.entity.UserFilter
import com.f0x1d.logfox.di.viewmodel.FilterId
import com.f0x1d.logfox.extensions.sendEvent
import com.f0x1d.logfox.repository.logging.FiltersRepository
import com.f0x1d.logfox.viewmodel.base.BaseViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
/**
 * EditFilterViewModel负责处理编辑过滤器的逻辑。
 * 它通过注入获得数据库、过滤器仓库、Gson实例和应用实例来实现数据的获取、更新和序列化。
 */
class EditFilterViewModel @Inject constructor(
    @FilterId val filterId: Long?, // 过滤器ID，如果不存在则为null
    private val database: AppDatabase, // 应用数据库实例
    private val filtersRepository: FiltersRepository, // 过滤器仓库实例
    private val gson: Gson, // Gson实例用于序列化和反序列化
    application: Application // 应用实例
): BaseViewModel(application) { // BaseViewModel基类，提供了一些基础的ViewModel逻辑

    companion object {
        // 事件类型常量，用于更新包名文本
        const val EVENT_TYPE_UPDATE_PACKAGE_NAME_TEXT = "update_package_name_text"
    }

    // 从数据库中获取当前编辑的过滤器，并更新相关的UI状态
    val filter = database.userFilterDao().get(filterId ?: -1L)
        .distinctUntilChanged() // 确保只通知最新的值
        .take(1) // 仅取第一次结果，不处理后续变化
        .flowOn(Dispatchers.IO) // 在IO线程上运行
        .onEach { filter ->
            // 如果过滤器不存在，则直接返回
            if (filter == null) return@onEach

            // 更新包括的选项、启用的日志级别、UID、PID、TID、包名、标签和内容
            including.update { filter.including }
            val allowedLevels = filter.allowedLevels.map { it.ordinal }
            for (i in 0 until enabledLogLevels.size) {
                enabledLogLevels[i] = allowedLevels.contains(i)
            }
            uid.update { filter.uid }
            pid.update { filter.pid }
            tid.update { filter.tid }
            packageName.update { filter.packageName }
            tag.update { filter.tag }
            content.update { filter.content }
        }
        .asLiveData()

    // 编辑过滤器时使用的状态流，用于UI更新
    val including = MutableStateFlow(true)
    val enabledLogLevels = mutableListOf(true, true, true, true, true, true, true)
    val uid = MutableStateFlow<String?>(null)
    val pid = MutableStateFlow<String?>(null)
    val tid = MutableStateFlow<String?>(null)
    val packageName = MutableStateFlow<String?>(null)
    val tag = MutableStateFlow<String?>(null)
    val content = MutableStateFlow<String?>(null)

    /**
     * 在仓库中创建一个新的过滤器。
     * @return 创建的过滤器信息
     */
    fun create() = filtersRepository.create(
        including.value,
        enabledLogLevels.toEnabledLogLevels(),
        uid.value, pid.value, tid.value, packageName.value, tag.value, content.value
    )

    /**
     * 在仓库中更新一个现有的过滤器。
     * @param userFilter 要更新的过滤器对象
     * @return 更新后的过滤器信息
     */
    fun update(userFilter: UserFilter) = filtersRepository.update(
        userFilter,
        including.value,
        enabledLogLevels.toEnabledLogLevels(),
        uid.value, pid.value, tid.value, packageName.value, tag.value, content.value
    )

    /**
     * 将过滤器导出到指定的URI。
     * @param uri 过滤器导出的目标URI
     */
    fun export(uri: Uri) = launchCatching(Dispatchers.IO) {
        ctx.contentResolver.openOutputStream(uri)?.use { outputStream ->
            // 将过滤器列表序列化并写入输出流
            val filters = filter.value?.let { listOf(it) } ?: emptyList()
            outputStream.write(gson.toJson(filters).encodeToByteArray())
        }
    }

    /**
     * 更新启用的日志级别。
     * @param which 要更新的日志级别索引
     * @param filtering 是否启用该日志级别
     */
    fun filterLevel(which: Int, filtering: Boolean) {
        enabledLogLevels[which] = filtering
    }

    /**
     * 根据选择的应用程序更新包名。
     * @param app 选择的已安装应用
     */
    fun selectApp(app: com.f0x1d.logfox.model.InstalledApp) = packageName.update {
        app.packageName
    }.also {
        // 发送事件以更新包名文本
        sendEvent(EVENT_TYPE_UPDATE_PACKAGE_NAME_TEXT)
    }

    // 将布尔列表转换为启用的日志级别列表
    private fun List<Boolean>.toEnabledLogLevels() = mapIndexed { index, value ->
        if (value)
            enumValues<com.f0x1d.logfox.model.LogLevel>()[index]
        else
            null
    }.filterNotNull()
}
