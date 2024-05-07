package com.f0x1d.logfox.viewmodel.filters

import android.app.Application
import android.net.Uri
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.database.AppDatabase
import com.f0x1d.logfox.database.entity.UserFilter
import com.f0x1d.logfox.repository.logging.FiltersRepository
import com.f0x1d.logfox.viewmodel.base.BaseViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * FiltersViewModel 类负责管理筛选器的视图状态。
 * 它通过 Hilt 进行依赖注入，接收数据库、筛选器仓库、Gson 和应用实例作为构造参数。
 */
@HiltViewModel
class FiltersViewModel @Inject constructor(
    private val database: AppDatabase,
    private val filtersRepository: FiltersRepository,
    private val gson: Gson,
    application: Application
): BaseViewModel(application) {

    /**
     * 从数据库中获取所有筛选器的 LiveData 流。
     * 该流会过滤掉重复的数据，并在IO线程上运行，以避免主线程阻塞。
     */
    val filters = database.userFilterDao().getAllAsFlow()
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
        .asLiveData()

    /**
     * 导入筛选器数据。
     * 从给定的Uri读取数据，将其解析为筛选器列表，并存储到筛选器仓库中。
     * @param uri 数据源的Uri。
     */
    fun import(uri: Uri) = launchCatching(Dispatchers.IO) {
        ctx.contentResolver.openInputStream(uri)?.use {
            val filters = gson.fromJson<List<UserFilter>>(
                it.readBytes().decodeToString(),
                object : TypeToken<List<UserFilter>>() {}.type
            )

            filtersRepository.createAll(filters)
        }
    }

    /**
     * 导出所有筛选器数据。
     * 将当前所有筛选器数据转换为JSON格式，并写入到指定的Uri中。
     * @param uri 保存筛选器数据的目标Uri。
     */
    fun exportAll(uri: Uri) = launchCatching(Dispatchers.IO) {
        val filters = filters.value ?: return@launchCatching

        ctx.contentResolver.openOutputStream(uri)?.use {
            it.write(gson.toJson(filters).encodeToByteArray())
        }
    }

    /**
     * 切换指定筛选器的启用状态。
     * @param userFilter 要切换的筛选器。
     * @param checked 新的启用状态。
     */
    fun switch(userFilter: UserFilter, checked: Boolean) = filtersRepository.switch(userFilter, checked)

    /**
     * 删除指定的筛选器。
     * @param userFilter 要删除的筛选器。
     */
    fun delete(userFilter: UserFilter) = filtersRepository.delete(userFilter)

    /**
     * 清除所有筛选器。
     */
    fun clearAll() = filtersRepository.clear()
}
