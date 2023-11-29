package org.mozilla.xiu.browser.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.LifecycleOwner

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

/**
 * 绑定返回键回调（建议使用该方法）
 * @param owner Receive callbacks to a new OnBackPressedCallback when the given LifecycleOwner is at least started.
 * This will automatically call addCallback(OnBackPressedCallback) and remove the callback as the lifecycle state changes. As a corollary, if your lifecycle is already at least started, calling this method will result in an immediate call to addCallback(OnBackPressedCallback).
 * When the LifecycleOwner is destroyed, it will automatically be removed from the list of callbacks. The only time you would need to manually call OnBackPressedCallback.remove() is if you'd like to remove the callback prior to destruction of the associated lifecycle.
 * @param onBackPressed 回调方法；返回true则表示消耗了按键事件，事件不会继续往下传递，相反返回false则表示没有消耗，事件继续往下传递
 * @return 注册的回调对象，如果想要移除注册的回调，直接通过调用[OnBackPressedCallback.remove]方法即可。
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(
    owner: LifecycleOwner,
    onBackPressed: () -> Boolean
): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(owner, it)
    }
}

/**
 * 绑定返回键回调，未关联生命周期，建议使用关联生命周期的办法（尤其在fragment中使用，应该关联fragment的生命周期）
 */
fun androidx.activity.ComponentActivity.addOnBackPressed(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(it)
    }
}
private fun androidx.activity.ComponentActivity.backPressedCallback(onBackPressed: () -> Boolean):OnBackPressedCallback{
    return object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!onBackPressed()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}

fun androidx.activity.ComponentDialog.addOnBackPressed(
    owner: LifecycleOwner,
    onBackPressed: () -> Boolean
): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(owner, it)
    }
}

/**
 * 绑定返回键回调，未关联生命周期，建议使用关联生命周期的办法（尤其在fragment中使用，应该关联fragment的生命周期）
 */
fun androidx.activity.ComponentDialog.addOnBackPressed(onBackPressed: () -> Boolean): OnBackPressedCallback {
    return backPressedCallback(onBackPressed).also {
        onBackPressedDispatcher.addCallback(it)
    }
}

private fun androidx.activity.ComponentDialog.backPressedCallback(onBackPressed: () -> Boolean):OnBackPressedCallback{
    return object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!onBackPressed()) {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }
}