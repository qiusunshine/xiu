package org.mozilla.xiu.browser.broswer.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class ButtonDialog {
    private Context context;
    private GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result;
    androidx.appcompat.app.AlertDialog dialog;

    public ButtonDialog(@NonNull Context context, GeckoSession.PromptDelegate.ButtonPrompt alertPrompt,
                        GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        this.context = context;
        this.result = result;
        dialog = onCreate(alertPrompt, context);
    }

    public androidx.appcompat.app.AlertDialog onCreate(GeckoSession.PromptDelegate.ButtonPrompt alertPrompt, Context context) {
        return new MaterialAlertDialogBuilder(context)
                .setTitle(alertPrompt.title)
                .setMessage(alertPrompt.message)
                .setPositiveButton("确认", (dialogInterface, i) -> endDialog(alertPrompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE)))
                .setNegativeButton("取消", (dialogInterface, i) -> endDialog(alertPrompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE)))
                .setOnDismissListener(dialogInterface -> {
                    if (!alertPrompt.isComplete()) {
                        result.complete(alertPrompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE));
                    }
                })
                .create();
    }

    public void show() {
        dialog.show();
    }

    public void endDialog(GeckoSession.PromptDelegate.PromptResponse result1) {
        result.complete(result1);
        dialog.dismiss();
    }
}
