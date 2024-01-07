package org.mozilla.xiu.browser.fxa.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.PageObservation
import mozilla.components.service.fxa.manager.FxaAccountManager
import org.mozilla.xiu.browser.ActivityManager
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import org.mozilla.xiu.browser.fxa.Fxa.Companion.historyStorage

class HistorySync {
    private lateinit var accountManager: FxaAccountManager
    fun initAccountManager(context: Context) {
        val accountManagerCollection: AccountManagerCollection =
            ViewModelProvider(context as ViewModelStoreOwner)[AccountManagerCollection::class.java]
        lifecycleScope.launch {
            accountManagerCollection.data.collect {
                accountManager = it
            }
        }
    }

    private val lifecycleScope get() = (ActivityManager.instance.currentActivity as LifecycleOwner).lifecycleScope

    fun sync(title: String, url: String) {
        if(!::accountManager.isInitialized) {
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            historyStorage.value.recordObservation(url, PageObservation(title))
        }
    }
}