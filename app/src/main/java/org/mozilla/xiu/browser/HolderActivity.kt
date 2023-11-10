package org.mozilla.xiu.browser

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mozilla.xiu.browser.databinding.ActivityHolderBinding
import org.mozilla.xiu.browser.utils.StatusUtils
import org.mozilla.xiu.browser.webextension.BrowseEvent
import org.mozilla.xiu.browser.webextension.WebExtensionsAddEvent

class HolderActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHolderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

        binding = ActivityHolderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        StatusUtils.init(this)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_holder)
        val navInflater = navController.navInflater
        val navGraph = navInflater.inflate(R.navigation.nav_graph2)
        when (intent.getStringExtra("Page")) {
            "DOWNLOAD" -> navGraph.setStartDestination(R.id.downloadFragment)
            "ADDONS" -> navGraph.setStartDestination(R.id.addonsManagerFragment)
            "SETTINGS" -> navGraph.setStartDestination(R.id.settingsFragment)
            "QRSCANNING" -> {
                navGraph.setStartDestination(R.id.qrScanningFragment)
                binding.toolbar.visibility = View.GONE
            }

        }
        navController.graph=navGraph
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun addWebExtension(event: WebExtensionsAddEvent) {
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun browse(event: BrowseEvent) {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_holder)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}