package org.mozilla.xiu.browser.utils

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.mozilla.xiu.browser.R


object StatusUtils {
    fun init(context: Activity) {
        MyStatusBarUtil.setColorNoTranslucent(context, context.resources.getColor(R.color.white))
    }

    fun hideStatusBar(context: Activity) {
        if (Build.VERSION.SDK_INT >= 21) {
            context.window
                .setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
        }
    }

    fun isDarkMode(context: Activity): Boolean {
        val mode: Int =
            context.getResources().getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 为啥不直接用setStatusBarVisibilityFullTheme，因为会出现横屏切换竖屏时屏幕左侧出现和状态栏高度一样的白边
     *
     * @param visible
     */
    fun setStatusBarVisibility(context: Activity, visible: Boolean, rootView: View) {
        if (!visible) {
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            controllerCompat.hide(WindowInsetsCompat.Type.systemBars())
            return
        } else {
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            controllerCompat.show(WindowInsetsCompat.Type.systemBars())
            return
        }
    }
}