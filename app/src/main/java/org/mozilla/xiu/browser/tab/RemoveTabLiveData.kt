package org.mozilla.xiu.browser.tab

import androidx.lifecycle.LiveData
import org.mozilla.xiu.browser.session.SessionDelegate

class RemoveTabLiveData : LiveData<SessionDelegate>() {
    fun Value(item: SessionDelegate?) {
        item?.let {
            postValue(it)
        }
    }

    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }

    companion object {
        private lateinit var globalData: RemoveTabLiveData
        fun getInstance(): RemoveTabLiveData {
            globalData =
                if (Companion::globalData.isInitialized) globalData else RemoveTabLiveData()
            return globalData
        }
    }
}