package com.f0x1d.logfox.viewmodel.crashes.list

import android.app.Application
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.database.AppDatabase
import com.f0x1d.logfox.database.entity.AppCrash
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
 * CrashesViewModel 类用于处理与应用崩溃相关的数据逻辑。
 * 它通过 Hilt 进行依赖注入，并与数据库和崩溃仓库进行交互来获取、更新崩溃信息。
 *
 * @param database AppDatabase 实例，用于本地数据存储。
 * @param crashesRepository CrashesRepository 实例，用于处理崩溃数据的仓库逻辑。
 * @param application 应用的 Application 实例，用于提供上下文。
 */
@HiltViewModel
class CrashesViewModel @Inject constructor(
    private val database: AppDatabase,
    private val crashesRepository: CrashesRepository,
    application: Application
): BaseViewModel(application) {

    /**
     * 将数据库中的崩溃信息流式传输并分组，然后转换为 AppCrashesCount 对象的 LiveData。
     * 这使得观察者能够接收到应用中每个包名的崩溃计数和最近一次崩溃的信息。
     */
    val crashes = database.appCrashDao().getAllAsFlow()
        .distinctUntilChanged()
        .map { crashes ->
            val groupedCrashes = crashes.groupBy { it.packageName }

            groupedCrashes.map {
                AppCrashesCount(
                    lastCrash = it.value.first(),
                    count = it.value.size
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .asLiveData()

    /**
     * 根据包名删除数据库中的崩溃记录。
     *
     * @param appCrash 包含要删除的崩溃记录的包名的 AppCrash 对象。
     */
    fun deleteCrashesByPackageName(appCrash: AppCrash) = crashesRepository.deleteAllByPackageName(appCrash)

    /**
     * 清除数据库中的所有崩溃记录。
     */
    fun clearCrashes() = crashesRepository.clear()
}
