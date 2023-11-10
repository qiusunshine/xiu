package org.mozilla.xiu.browser.tab

import androidx.lifecycle.LiveData
import org.mozilla.xiu.browser.session.SessionDelegate

class DelegateListLiveData : LiveData<ArrayList<SessionDelegate>>() {
    fun Value(sessionDelegates:ArrayList<SessionDelegate>){
        postValue(sessionDelegates)
    }
    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }
    companion object {
        private lateinit var globalData: DelegateListLiveData
        fun getInstance(): DelegateListLiveData {
            globalData = if (Companion::globalData.isInitialized) globalData else DelegateListLiveData()
            return globalData
        }
    }
}