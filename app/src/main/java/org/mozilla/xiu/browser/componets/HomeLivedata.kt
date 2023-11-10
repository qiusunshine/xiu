package org.mozilla.xiu.browser.componets

import androidx.lifecycle.LiveData

class HomeLivedata: LiveData<Boolean>() {
    fun Value(boolean: Boolean){
        postValue(boolean)
    }
    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }
    companion object {
        private lateinit var globalData: HomeLivedata
        fun getInstance(): HomeLivedata {
            globalData = if (Companion::globalData.isInitialized) globalData else HomeLivedata()
            return globalData
        }
    }
}