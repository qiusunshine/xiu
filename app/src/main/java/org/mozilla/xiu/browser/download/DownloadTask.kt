package org.mozilla.xiu.browser.download

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import org.mozilla.xiu.browser.BR
import org.mozilla.xiu.browser.HolderActivity
import org.mozilla.xiu.browser.R
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent
import org.mozilla.xiu.browser.webextension.WebextensionSession
import rxhttp.RxHttpPlugins
import rxhttp.toDownloadFlow
import rxhttp.wrapper.param.RxHttp
import java.io.File
import org.greenrobot.eventbus.EventBus
import org.mozilla.xiu.browser.utils.UriUtilsPro


class DownloadTask : BaseObservable {
    @get:Bindable
    var title: String = ""

    @get:Bindable
    var currentSize: Long = 0

    @get:Bindable
    var totalSize: Long = 0

    @get:Bindable
    var progress: Int = 0

    @Bindable
    var state: Int = 0

    @get:Bindable
    var text: String = ""

    private var mContext: Context
    var uri: String
    private var downloadFactory: Android10DownloadFactory
    var filename: String
    private lateinit var notificationManager: NotificationManager
    private lateinit var customNotification: Notification

    constructor(mContext: Context, uri: String, filename: String) {
        this.mContext = mContext
        this.uri = uri
        this.filename = filename
        title = filename
        notifyPropertyChanged(BR.title)
        downloadFactory = Android10DownloadFactory(mContext, filename)
        notificationManager = mContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (!notificationManager.areNotificationsEnabled()) {
            Toast.makeText(mContext, "请先授予通知权限", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, "org.mozilla.xiu.browser")
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
                mContext.startActivity(intent)
            }
        }
    }

    fun open() {
        (mContext as FragmentActivity).lifecycleScope.launch {
            RxHttp.get(uri)
                .tag(uri)
                .toDownloadFlow(downloadFactory, true) {
                    progress = it.progress //当前进度 0-100
                    currentSize = it.currentSize / 1024 / 1024 //当前已下载的字节大小
                    totalSize = it.totalSize / 1024 / 1024 //要下载的总字节大小
                    android.util.Log.d("下载进度", "" + progress)
                    text = "已下载$currentSize MB•共$totalSize MB $progress%"

                    if (progress != 100) {
                        var intent = Intent(mContext, HolderActivity::class.java).apply {
                            putExtra("Page", "DOWNLOAD")
                        }
                        initCustomNotification("正在进行下载任务", R.drawable.download, intent)
                        notificationManager.notify(5, customNotification)
                    }
                    notifyChange()

                }.catch {
                }.collect {
                    open(mContext, it)
                    val resolver: ContentResolver = mContext.contentResolver
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    intent.setDataAndType(it, resolver.getType(it));
                    initCustomNotification("下载任务已完成", R.drawable.checkmark_circle, intent)
                    notificationManager.notify(5, customNotification)
                }

        }
        state = 1
        notifyPropertyChanged(BR.state)

    }

    fun pause() {
        RxHttpPlugins.cancelAll(uri)
        state = 0
        notifyPropertyChanged(BR.state)
    }

    fun open(context: Context, uri: Uri) {
        state = 2
        notifyPropertyChanged(BR.state)
        val relativePath: String = Environment.DIRECTORY_DOWNLOADS
        val name = UriUtilsPro.getFileName(uri)
        val file = File("${Environment.getExternalStorageDirectory()}/$relativePath/$name")
        Log.d("open", "open: " + file.absolutePath + ", exist: " + file.exists())
        if (file.exists() && (name.contains(".crx") || name.contains(".xpi"))) {
            EventBus.getDefault().post(WebExtensionsAddEvent(file.absolutePath))
        }
        ToastMgr.shortBottomCenter(App.application, title + "下载完成")
    }

    /**
     * 创建通知渠道
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) =
        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                importance
            )
        )

    @SuppressLint("RemoteViewLayout")
    private fun initCustomNotification(title: String, icon: Int, intent: Intent) {
        //RemoteView
        val remoteViews =
            RemoteViews("org.mozilla.xiu.browser", R.layout.custom_download_notification)
        remoteViews.setTextViewText(R.id.textView33, title)

        val pendingIntent = PendingIntent.getActivity(
            mContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        customNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                "custom",
                "自定义通知",
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            )
            NotificationCompat.Builder(mContext, "custom")
        } else {
            NotificationCompat.Builder(mContext)
        }.apply {
            setSmallIcon(icon)//小图标（显示在状态栏）
            setCustomContentView(remoteViews)//设置自定义内容视图
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)
            setOngoing(true)
            setContentIntent(pendingIntent)
            setAutoCancel(true)
        }.build()
    }


}