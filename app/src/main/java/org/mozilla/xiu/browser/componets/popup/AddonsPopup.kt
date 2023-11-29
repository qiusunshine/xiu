package org.mozilla.xiu.browser.componets.popup

import android.content.Context
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.ProgressDelegate
import org.mozilla.geckoview.WebExtension
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.base.addOnBackPressed
import org.mozilla.xiu.browser.databinding.PopupAddonsBinding
import org.mozilla.xiu.browser.utils.ThreadTool
import org.mozilla.xiu.browser.webextension.WebExtensionAddTabEvent

class AddonsPopup {
    val context: Context
    lateinit var bottomSheetDialog: BottomSheetDialog
    lateinit var binding: PopupAddonsBinding
    private var extensionId: String? = null
    private var sessionState: GeckoSession.SessionState? = null

    constructor(
        context: Context,
    ) {
        this.context = context
        bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        binding = PopupAddonsBinding.inflate(LayoutInflater.from(context))
        bottomSheetDialog.setContentView(binding.root)
        bottomSheetDialog.behavior.isDraggable = false
        binding.button.setOnClickListener { bottomSheetDialog.dismiss() }
    }

    fun show(session: GeckoSession, extension: WebExtension) {
        extensionId = extension.id
        session.open(GeckoRuntime.getDefault(context))
        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ChoicePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                //prompt.
                val jsChoiceDialog =
                    org.mozilla.xiu.browser.broswer.dialog.JsChoiceDialog(
                        context,
                        prompt,
                        result
                    )
                jsChoiceDialog.show()
                return result
            }

            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val alertDialog =
                    org.mozilla.xiu.browser.broswer.dialog.AlertDialog(context, prompt, result)
                alertDialog.show()
                return result
            }
        }
        session.progressDelegate = object : ProgressDelegate {
            override fun onSessionStateChange(
                session: GeckoSession,
                sessionState: GeckoSession.SessionState
            ) {
                this@AddonsPopup.sessionState = sessionState
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onKill(session: GeckoSession) {
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(context))
                }
                if (sessionState != null) {
                    session.restoreState(sessionState!!)
                }
            }

            override fun onCrash(session: GeckoSession) {
                if (!session.isOpen) {
                    session.open(GeckoRuntime.getDefault(context))
                }
                if (sessionState != null) {
                    session.restoreState(sessionState!!)
                }
            }
        }
        context as LifecycleOwner
        context.lifecycleScope.launch {
            extension.metaData.icon.getBitmap(72).accept { binding.imageView5.setImageBitmap(it) }

            binding.textView.text = extension.metaData.name
        }
        binding.addonsView.coverUntilFirstPaint(ContextCompat.getColor(context, R.color.surface))
        binding.addonsView.setSession(session)
        bottomSheetDialog.show()
        val onBackPressedCallback = bottomSheetDialog.addOnBackPressed {
            false
        }
        bottomSheetDialog.setOnDismissListener {
            onBackPressedCallback.remove()
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
        }
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    @Subscribe
    fun onWebExtensionAddTab(event: WebExtensionAddTabEvent) {
        if (extensionId != event.id) {
            return
        }
        ThreadTool.runOnUI {
            if (bottomSheetDialog.isShowing) {
                bottomSheetDialog.dismiss()
            }
        }
    }
}