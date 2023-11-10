package org.mozilla.xiu.browser.fxa.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.concept.storage.PageVisit
import mozilla.components.concept.storage.VisitType
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.GlobalSyncableStoreProvider
import mozilla.components.service.fxa.sync.SyncReason
import org.mozilla.xiu.browser.fxa.AccountManagerCollection

//todo 崩溃，报 undefined symbol: ffi_glean_64d5_OnGleanEvents_init_callback
class HistorySync(val context: Context) {
    private var historyStorage: Lazy<PlacesHistoryStorage> = lazy {
        PlacesHistoryStorage(context)
    }
    private var accountManagerCollection: AccountManagerCollection =
        ViewModelProvider(context as ViewModelStoreOwner)[AccountManagerCollection::class.java]
    private lateinit var accountManager: FxaAccountManager

    init {
        GlobalSyncableStoreProvider.configureStore(SyncEngine.History to historyStorage)
        (context as LifecycleOwner).lifecycleScope.launch {
            accountManagerCollection.data.collect() {
                accountManager = it


            }

        }
    }

    fun sync(url: String) {
        (context as LifecycleOwner).lifecycleScope.launch {
            historyStorage.value.recordVisit(url, PageVisit(VisitType.LINK))
            accountManager.syncNow(SyncReason.User)


        }
    }
}