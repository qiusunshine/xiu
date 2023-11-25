package org.mozilla.xiu.browser.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * 作者：By 15968
 * 日期：On 2023/11/21
 * 时间：At 19:19
 */

fun Context.toActivity(): Activity? {
    return getActivityFromContext(this)
}

fun getActivityFromContext(outerContext: Context): Activity? {
    var context: Context? = outerContext
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}