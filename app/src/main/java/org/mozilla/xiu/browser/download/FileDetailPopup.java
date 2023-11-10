package org.mozilla.xiu.browser.download;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;

import org.mozilla.xiu.browser.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 作者：By 15968
 * 日期：On 2020/3/23
 * 时间：At 21:08
 */
public class FileDetailPopup extends BottomPopupView {

    private List<String> operations;
    private String title;
    private FileDetailAdapter.OnClickListener clickListener;
    private FileDetailAdapter adapter;

    public FileDetailPopup(@NonNull Context context) {
        super(context);
    }

    public FileDetailPopup(@NonNull Activity activity, String title, String[] operations) {
        super(activity);
        this.title = title;
        this.operations = new ArrayList<>();
        this.operations.addAll(Arrays.asList(operations));
    }

    public FileDetailPopup(@NonNull Activity activity, String title, List<String> operations) {
        super(activity);
        this.title = title;
        this.operations = operations;
    }

    // 返回自定义弹窗的布局
    @Override
    protected int getImplLayoutId() {
        return R.layout.view_setting_menu_popup;
    }

    // 执行初始化操作，比如：findView，设置点击，或者任何你弹窗内的业务逻辑
    @Override
    protected void onCreate() {
        super.onCreate();

        TextView textView = findViewById(R.id.textView);
        textView.setText(title);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new FileDetailAdapter(getContext(), operations, new FileDetailAdapter.OnClickListener() {
            @Override
            public void click(String text) {
                if(clickListener != null){
                    clickListener.click(text);
                }
            }

            @Override
            public void longClick(View view, String text) {
                if(clickListener != null){
                    clickListener.longClick(view, text);
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    public void updateData(String[] operations){
        this.operations.clear();
        this.operations.addAll(Arrays.asList(operations));
        adapter.notifyDataSetChanged();
    }

    @Override
    public BasePopupView show() {
        return super.show();
    }


    @Override
    protected int getPopupHeight() {
        return (int) (XPopupUtils.getScreenHeight(getContext()) * .75f);
    }


    public FileDetailPopup withClickListener(FileDetailAdapter.OnClickListener clickListener) {
        this.clickListener = clickListener;
        return this;
    }

    public FileDetailAdapter.OnClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(FileDetailAdapter.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public interface OnItemClickListener {
        void onClick(String text);
    }
}
