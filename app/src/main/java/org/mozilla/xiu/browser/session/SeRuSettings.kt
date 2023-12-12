package org.mozilla.xiu.browser.session

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.ActivityInfo
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.Autocomplete
import org.mozilla.geckoview.Autocomplete.StorageDelegate
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.ContentBlocking.EtpLevel
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntime.ServiceWorkerDelegate
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoRuntimeSettings.ALLOW_ALL
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.OrientationController.OrientationDelegate
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate
import org.mozilla.geckoview.WebRequest
import org.mozilla.geckoview.WebResponse
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.utils.ScreenUtil
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.getSizeName

const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/120.0"

class SeRuSettings {
    private var geckoSessionSettings: GeckoSessionSettings
    private lateinit var geckoRuntimeSettings: GeckoRuntimeSettings
    var activity: Activity
    private val CHANNEL_ID = "Xiu Browser"

    constructor(
        geckoSessionSettings: GeckoSessionSettings,
        activity: Activity
    ) {
        this.geckoSessionSettings = geckoSessionSettings
        this.activity = activity
        geckoRuntimeSettings = GeckoRuntime.getDefault(activity).settings
        if ((getSizeName(activity) == "large" && ScreenUtil.isOrientationLand(activity)) || desktopMode) {
            //geckoSessionSettings.userAgentOverride = DESKTOP_UA
            geckoSessionSettings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            geckoSessionSettings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            geckoSessionSettings.displayMode = GeckoSessionSettings.DISPLAY_MODE_BROWSER
        } else {
            geckoSessionSettings.userAgentMode = GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            geckoSessionSettings.viewportMode = GeckoSessionSettings.VIEWPORT_MODE_MOBILE
        }
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(activity /* Activity context */)
        geckoRuntimeSettings.forceUserScalableEnabled =
            sharedPreferences.getBoolean("switch_userscalable", false)
        geckoRuntimeSettings.automaticFontSizeAdjustment =
            sharedPreferences.getBoolean("switch_automatic_fontsize", false)
        init(activity)
        val sGeckoRuntime = GeckoRuntime.getDefault(activity)
        sGeckoRuntime.orientationController.delegate =
            ExampleOrientationDelegate(activity)
        sGeckoRuntime.serviceWorkerDelegate = ServiceWorkerDelegate { url ->
            GeckoResult.fromValue(createSession(url, activity))
        }

        // `getSystemService` call requires API level 23
        sGeckoRuntime.webNotificationDelegate = object : WebNotificationDelegate {
            var notificationManager: NotificationManager =
                activity.getSystemService(
                    NotificationManager::class.java
                )

            @RequiresApi(Build.VERSION_CODES.O)
            private fun createNotificationChannel(): String {
                val channelId = CHANNEL_ID
                val channelName = ContextCompat.getString(activity, R.string.extensions)
                val chan = NotificationChannel(
                    channelId,
                    channelName, NotificationManager.IMPORTANCE_HIGH
                )
                chan.lightColor = Color.BLUE
                chan.importance = NotificationManager.IMPORTANCE_HIGH
                chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                val service =
                    activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                service?.createNotificationChannel(chan)
                return channelId
            }

            override fun onShowNotification(notification: WebNotification) {
                ThreadTool.runOnUI {
                    val clickIntent = Intent(
                        activity,
                        activity::class.java
                    )
                    clickIntent.putExtra("onClick", notification)
                    val dismissIntent = PendingIntent.getActivity(
                        activity,
                        mLastID,
                        clickIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )

                    val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        createNotificationChannel()
                    } else {
                        CHANNEL_ID
                    }
                    val builder: NotificationCompat.Builder = NotificationCompat.Builder(
                        activity,
                        channelId
                    )
                        .setContentTitle(notification.title)
                        .setContentText(notification.text)
                        .setSmallIcon(R.drawable.ic_status_logo)
                        .setContentIntent(dismissIntent)
                        .setAutoCancel(true)
                    mNotificationIDMap[notification.tag] = mLastID
                    if (notification.imageUrl != null && notification.imageUrl!!.isNotEmpty()) {
                        try {
                            if (notification.imageUrl!!.startsWith("data")) {
                                ThreadTool.async {
                                    try {
                                        if (notification.imageUrl!!.startsWith("data:image/svg+xml")) {
                                            //写入本地
                                        }
                                        val bitmap = Glide.with(App.getContext()).asBitmap()
                                            .load(notification.imageUrl).submit().get()
                                        if (bitmap == null) {
                                            withContext(Dispatchers.Main) {
                                                notificationManager.notify(
                                                    mLastID++,
                                                    builder.build()
                                                )
                                                ToastMgr.shortBottomCenter(
                                                    activity,
                                                    notification.title + ": " + notification.text
                                                )
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                builder.setLargeIcon(bitmap)
                                                notificationManager.notify(
                                                    mLastID++,
                                                    builder.build()
                                                )
                                                ToastMgr.shortBottomCenter(
                                                    activity,
                                                    notification.title + ": " + notification.text
                                                )
                                            }
                                        }
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) {
                                            notificationManager.notify(mLastID++, builder.build())
                                            ToastMgr.shortBottomCenter(
                                                activity,
                                                notification.title + ": " + notification.text
                                            )
                                        }
                                    }
                                }
                                return@runOnUI
                            }
                            val executor =
                                GeckoWebExecutor(GeckoRuntime.getDefault(activity))
                            val response = executor.fetch(
                                WebRequest.Builder(notification.imageUrl!!)
                                    .addHeader("Accept", "image")
                                    .build()
                            )
                            response.accept { value: WebResponse? ->
                                val bitmap = BitmapFactory.decodeStream(
                                    value!!.body
                                )
                                ThreadTool.runOnUI {
                                    builder.setLargeIcon(bitmap)
                                    notificationManager.notify(mLastID++, builder.build())
                                    ToastMgr.shortBottomCenter(
                                        activity,
                                        notification.title + ": " + notification.text
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        notificationManager.notify(mLastID++, builder.build())
                    }
                }
            }

            override fun onCloseNotification(notification: WebNotification) {
                ThreadTool.runOnUI {
                    if (mNotificationIDMap.containsKey(notification.tag)) {
                        val id: Int? = mNotificationIDMap[notification.tag]
                        if (id != null) {
                            notificationManager.cancel(id)
                            mNotificationIDMap.remove(notification.tag)
                        }
                    }
                    notification.dismiss()
                }
            }
        }

        sGeckoRuntime.setDelegate {
            mKillProcessOnDestroy = true
            activity.finish()
        }

        sGeckoRuntime.setActivityDelegate { pendingIntent: PendingIntent ->
            val result = GeckoResult<Intent>()
            try {
                val code: Int = mNextActivityResultCode++
                mPendingActivityResult[code] = result
                activity.startIntentSenderForResult(
                    pendingIntent.intentSender, code, null, 0, 0, 0
                )
            } catch (e: SendIntentException) {
                result.completeExceptionally(e)
            }
            result
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                "switch_userscalable" -> geckoRuntimeSettings.forceUserScalableEnabled =
                    sharedPreferences.getBoolean("switch_userscalable", false)

                "switch_automatic_fontsize" -> geckoRuntimeSettings.automaticFontSizeAdjustment =
                    sharedPreferences.getBoolean("switch_automatic_fontsize", false)
            }
        }
    }

    private class ExampleAutocompleteStorageDelegate : StorageDelegate {
        private val mStorage: MutableMap<String?, Autocomplete.LoginEntry> = HashMap()
        override fun onLoginFetch(): GeckoResult<Array<Autocomplete.LoginEntry>> {
            return GeckoResult.fromValue(mStorage.values.toTypedArray())
        }

        override fun onLoginSave(login: Autocomplete.LoginEntry) {
            mStorage[login.guid] = login
        }
    }

    private class ExampleOrientationDelegate(val activity: Activity) : OrientationDelegate {
        override fun onOrientationLock(aOrientation: Int): GeckoResult<AllowOrDeny> {
            activity.requestedOrientation = aOrientation
            return GeckoResult.allow()
        }

        override fun onOrientationUnlock() {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    companion object {
        private var inited: Boolean = false
        private var mLastID = 100
        var desktopMode = false
        var mKillProcessOnDestroy = false
        val mPendingActivityResult = java.util.HashMap<Int, GeckoResult<Intent>>()

        private var mNextActivityResultCode = 10

        private val mNotificationIDMap = java.util.HashMap<String, Int>()
        fun init(activity: Activity) {
            if (inited) return
            inited = true
            val geckoRuntimeSettings = GeckoRuntime.getDefault(activity).settings
            geckoRuntimeSettings.consoleOutputEnabled = true
            geckoRuntimeSettings.aboutConfigEnabled = true
            geckoRuntimeSettings.webFontsEnabled = true
            geckoRuntimeSettings.loginAutofillEnabled = true
            geckoRuntimeSettings.doubleTapZoomingEnabled = true
            geckoRuntimeSettings.allowInsecureConnections = ALLOW_ALL
            geckoRuntimeSettings.webManifestEnabled = true
//        geckoRuntimeSettings.setExtensionsProcessEnabled(
//            sharedPreferences.getBoolean(
//                "switch_extension_process",
//                false
//            )
//        )
            geckoRuntimeSettings.setExtensionsProcessEnabled(
                false
            )
            GeckoRuntime.getDefault(activity).settings.contentBlocking.setSafeBrowsing(
                ContentBlocking.SafeBrowsing.NONE
            )
            GeckoRuntime.getDefault(activity).settings.contentBlocking.enhancedTrackingProtectionLevel =
                EtpLevel.NONE
            geckoRuntimeSettings.contentBlocking.setSafeBrowsing(ContentBlocking.SafeBrowsing.NONE)

            GeckoRuntime.getDefault(activity).autocompleteStorageDelegate =
                ExampleAutocompleteStorageDelegate()
        }
    }
}