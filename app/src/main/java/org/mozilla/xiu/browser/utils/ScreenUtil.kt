package org.mozilla.xiu.browser.utils

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.lang.reflect.Method


/**
 * 作者：By 15968
 * 日期：On 2022/3/4
 * 时间：At 19:34
 */
object ScreenUtil {

    fun getScreenHeight(activity: Activity): Int {
        val manager = activity.windowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        var screenHeight = outMetrics.heightPixels
        if (!isOrientation(activity) && outMetrics.widthPixels > screenHeight) {
            screenHeight = outMetrics.widthPixels
        }
        return screenHeight
    }

    fun getScreenWidth(activity: Context): Int {
        val manager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        var screenWidth = outMetrics.widthPixels
        if (!isOrientation(activity) && outMetrics.heightPixels < screenWidth) {
            screenWidth = outMetrics.heightPixels
        }
        return screenWidth
    }

    fun getScreenMin(context: Context): Int {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        return Math.min(outMetrics.widthPixels, outMetrics.heightPixels)
    }

    fun getScreenMax(context: Context): Int {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        return Math.max(outMetrics.widthPixels, outMetrics.heightPixels)
    }

    /**
     * 获取整个手机屏幕的大小(包括虚拟按钮)
     * 必须在onWindowFocus方法之后使用
     *
     * @param activity
     * @return
     */
    fun getScreenSize(activity: Activity): IntArray? {
        val size = IntArray(2)
        val decorView: View = activity.window.decorView
        size[0] = decorView.getWidth()
        size[1] = decorView.getHeight()
        return size
    }

    fun getScreenWidth3(context: Context): Int {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        manager.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    /**
     * 获取状态栏的高度
     */
    fun getStatusBarHeight(activity: Activity): Int {
        val resources: Resources = activity.resources
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    /**
     * 获取虚拟按键的高度
     */
    fun getNavigationBarHeight(activity: Activity): Int {
        var navigationBarHeight = 0
        val rs: Resources = activity.resources
        val id: Int = rs.getIdentifier("navigation_bar_height", "dimen", "android")
        if (id > 0 && hasNavigationBar(activity)) {
            navigationBarHeight = rs.getDimensionPixelSize(id)
        }
        return navigationBarHeight
    }

    /**
     * 是否存在虚拟按键
     *
     * @return
     */
    private fun hasNavigationBar(activity: Activity): Boolean {
        var hasNavigationBar = false
        val rs: Resources = activity.resources
        val id: Int = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id)
        }
        try {
            @SuppressLint("PrivateApi") val systemPropertiesClass =
                Class.forName("android.os.SystemProperties")
            val m: Method = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            if ("1" == navBarOverride) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (ignored: Exception) {
        }
        return hasNavigationBar
    }

    fun getDisplayMetrics(activity: Activity): DisplayMetrics? {
        return activity
            .resources
            .displayMetrics
    }

    fun toPixels(res: Resources, dp: Float): Int {
        return (dp * res.getDisplayMetrics().density) as Int
    }

    /**
     * Converts sp to px
     *
     * @param res Resources
     * @param sp  the value in sp
     * @return int
     */
    fun toScreenPixels(res: Resources, sp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, res.getDisplayMetrics())
            .toInt()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun isRtl(res: Resources): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                res.getConfiguration().getLayoutDirection() === View.LAYOUT_DIRECTION_RTL
    }

    fun isTablet(context: Context): Boolean {
        return (context.getResources()
            .getConfiguration().screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
                && isPad(context))
    }

    fun isPad(context: Context): Boolean {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val x = Math.pow((dm.widthPixels / dm.xdpi).toDouble(), 2.0)
        val y = Math.pow((dm.heightPixels / dm.ydpi).toDouble(), 2.0)
        // 屏幕尺寸
        val screenInches = Math.sqrt(x + y)
        // 大于7尺寸则为Pad
        return screenInches >= 7.0
    }

    fun isOrientationLand(activity: Context?): Boolean {
        return if (activity == null || activity.resources == null) {
            false
        } else activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    fun isOrientation(activity: Context?): Boolean {
        return if (activity == null || activity.resources == null) {
            false
        } else activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }


    private fun getWindowInsetsController(activity: Activity): WindowInsetsControllerCompat? {
        return if (Build.VERSION.SDK_INT >= 30) {
            //在部分SDK_INT < 30的系统上直接用getWindowInsetsController拿不到
            ViewCompat.getWindowInsetsController(activity.window.decorView)
        } else WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    }

    fun setDisplayInNotch(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val window = activity.window
            // 延伸显示区域到耳朵区
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
    }
}