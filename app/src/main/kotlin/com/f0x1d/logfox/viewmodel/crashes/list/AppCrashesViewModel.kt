package com.f0x1d.logfox.viewmodel.crashes.list

import android.app.Application
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.database.AppDatabase
import com.f0x1d.logfox.database.entity.AppCrash
import com.f0x1d.logfox.di.viewmodel.AppName
import com.f0x1d.logfox.di.viewmodel.PackageName
import com.f0x1d.logfox.model.AppCrashesCount
import com.f0x1d.logfox.repository.logging.CrashesRepository
import com.f0x1d.logfox.viewmodel.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * AppCrashesViewModel 类负责管理应用崩溃数据的视图状态。
 * 使用 Hilt 进行依赖注入，构造函数接收包名、应用名、数据库实例、崩溃数据仓库和应用实例。
 * 继承自 BaseViewModel，提供了一个用于获取应用崩溃数据的 LiveData 对象和一个删除崩溃记录的方法。
 */
@HiltViewModel
class AppCrashesViewModel @Inject constructor(
    @PackageName val packageName: String, // 应用的包名
    @AppName val appName: String?, // 应用的名称，可以为 null
    private val database: AppDatabase, // 数据库实例，用于本地数据存储
    private val crashesRepository: CrashesRepository, // 崩溃数据仓库，用于进行崩溃数据的网络请求或其它操作
    application: Application // 应用实例，用于提供上下文
): BaseViewModel(application) {

    /**
     * 一个 LiveData 对象，包含应用的崩溃计数。
     * 通过数据库流获取所有崩溃记录，过滤出与当前应用包名匹配的记录，然后转换为 AppCrashesCount 对象的列表。
     * 数据的刷新和转换在 IO 线程上进行。
     */
    val crashes = database.appCrashDao().getAllAsFlow()
        .distinctUntilChanged()
        .map { crashes ->
            crashes.filter { crash ->
                crash.packageName == packageName // 过滤出与当前应用包名匹配的崩溃记录
            }.map {
                AppCrashesCount(it) // 将崩溃记录转换为 AppCrashesCount 对象
            }
        }
        .flowOn(Dispatchers.IO) // 确保在 IO 线程上运行以避免阻塞主线程
        .asLiveData()

    /**
     * 删除指定的崩溃记录。
     * @param appCrash 要删除的崩溃记录。
     */
    fun deleteCrash(appCrash: AppCrash) = crashesRepository.delete(appCrash) // 调用崩溃数据仓库的 delete 方法删除指定的崩溃记录
}
