package org.mozilla.xiu.browser.utils.filePicker

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.xiu.browser.utils.filePicker.FilePicker.UriListener

/**
 * 作者：By 15968
 * 日期：On 2023/11/19
 * 时间：At 20:21
 */
fun getFileFromPicker(
    activity: FragmentActivity,
    filePicker: FilePicker,
    prompt: GeckoSession.PromptDelegate.FilePrompt,
    result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
) {
    filePicker.putUriListener(object : UriListener {
        override fun UriGet(uri: Uri?) {
            if (uri != null) {
                result.complete(prompt.confirm(activity, uri))
            }
        }
    })
    filePicker.open(activity, prompt.mimeTypes ?: arrayOf())
}