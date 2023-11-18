package org.mozilla.xiu.browser.broswer.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class ConfirmDialog {
    Context context;

    androidx.appcompat.app.AlertDialog dialog;

    public ConfirmDialog(@NonNull Context context, GeckoSession.PromptDelegate.RepostConfirmPrompt alertPrompt,
                       GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        this.context = context;
        dialog = onCreate(alertPrompt, context, result);
    }

    public androidx.appcompat.app.AlertDialog onCreate(GeckoSession.PromptDelegate.RepostConfirmPrompt alertPrompt, Context context,
                                                       GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        return new MaterialAlertDialogBuilder(context)
                .setTitle(alertPrompt.title)
                .setPositiveButton("确认", (dialogInterface, i) -> {
                    result.complete(alertPrompt.dismiss());
                    dialogInterface.dismiss();
                })
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
}
