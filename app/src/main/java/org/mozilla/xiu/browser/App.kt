package org.mozilla.xiu.browser

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.google.android.material.color.DynamicColors
import com.kongzue.dialogx.DialogX
import java.util.Timer
import kotlin.concurrent.timerTask

open class App: Application() {

    override fun onCreate() {
        super.onCreate()
        // apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
        DialogX.init(this)
        //syncLooper()
        application = this
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        xcrash.XCrash.init(this)


    }


    fun syncLooper(){
        val timer=Timer()
        timer.schedule(timerTask {
            Toast.makeText(applicationContext,"",Toast.LENGTH_SHORT)

        },5000,5000)
    }

    companion object {
        @JvmField
        var application: Application? = null
    }
}