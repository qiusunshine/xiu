package org.mozilla.xiu.browser.download

import androidx.lifecycle.LiveData

class DownloadTaskLiveData : LiveData<ArrayList<DownloadTask>>() {
    fun Value(downloadTasks: ArrayList<DownloadTask>){
        postValue(downloadTasks)
    }
    fun remove(downloadTasks: ArrayList<DownloadTask>) {

    }
    override fun onActive() {
        super.onActive()
    }

    override fun onInactive() {
        super.onInactive()
    }
    companion object {
        private lateinit var globalData: DownloadTaskLiveData
        fun getInstance(): DownloadTaskLiveData {
            globalData = if (Companion::globalData.isInitialized) globalData else DownloadTaskLiveData()
            return globalData
        }
    }
}