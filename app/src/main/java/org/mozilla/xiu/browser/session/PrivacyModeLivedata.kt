package org.mozilla.xiu.browser.session

import androidx.lifecycle.LiveData
import org.mozilla.xiu.browser.App
import org.mozilla.xiu.browser.utils.PreferenceMgr

class PrivacyModeLivedata(t: Boolean) : LiveData<Boolean>(t) {
    fun Value(t: Boolean) {
        postValue(t)
    }

    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }

    companion object {
        private lateinit var globalData: PrivacyModeLivedata
        fun getInstance(): PrivacyModeLivedata {
            globalData =
                if (Companion::globalData.isInitialized) globalData else PrivacyModeLivedata(
                    PreferenceMgr.getBoolean(App.getContext(), "privacyMode", false)
                )
            return globalData
        }
    }
}