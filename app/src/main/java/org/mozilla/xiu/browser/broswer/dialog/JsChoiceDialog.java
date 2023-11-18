package org.mozilla.xiu.browser.broswer.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.xiu.browser.componets.MyDialog;
import org.mozilla.xiu.browser.databinding.DiaChoiceBinding;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsChoiceDialog extends MyDialog {
    DiaChoiceBinding binding;

    public JsChoiceDialog(@NonNull Context context, GeckoSession.PromptDelegate.ChoicePrompt choicePrompt,
                          GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        super(context);
        onCreate(choicePrompt, context, result);
    }

    public void onCreate(GeckoSession.PromptDelegate.ChoicePrompt choicePrompt, Context context,
                         GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result) {
        binding = DiaChoiceBinding.inflate(LayoutInflater.from(getContext()));
        setTitle(choicePrompt.title);
        setMessage(choicePrompt.message);
        setView(binding.getRoot());
        List<String> collect = null;
        collect = Arrays.stream(choicePrompt.choices).map(choice -> choice.label).collect(Collectors.toList());
        for (int i = 0; i < collect.size(); i++) {
            RadioButton radioButton = new RadioButton(context);
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            radioButton.setText(collect.get(i));
            binding.ChoiceGroup.addView(radioButton, lp);
            int finalI = i;
            radioButton.setOnClickListener(view -> endDialog(result, choicePrompt.confirm(String.valueOf(finalI))));
        }
        setOnDismissListener(dialogInterface -> {
            if (!choicePrompt.isComplete()) {
                result.complete(choicePrompt.confirm(String.valueOf(0)));
            }
        });
    }


    public void endDialog(GeckoResult<GeckoSession.PromptDelegate.PromptResponse> result, GeckoSession.PromptDelegate.PromptResponse result1) {
        result.complete(result1);
        super.dismiss();
    }
}
