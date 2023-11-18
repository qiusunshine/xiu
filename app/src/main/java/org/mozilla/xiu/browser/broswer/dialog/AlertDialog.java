package org.mozilla.xiu.browser.broswer.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class AlertDialog {
    Context context;
    androidx.appcompat.app.AlertDialog dialog;

    public AlertDialog(@NonNull Context context, GeckoSession.PromptDelegate.AlertPrompt alertPrompt,
                       GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        this.context = context;
        dialog = onCreate(alertPrompt, context, result);
    }

    public androidx.appcompat.app.AlertDialog onCreate(GeckoSession.PromptDelegate.AlertPrompt alertPrompt, Context context,
                                                       GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        return new MaterialAlertDialogBuilder(context)
                .setTitle(alertPrompt.title)
                .setMessage(alertPrompt.message)
                .setPositiveButton("确认", (dialogInterface, i) -> endDialog(alertPrompt.dismiss(), result))
                .setOnDismissListener(dialogInterface -> {
                    if (!alertPrompt.isComplete()) {
                        result.complete(alertPrompt.dismiss());
                    }
                })
                .create();
    }

    public void show() {
        dialog.show();
    }

    public void endDialog(GeckoSession.PromptDelegate.PromptResponse result1, GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        result.complete(result1);
        dialog.dismiss();
    }
}
