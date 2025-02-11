/*
 * Copyright (C) 2018 Drake, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.drake.net

import android.annotation.SuppressLint
import android.app.Application
import android.app.Dialog
import android.app.ProgressDialog
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.drake.net.NetConfig.app
import com.drake.net.NetConfig.converter
import com.drake.net.NetConfig.dialogFactory
import com.drake.net.NetConfig.errorHandler
import com.drake.net.NetConfig.host
import com.drake.net.NetConfig.logEnabled
import com.drake.net.NetConfig.onError
import com.drake.net.NetConfig.requestInterceptor
import com.drake.net.NetConfig.runningCalls
import com.drake.net.convert.NetConverter
import com.drake.net.exception.*
import com.drake.net.interceptor.RequestInterceptor
import com.drake.net.interfaces.NetDialogFactory
import com.drake.net.interfaces.NetErrorHandler
import com.drake.net.okhttp.toNetOkhttp
import com.drake.net.utils.TipUtils
import okhttp3.Call
import okhttp3.OkHttpClient
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import java.util.concurrent.*


/**
 * Net的全局配置
 *
 * @property app 全局上下文, 一般执行[initNet]即可, 无需手动赋值
 * @property host 全局的域名或者ip(baseUrl)
 * @property runningCalls Net中正在运行的请求Call
 * @property requestInterceptor 请求拦截器
 * @property logEnabled 是否启用日志
 * @property dialogFactory 全局加载框
 * @property errorHandler 全局错误处理器, 会覆盖onError/onStateError
 * @property converter 全局数据转换器
 */
@SuppressLint("StaticFieldLeak")
object NetConfig {

    lateinit var app: Application
    var okHttpClient: OkHttpClient = OkHttpClient.Builder().toNetOkhttp().build()
        set(value) {
            field = value.toNetOkhttp()
        }
    var host: String = ""
    var logEnabled = true
    var runningCalls: ConcurrentLinkedQueue<WeakReference<Call>> = ConcurrentLinkedQueue()
        private set
    var requestInterceptor: RequestInterceptor? = null
    var converter: NetConverter = NetConverter
    var errorHandler: NetErrorHandler = NetErrorHandler
    var dialogFactory: NetDialogFactory = NetDialogFactory

    @Deprecated("废弃", replaceWith = ReplaceWith("NetConfig.dialogFactory"))
    var onDialog: (FragmentActivity) -> Dialog = { activity ->
        val progress = ProgressDialog(activity)
        progress.setMessage(activity.getString(R.string.net_dialog_msg))
        progress
    }

    @Deprecated("使用NetErrorHandler统一处理错误", replaceWith = ReplaceWith("NetConfig.errorHandler"))
    var onError: Throwable.() -> Unit = onError@{

        val message = when (this) {
            is UnknownHostException -> app.getString(R.string.net_host_error)
            is URLParseException -> app.getString(R.string.net_url_error)
            is NetConnectException -> app.getString(R.string.net_network_error)
            is NetSocketTimeoutException -> app.getString(
                R.string.net_connect_timeout_error,
                message
            )
            is DownloadFileException -> app.getString(R.string.net_download_error)
            is ConvertException -> app.getString(R.string.net_parse_error)
            is RequestParamsException -> app.getString(R.string.net_request_error)
            is ServerResponseException -> app.getString(R.string.net_server_error)
            is NullPointerException -> app.getString(R.string.net_null_error)
            is NoCacheException -> app.getString(R.string.net_no_cache_error)
            is ResponseException -> message
            is NetException -> app.getString(R.string.net_error)
            else -> app.getString(R.string.net_other_error)
        }

        if (logEnabled) printStackTrace()
        TipUtils.toast(message)
    }


    @Deprecated("使用NetErrorHandler统一处理错误", replaceWith = ReplaceWith("NetConfig.errorHandler"))
    var onStateError: Throwable.(view: View) -> Unit = {
        when (this) {
            is ConvertException,
            is RequestParamsException,
            is ResponseException,
            is NullPointerException -> errorHandler.onError(this)
            else -> if (logEnabled) printStackTrace()
        }
    }

    //<editor-fold desc="初始化">
    /**
     * 初始化框架, 该函数仅在Kotlin下有效
     *
     * @param host 请求url的主机名
     * @param config 进行配置网络请求
     */
    fun init(host: String = "", config: OkHttpClient.Builder.() -> Unit = {}) {
        NetConfig.host = host
        val builder = OkHttpClient.Builder()
        builder.config()
        okHttpClient = builder.toNetOkhttp().build()
    }

    fun init(host: String = "", config: OkHttpClient.Builder) {
        NetConfig.host = host
        okHttpClient = config.toNetOkhttp().build()
    }
    //</editor-fold>
}

