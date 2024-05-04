package org.mozilla.xiu.browser.broswer

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContentProviderCompat.requireContext
import org.mozilla.xiu.browser.MainActivity
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.BaseTranslucentActivity
import org.mozilla.xiu.browser.session.createSession
import org.mozilla.xiu.browser.utils.StrUtil
import org.mozilla.xiu.browser.utils.StringUtil
import timber.log.Timber

class SearchIntentActivity : BaseTranslucentActivity() {
    override fun onNewIntent(intent: Intent) {
        resolveIntent(intent)
        finish()
        super.onNewIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        if ("android.intent.action.WEB_SEARCH" == intent.action) {
            val query = intent.getStringExtra("query")
            if (StringUtil.isNotEmpty(query)) {
                dealIntentUrl("hiker://search?s=$query")
                return
            }
        }
        val t = intent.getCharSequenceExtra("android.intent.extra.PROCESS_TEXT")
        var text: String? = t?.toString() ?: ""
        if (canDeal(text)) {
            dealIntentUrl(text)
            return
        } else if (StringUtil.isNotEmpty(text)) {
            dealIntentUrl("hiker://search?s=$text")
            return
        }
        text = intent.dataString
        if (canDeal(text)) {
            dealIntentUrl(text)
            return
        } else if (StringUtil.isNotEmpty(text)) {
            dealIntentUrl("hiker://search?s=$text")
            return
        }
        text = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (canDeal(text)) {
            dealIntentUrl(text)
            return
        } else if (StringUtil.isNotEmpty(text)) {
            dealIntentUrl("hiker://search?s=$text")
            return
        }
        text = intent.getStringExtra(Intent.EXTRA_HTML_TEXT)
        if (canDeal(text)) {
            dealIntentUrl(text)
            return
        } else if (StringUtil.isNotEmpty(text)) {
            dealIntentUrl("hiker://search?s=$text")
            return
        }
        val st = if (intent.extras == null) null else intent.extras!![Intent.EXTRA_STREAM]
        if (st != null) {
            text = st.toString()
            if (text.startsWith("file://") || text.startsWith("content://")) {
                dealIntentUrl(text)
                return
            }
        }
        val uri = intent.data
        if (uri != null && canDeal(uri.toString())) {
            dealIntentUrl(uri.toString())
        } else if (StringUtil.isNotEmpty(text)) {
            dealIntentUrl("hiker://search?s=$text")
        }
    }

    private fun canDeal(text: String?): Boolean {
        if (StringUtil.isEmpty(text)) {
            return false
        }
        val scheme =
            (text!!.startsWith("http") || text.startsWith("file") || text.startsWith("hiker") || text.startsWith(
                "content://"
            )
                    || text.startsWith("magnet") || text.startsWith("ed2k") || text.startsWith("ftp"))
        return if (!scheme && text.contains("\nhttp")) {
            true
        } else scheme
    }

    private fun dealIntentUrl(url: String?) {
//        if (url.startsWith("file")) {
//            if (!url.contains(UriUtils.getRootDir(getContext()))) {
//                ToastMgr.shortBottomCenter(getContext(), "没有申请存储权限哦，不能访问文件" + url);
//                return;
//            }
//        }
        var url = url
        if (url!!.contains("\nhttp")) {
            url = "http" + url.split("\nhttp".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[1]
        }
        Timber.i("dealIntentUrl: %s", url)

        val intent = Intent(context, MainActivity::class.java)
        if ((getResources().getConfiguration().screenLayout and
                    Configuration.SCREENLAYOUT_SIZE_MASK) ===
            Configuration.SCREENLAYOUT_SIZE_LARGE
        ) {
            val uu = if(StrUtil.isGeckoUrl(url)) {
                url
            } else {
                val a = url.replace("hiker://search?s=", "")
                "${SearchEngine(this)}$a"
            }
            createSession(uu, this)
        } else {
            intent.data = Uri.parse(url)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    override fun initLayout(savedInstanceState: Bundle?): Int {
        return R.layout.activit_intent
    }

    override fun initView() {}
    override fun initData(savedInstanceState: Bundle?) {
        resolveIntent(intent)
        finish()
    }

    companion object {
        private const val TAG = "SearchIntentActivity"
    }
}