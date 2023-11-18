package org.mozilla.xiu.browser.view.toast

/**
 * 作者：By 15968
 * 日期：On 2023/3/23
 * 时间：At 0:57
 */

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import org.mozilla.xiu.browser.R
import org.mozilla.xiu.browser.utils.DisplayUtil
import org.mozilla.xiu.browser.view.toast.ChefSnackbar.Companion.make

class ChefSnackbar(
    parent: ViewGroup,
    val content: ChefSnackbarView
) : BaseTransientBottomBar<ChefSnackbar>(parent, content, content) {

    init {
        getView().setBackgroundColor(
            ContextCompat.getColor(
                view.context,
                android.R.color.transparent
            )
        )
        val dp10 = DisplayUtil.dpToPx(context, 10)
        getView().setPadding(dp10, dp10, dp10, dp10)

        val view: View = content
        val background = GradientDrawable()
        background.shape = GradientDrawable.RECTANGLE
        background.setColor(ContextCompat.getColor(context, R.color.dark_edit))
        background.cornerRadius = DisplayUtil.dpToPx(context, 6).toFloat()
        view.background = background
    }

    fun setConfirmButton(text: String, action: (v: View) -> Unit): ChefSnackbar {
        val textView: TextView = content.findViewById(R.id.btn_ok)
        textView.text = text
        textView.setOnClickListener { v ->
            dismiss()
            action(v)
        }
        return this
    }

    fun setCancelButton(text: String, action: (v: View) -> Unit = {}): ChefSnackbar {
        val textView: TextView = content.findViewById(R.id.btn_cancel)
        textView.text = text
        textView.setOnClickListener { v ->
            dismiss()
            action(v)
        }
        return this
    }

    fun setText(text: String): ChefSnackbar {
        val textView: TextView = content.findViewById(R.id.title)
        textView.text = text
        return this
    }

    fun setAction(t: String, listener: View.OnClickListener): ChefSnackbar {
        setConfirmButton(t) {
            listener.onClick(it)
        }
        return this
    }

    companion object {

        fun make(view: View): ChefSnackbar {

            // First we find a suitable parent for our custom view
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                "No suitable parent found from the given view. Please provide a valid view."
            )

            // We inflate our custom view
            val customView = LayoutInflater.from(view.context).inflate(
                R.layout.layout_snackbar_chef,
                parent,
                false
            ) as ChefSnackbarView

            // We create and return our Snackbar
            return ChefSnackbar(
                parent,
                customView
            )
        }

    }

}

fun make(view: View, text: String, length: Int): ChefSnackbar {
    return make(view)
        .setDuration(length)
        .setText(text)
        .setCancelButton("取消") { v: View? -> }
}