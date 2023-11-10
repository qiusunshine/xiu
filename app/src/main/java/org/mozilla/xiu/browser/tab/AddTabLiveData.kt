package org.mozilla.xiu.browser.tab

import androidx.lifecycle.LiveData

class AddTabLiveData : LiveData<Int>() {
    fun Value(i: Int){
        postValue(i)
    }
    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }
    companion object {
        private lateinit var globalData: AddTabLiveData
        fun getInstance(): AddTabLiveData {
            globalData = if (Companion::globalData.isInitialized) globalData else AddTabLiveData()
            return globalData
        }
    }
}