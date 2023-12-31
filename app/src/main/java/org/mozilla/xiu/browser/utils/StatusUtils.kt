package org.mozilla.xiu.browser.utils

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.githang.statusbar.StatusBarCompat
import org.mozilla.xiu.browser.R


object StatusUtils {
    fun init(context: Activity, rootView: View) {
        MyStatusBarUtil.setColorNoTranslucent(context, context.resources.getColor(R.color.surface))
        try {
            val isLightColor = StatusBarCompat.toGrey(ContextCompat.getColor(context, R.color.surface)) > 225
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            controllerCompat.isAppearanceLightNavigationBars = isLightColor
            controllerCompat.isAppearanceLightStatusBars = isLightColor
        } catch (e: Exception) {
            e.printStackTrace()
        }
        context.window.navigationBarColor = ContextCompat.getColor(context, R.color.surface)
    }

    fun setStatusBarColor(context: Activity, statusBarColor: Int, rootView: View) {
        MyStatusBarUtil.setColorNoTranslucent(context, statusBarColor)
        try {
            val isLightColor = StatusBarCompat.toGrey(statusBarColor) > 225
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            controllerCompat.isAppearanceLightStatusBars = isLightColor
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
     * 显示或者隐藏状态栏、导航栏
     *
     * @param visible
     */
    fun setStatusBarVisibility(context: Activity, visible: Boolean, rootView: View) {
        if (!visible) {
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            WindowCompat.setDecorFitsSystemWindows(context.window, false)
            controllerCompat.hide(WindowInsetsCompat.Type.systemBars())
            controllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            return
        } else {
            WindowCompat.setDecorFitsSystemWindows(context.window, true)
            val controllerCompat = WindowCompat.getInsetsController(context.window, rootView)
            controllerCompat.show(WindowInsetsCompat.Type.systemBars())
            controllerCompat.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            return
        }
    }
}