package org.mozilla.xiu.browser.broswer.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;

public class TextDialog extends androidx.appcompat.app.AlertDialog {
    Context context;

    public TextDialog(@NonNull Context context, GeckoSession.PromptDelegate.TextPrompt alertPrompt,
                      GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        super(context);
        this.context = context;
        onCreate(alertPrompt, context, result);
    }

    public void onCreate(GeckoSession.PromptDelegate.TextPrompt alertPrompt, Context context,
                         GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        setTitle(alertPrompt.title);
        setMessage(alertPrompt.message);
        setButton(BUTTON_POSITIVE, "确认", (dialogInterface, i) -> {
            result.complete(alertPrompt.dismiss());
            dismiss();
        });
        setOnDismissListener(dialogInterface -> {
            if (!alertPrompt.isComplete()) {
                result.complete(alertPrompt.dismiss());
            }
        });

    }
}
