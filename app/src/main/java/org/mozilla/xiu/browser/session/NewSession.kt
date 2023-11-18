package org.mozilla.xiu.browser.session

import android.app.Activity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.xiu.browser.componets.HomeLivedata

fun createSession(uri: String?, activity: Activity): GeckoSession {
    HomeLivedata.getInstance().Value(false)
    val session = GeckoSession()
    val sessionSettings = session.settings
    val geckoViewModel =
        activity?.let { ViewModelProvider(it as ViewModelStoreOwner)[GeckoViewModel::class.java] }!!
    SeRuSettings(sessionSettings, activity)

    activity?.let { GeckoRuntime.getDefault(it) }?.let { session.open(it) }
    if (uri != null) {
        session.loadUri(uri)
    }
    geckoViewModel.changeSearch(session)
    return session
}