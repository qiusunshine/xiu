package org.mozilla.xiu.browser.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.xiu.browser.R;
import org.mozilla.xiu.browser.base.BaseActivity;
import org.mozilla.xiu.browser.utils.PreferenceMgr;
import org.mozilla.xiu.browser.utils.StatusUtils;

/**
 * 作者：By hdy
 * 日期：On 2018/6/17
 * 时间：At 11:19
 */

public class TextSizeActivity extends BaseActivity {

    private SeekBar progressBar_start;
    private TextView textView, zoomText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        drawStatusBar = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int initLayout(Bundle savedInstanceState) {
        return R.layout.activity_text_size;
    }

    @Override
    protected void initView() {
        try {
            setSupportActionBar(findView(R.id.home_toolbar));
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        textView = findView(R.id.text);
        StatusUtils.INSTANCE.init(this, findView(R.id.root));
        progressBar_start = findViewById(R.id.start_progress_bar);
        zoomText = findView(R.id.zoom);
        //初始化
        progressBar_start.setProgress(getProgress());
        int zoo = getZoom(progressBar_start.getProgress());
        zoomText.setText(zoo == 100 ? "默认" : zoo + "%");
        textView.setTextSize((float) zoo / 100 * 16);

        ImageView start_img_start = findViewById(R.id.start_img_start);
        ImageView start_img_end = findViewById(R.id.start_img_end);
        start_img_start.setOnClickListener(v -> {
            int start = progressBar_start.getProgress();
            if (start > 0 && start <= 100) {
                progressBar_start.setProgress(Math.max(start - 10, 0));
            }
        });
        start_img_end.setOnClickListener(v -> {
            int start = progressBar_start.getProgress();
            if (start >= 0 && start < 100) {
                progressBar_start.setProgress(Math.min(start + 10, 100));
            }
        });
        progressBar_start.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int gap = progress - 50;
                int zoom;
                if (gap > 0) {
                    zoom = 100 + gap;
                } else {
                    zoom = 100 + gap / 2;
                }
                zoomText.setText(zoom == 100 ? "默认" : zoom + "%");
                textView.setTextSize((float) zoom / 100 * 16);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    @Override
    public void finish() {
        PreferenceMgr.put(getContext(), "textZoom", progressBar_start.getProgress());
        if (progressBar_start.getProgress() == 50) {
            boolean auto = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("switch_automatic_fontsize", false);
            GeckoRuntime.getDefault(this).getSettings().setAutomaticFontSizeAdjustment(false);
            GeckoRuntime.getDefault(this).getSettings().setFontSizeFactor(1f);
            if (auto) {
                GeckoRuntime.getDefault(this).getSettings().setAutomaticFontSizeAdjustment(auto);
            }
        } else {
            GeckoRuntime.getDefault(this).getSettings().setAutomaticFontSizeAdjustment(false);
            GeckoRuntime.getDefault(this).getSettings().setFontSizeFactor(getTextZoom(this) / 100f);
        }
        super.finish();
    }

    private static int getZoom(int progress) {
        int gap = progress - 50;
        int zoom;
        if (gap > 0) {
            zoom = 100 + gap;
        } else {
            zoom = 100 + gap / 2;
        }
        return zoom;
    }

    public static int getTextZoom(Context context) {
        int progress = PreferenceMgr.getInt(context, "textZoom", 50);
        return getZoom(progress);
    }

    public int getProgress() {
        return PreferenceMgr.getInt(getContext(), "textZoom", 50);
    }
}
