package org.mozilla.xiu.browser.broswer.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;

import org.mozilla.geckoview.GeckoSession;
import org.mozilla.xiu.browser.componets.MyDialog;
import org.mozilla.xiu.browser.databinding.DiaChoiceBinding;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsChoiceDialog extends MyDialog {
    int dialogResult;
    Handler mHandler ;
    DiaChoiceBinding binding;
    public JsChoiceDialog(@NonNull Context context, GeckoSession.PromptDelegate.ChoicePrompt choicePrompt) {
        super(context);
        onCreate(choicePrompt,context);
    }
    public void onCreate(GeckoSession.PromptDelegate.ChoicePrompt choicePrompt, Context context) {
        binding= DiaChoiceBinding.inflate(LayoutInflater.from(getContext()));

        setTitle(choicePrompt.title);
        setMessage(choicePrompt.message);
        setView(binding.getRoot());
        List<String> collect= null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            collect = Arrays.stream(choicePrompt.choices).map(choice -> choice.label).collect(Collectors.toList());
        }
        for (int i=0;i<collect.size();i++)
        {
            RadioButton radioButton=new RadioButton(context);
            RadioGroup.LayoutParams lp = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            radioButton.setText(collect.get(i));
            binding.ChoiceGroup.addView(radioButton,lp);
            int finalI = i;
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    endDialog(finalI);




                }
            });





        }


    }


    public void endDialog(int result)
    {
        setDialogResult(result);
        super.dismiss();
        Message m = mHandler.obtainMessage();
        mHandler.sendMessage(m);
        Log.d("endDia",result+"");





    }
    @SuppressLint("HandlerLeak")
    public int showDialog()
    {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message mesg) {
                // process incoming messages here
                //super.handleMessage(msg);
                throw new RuntimeException();
            }
        };
        super.show();
        try {
            Looper.getMainLooper().loop();
        }
        catch(RuntimeException e2)
        {
        }
        return dialogResult;
    }

    public int getDialogResult() {
        return dialogResult;
    }

    public void setDialogResult(int dialogResult) {
        this.dialogResult = dialogResult;
    }
}
