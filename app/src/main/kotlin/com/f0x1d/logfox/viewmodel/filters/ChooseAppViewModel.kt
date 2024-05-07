package com.f0x1d.logfox.viewmodel.filters

import android.app.Application
import androidx.lifecycle.asLiveData
import com.f0x1d.logfox.viewmodel.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * ChooseAppViewModel 类负责管理应用程序选择界面的视图状态。
 * 它使用 Hilt 进行依赖注入，并在构造时接收一个 Application 对象。
 */
@HiltViewModel
class ChooseAppViewModel @Inject constructor(
    application: Application
): BaseViewModel(application) {

    // 表示应用程序列表的 StateFlow，初始为空列表。
    val apps = MutableStateFlow(emptyList<com.f0x1d.logfox.model.InstalledApp>())
    // 用户搜索查询的文本，用于过滤应用程序列表。
    val query = MutableStateFlow("")

    // 根据查询过滤应用程序列表的 StateFlow。它会异步更新，并且避免重复的值。
    val searchedApps = combine(apps, query) { apps, query ->
        apps to query
    }.map {
        it.first.filter { app ->
            // 过滤逻辑，保留标题或包名包含查询文本的应用程序。
            app.title.toString().contains(it.second) || app.packageName.contains(it.second)
        }
    }.flowOn(
        Dispatchers.IO // 在 IO 调度器上运行，以避免阻塞主线程
    ).distinctUntilChanged().asLiveData()

    // 类初始化块，用于加载应用程序列表。
    init {
        load()
    }

    /**
     * 加载安装在设备上的应用程序信息，并更新 [apps] 状态。
     * 这个函数会异步执行，捕获任何异常，并将它们报告给 ViewModel 的错误处理逻辑。
     */
    private fun load() = launchCatching(Dispatchers.IO) {
        // 获取包管理器，并使用它来获取已安装的应用程序列表。
        val packageManager = ctx.packageManager

        // 将包管理器返回的安装应用程序列表转换为模型类实例，并按标题进行排序。
        val installedApps = packageManager.getInstalledPackages(0).map {
            com.f0x1d.logfox.model.InstalledApp(
                title = it.applicationInfo.loadLabel(packageManager),
                packageName = it.packageName
            )
        }.sortedBy {
            it.title.toString()
        }

        // 更新应用程序列表状态。
        apps.update {
            installedApps
        }
    }
}
