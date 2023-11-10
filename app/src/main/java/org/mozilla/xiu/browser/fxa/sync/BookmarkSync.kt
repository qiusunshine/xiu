package org.mozilla.xiu.browser.fxa.sync

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import org.mozilla.xiu.browser.fxa.AccountManagerCollection
import kotlinx.coroutines.launch
import mozilla.components.browser.storage.sync.PlacesBookmarksStorage
import mozilla.components.service.fxa.SyncEngine
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.service.fxa.sync.GlobalSyncableStoreProvider
import mozilla.components.service.fxa.sync.SyncReason

class BookmarkSync (val context: Context) {
    private var bookmarkStorage = lazy {
        PlacesBookmarksStorage(context)
    }
    private var accountManagerCollection: AccountManagerCollection = ViewModelProvider(context as ViewModelStoreOwner)[AccountManagerCollection::class.java]
    private lateinit var accountManager: FxaAccountManager

    init {
        GlobalSyncableStoreProvider.configureStore(SyncEngine.Bookmarks to bookmarkStorage)
        (context as LifecycleOwner).lifecycleScope.launch {
            accountManagerCollection.data.collect(){
                accountManager = it


            }

        }
    }

    fun sync(url :String,title: String){
        (context as LifecycleOwner).lifecycleScope.launch {
            var a = 1
            bookmarkStorage.value.addItem("mobile______",url,title,a.toUInt())
            accountManager.syncNow(SyncReason.User)



        }
    }
}