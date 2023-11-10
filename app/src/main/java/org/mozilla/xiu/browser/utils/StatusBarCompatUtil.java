package org.mozilla.xiu.browser.utils;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;

import com.githang.statusbar.StatusBarCompat;

/**
 * 作者：By 15968
 * 日期：On 2020/6/28
 * 时间：At 20:05
 */
public class StatusBarCompatUtil {
//    public static void setNowColor(int nowColor) {
//        StatusBarCompatUtil.nowColor = nowColor;
//    }
//
//    private static int nowColor;

    public static void setStatusBarColor(Activity activity, int color) {
//        if (nowColor == color) {
//            return;
//        }
//        StatusBarCompat.setStatusBarColor(activity, color);
//        nowColor = color;
        setStatusBarColorForce(activity, color);
    }

    public static void setStatusBarColorForce(Activity activity, int color) {
        if (isDark(activity)) {
            boolean isLightColor = StatusBarCompat.toGrey(color) > 225;
            StatusBarCompat.setStatusBarColor(activity, color, !isLightColor);
        } else {
            StatusBarCompat.setStatusBarColor(activity, color);
        }
    }

    private static boolean isDark(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false;
        }
        int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            return true;
        }
        return false;
    }
}
