package com.f0x1d.logfox.viewmodel.base

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.f0x1d.logfox.R
import com.f0x1d.logfox.model.event.Event
import com.f0x1d.logfox.model.event.SnackbarEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * BaseViewModel 提供了基本的 ViewModel 功能，包括错误处理和Snackbar消息的抽象。
 * 它是 AndroidViewModel 的一个抽象子类，主要面向于处理UI逻辑和应用状态。
 */
abstract class BaseViewModel(application: Application): AndroidViewModel(application) {

    // 提供对Application上下文的访问
    val ctx: Context get() = getApplication()

    // 用于存储和传递事件数据的LiveData对象
    val eventsData = MutableLiveData<Event>()
    // 用于存储和传递Snackbar事件的LiveData对象
    val snackbarEventsData = MutableLiveData<SnackbarEvent>()

    /**
     * 启动一个协程，并在发生异常时捕获，然后执行错误处理逻辑。
     * @param context 协程的执行上下文，默认为主线程的Dispatcher
     * @param errorBlock 发生异常时执行的协程块
     * @param block 主协程块，可能会抛出异常
     */
    fun launchCatching(
        context: CoroutineContext = Dispatchers.Main,
        errorBlock: suspend CoroutineScope.() -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) = viewModelScope.launch(context) {
        try {
            coroutineScope {
                block(this)
            }
        } catch (e: Exception) {
            if (e is CancellationException) return@launch

            errorBlock(this)

            e.printStackTrace()

            snackbar(ctx.getString(R.string.error, e.localizedMessage))
        }
    }

    // 使用资源id显示Snackbar消息
    protected fun snackbar(id: Int) = snackbar(ctx.getString(id))

    /**
     * 显示Snackbar消息。
     * @param text 显示在Snackbar上的文本
     */
    protected fun snackbar(text: String) {
        viewModelScope.launch(Dispatchers.Main.immediate) {
            snackbarEventsData.value = SnackbarEvent(text)
        }
    }
}
