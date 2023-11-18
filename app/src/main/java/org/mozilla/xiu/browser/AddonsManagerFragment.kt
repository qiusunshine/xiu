package org.mozilla.xiu.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.xiu.browser.componets.AddonsAdapter
import org.mozilla.xiu.browser.databinding.FragmentAddonsManagerBinding
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.utils.ToastMgr
import org.mozilla.xiu.browser.utils.UriUtilsPro
import org.mozilla.xiu.browser.webextension.WebExtensionRuntimeManager
import org.mozilla.xiu.browser.webextension.WebExtensionWrapper
import org.mozilla.xiu.browser.webextension.WebExtensionsRefreshEvent
import org.mozilla.xiu.browser.webextension.WebextensionSession
import java.io.File

class AddonsManagerFragment : Fragment() {
    lateinit var binding: FragmentAddonsManagerBinding
    private lateinit var SheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var SheetBehavior2: BottomSheetBehavior<ConstraintLayout>
    lateinit var webExtensionController: WebExtensionController
    var adapter = AddonsAdapter()
    private lateinit var launcher: ActivityResultLauncher<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWebExtensionsRefresh(event: WebExtensionsRefreshEvent) {
        webExtensionController.list().accept { list ->
            adapter.submitList(list?.map {
                WebExtensionWrapper(
                    it.metaData.name,
                    it.metaData.enabled,
                    it
                )
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddonsManagerBinding.inflate(LayoutInflater.from(requireContext()))
        webExtensionController = GeckoRuntime.getDefault(requireContext()).webExtensionController
        binding.AddonsManagerRecycler.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false);
        binding.AddonsManagerRecycler.adapter = adapter
        webExtensionController.list().accept { list ->
            adapter.submitList(list?.map {
                WebExtensionWrapper(
                    it.metaData.name,
                    it.metaData.enabled,
                    it
                )
            })
        }
        if (binding.managerDrawer != null) {
            SheetBehavior = BottomSheetBehavior.from(binding.managerDrawer as ConstraintLayout)
            SheetBehavior.peekHeight = 0
        }
        if (binding.addDrawer != null) {
            SheetBehavior2 = BottomSheetBehavior.from(binding.addDrawer as ConstraintLayout)
            SheetBehavior2.peekHeight = 0
        }
        adapter.select = object : AddonsAdapter.Select {
            override fun onSelect(bean: WebExtension) {
                openSheet(bean)
            }
        }
        binding.materialButton4.setOnClickListener {
            openAddSheet()
        }
        launcher = registerForActivityResult(
            object : ActivityResultContract<Boolean, Intent?>() {
                override fun createIntent(context: Context, input: Boolean): Intent {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/*"
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    return intent
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
                    return intent
                }

            },
            object :
                ActivityResultCallback<Intent?> {
                override fun onActivityResult(result: Intent?) {
                    if (result == null || result.data == null) {
                        return
                    }
                    val uri = result.data
                    val path: String =
                        UriUtilsPro.getRootDir(context) + File.separator + "_cache" + File.separator + UriUtilsPro.getFileName(
                            uri
                        )
                    UriUtilsPro.getFilePathFromURI(
                        context,
                        uri,
                        path,
                        object : UriUtilsPro.LoadListener {
                            override fun success(s: String) {
                                ThreadTool.runOnUI {
                                    val p = "file://$s"
                                    WebextensionSession(requireActivity()).install(p)
                                }
                            }

                            override fun failed(msg: String) {
                                ToastMgr.shortBottomCenter(
                                    context,
                                    "出错：$msg"
                                )
                            }
                        }
                    )
                }
            })
        return binding.root
    }

    fun openSheet(extension: WebExtension) {
        var metadata: WebExtension.MetaData = extension.metaData
        metadata.icon.getBitmap(72).accept { binding.imageView7.setImageBitmap(it) }
        binding.textView12.text = metadata.name
        binding.textView18.text = metadata.creatorName
        binding.textView19.text = metadata.version
        binding.textView20.text = metadata.description
        SheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        if (metadata.optionsPageUrl != null)
            binding.button3.visibility = View.VISIBLE
        else
            binding.button3.visibility = View.GONE

        binding.button3.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.data = Uri.parse(metadata.optionsPageUrl)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)

        }
        binding.button2.setOnClickListener {
            webExtensionController.uninstall(extension).accept {
                webExtensionController.list().accept { list ->
                    adapter.submitList(list?.map {
                        WebExtensionWrapper(
                            it.metaData.name,
                            it.metaData.enabled,
                            it
                        )
                    })
                }
                WebExtensionRuntimeManager.refresh()
            }
            SheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        }
    }

    private fun openAddSheet() {
        SheetBehavior2.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        binding.addFromFirefox.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.data = Uri.parse("https://addons.mozilla.org/zh-CN/firefox/")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        binding.addFromLocal.setOnClickListener {
            SheetBehavior2.state = BottomSheetBehavior.STATE_COLLAPSED
            launcher.launch(true)
        }
        binding.addFromOther.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("使用协议")
                .setMessage("第三方网站内的扩展程序由网站所有者提供，网站内所有扩展、广告、内容等均与本软件无关，请注意甄别信息真假性、合法性、合规性，由此产生的任何争议，应与网站所有者协商或诉讼解决")
                .setPositiveButton("同意") { _, _ ->
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.data = Uri.parse("https://www.crxsoso.com/")
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }.setNegativeButton("取消") { d, _ ->
                    d.dismiss()
                }.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
    }
}