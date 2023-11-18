package chuangyuan.ycj.videolibrary.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import chuangyuan.ycj.videolibrary.R;
import chuangyuan.ycj.videolibrary.video.VideoPlayerManager;

/**
 * author  yangc
 * date 2018/3/23
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated:  手势控制view
 */

class GestureControlView extends FrameLayout {
    /***调整进度布局,控制音频，亮度布局***/
    private View dialogProLayout, exoAudioLayout, exoBrightnessLayout, exoTempFastLayout, noticeLayout;
    /***水印,封面图占位,显示音频和亮度布图*/
    private ImageView videoAudioImg, videoBrightnessImg;
    /***显示音频和亮度*/
    private ProgressBar videoAudioPro, videoBrightnessPro;
    /***视视频标题,清晰度切换,实时视频,加载速度显示,控制进度*/
    private TextView videoDialogProText;
    private TextView tempFastTextView, noticeView;
    private long noticeClickTime;

    public GestureControlView(@NonNull Context context) {
        this(context, null);
    }

    public GestureControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.TRANSPARENT);
        int videoProgressId = R.layout.simple_exo_video_progress_dialog;
        int audioId = R.layout.simple_video_audio_brightness_dialog;
        int brightnessId = R.layout.simple_video_audio_brightness_dialog;
        int exoTempFastId = R.layout.simple_exo_video_temp_fast_dialog;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.GestureControlView, 0, 0);
            try {
                audioId = a.getResourceId(R.styleable.GestureControlView_player_gesture_audio_layout_id, audioId);
                videoProgressId = a.getResourceId(R.styleable.GestureControlView_player_gesture_progress_layout_id, videoProgressId);
                brightnessId = a.getResourceId(R.styleable.GestureControlView_player_gesture_bright_layout_id, brightnessId);

            } finally {
                a.recycle();
            }
        }
        intiGestureView(audioId, brightnessId, videoProgressId, exoTempFastId);
    }

    /***
     * 初始化手势布局view
     * @param audioId 音频布局id
     * @param brightnessId 亮度布局id
     * @param videoProgressId 进度布局id
     */
    protected void intiGestureView(int audioId, int brightnessId, int videoProgressId, int exoTempFastId) {
        exoAudioLayout = inflate(getContext(), audioId, null);
        exoBrightnessLayout = inflate(getContext(), brightnessId, null);
        exoTempFastLayout = inflate(getContext(), exoTempFastId, null);
        dialogProLayout = inflate(getContext(), videoProgressId, null);
        noticeLayout = inflate(getContext(), R.layout.simple_exo_video_notice, null);
        dialogProLayout.setVisibility(GONE);
        exoAudioLayout.setVisibility(GONE);
        exoBrightnessLayout.setVisibility(GONE);
        exoTempFastLayout.setVisibility(GONE);
        addView(dialogProLayout, getChildCount());
        addView(exoAudioLayout, getChildCount());
        addView(exoBrightnessLayout, getChildCount());
        addView(exoTempFastLayout, getChildCount());
        addView(noticeLayout, getChildCount());
        if (audioId == R.layout.simple_video_audio_brightness_dialog) {
            videoAudioImg = exoAudioLayout.findViewById(R.id.exo_video_audio_brightness_img);
            videoAudioPro = exoAudioLayout.findViewById(R.id.exo_video_audio_brightness_pro);
        }
        if (brightnessId == R.layout.simple_video_audio_brightness_dialog) {
            videoBrightnessImg = exoBrightnessLayout.findViewById(R.id.exo_video_audio_brightness_img);
            videoBrightnessPro = exoBrightnessLayout.findViewById(R.id.exo_video_audio_brightness_pro);
        }
        if (videoProgressId == R.layout.simple_exo_video_progress_dialog) {
            videoDialogProText = dialogProLayout.findViewById(R.id.exo_video_dialog_pro_text);
        }
        tempFastTextView = exoTempFastLayout.findViewById(R.id.exo_video_dialog_pro_temp_fast_text);
        noticeView = noticeLayout.findViewById(R.id.exo_notice);
    }

    /***
     * 显示隐藏手势布局
     *
     * @param visibility 状态
     */
    protected void showGesture(int visibility) {
        if (exoAudioLayout != null) {
            exoAudioLayout.setVisibility(visibility);
        }
        if (exoBrightnessLayout != null) {
            exoBrightnessLayout.setVisibility(visibility);
        }
        if (exoTempFastLayout != null) {
            exoTempFastLayout.setVisibility(visibility);
        }
        if (dialogProLayout != null) {
            dialogProLayout.setVisibility(visibility);
        }
    }

    /**
     *
     **/
    public void setTimePosition(@NonNull SpannableString seekTime) {
        if (dialogProLayout != null) {
            dialogProLayout.setVisibility(View.VISIBLE);
            videoDialogProText.setText(seekTime);
        }
    }

    /**
     *
     **/
    public void setVolumePosition(int mMaxVolume, int currIndex) {
        if (exoAudioLayout != null) {
            if (exoAudioLayout.getVisibility() != VISIBLE) {
                videoAudioPro.setMax(mMaxVolume);
            }
            exoAudioLayout.setVisibility(View.VISIBLE);
            videoAudioPro.setProgress(currIndex);
            videoAudioImg.setImageResource(currIndex == 0 ? com.google.android.exoplayer2.ui.R.drawable.ic_volume_off_white_48px : com.google.android.exoplayer2.ui.R.drawable.ic_volume_up_white_48px);
        }
    }

    /**
     *
     **/
    public void setBrightnessPosition(int mMaxVolume, int currIndex) {
        if (exoBrightnessLayout != null) {
            if (exoBrightnessLayout.getVisibility() != VISIBLE) {
                videoBrightnessPro.setMax(mMaxVolume);
                videoBrightnessImg.setImageResource(com.google.android.exoplayer2.ui.R.drawable.ic_brightness_6_white_48px);
            }
            exoBrightnessLayout.setVisibility(View.VISIBLE);
            videoBrightnessPro.setProgress(currIndex);
        }
    }

    public void showTempFast(float speedNow) {
        if (exoTempFastLayout != null) {
            exoTempFastLayout.setVisibility(View.VISIBLE);
            float speed = VideoPlayerManager.FAST_PLAY_TIMES > 0 ? VideoPlayerManager.FAST_PLAY_TIMES * speedNow : -VideoPlayerManager.FAST_PLAY_TIMES;
            tempFastTextView.setText(String.format("%.1fX", speed));
        }
    }

    public void showNotice(String text) {
        if (noticeView == null) {
            return;
        }
        noticeClickTime = System.currentTimeMillis();
        noticeView.setText(text);
        noticeView.setVisibility(VISIBLE);
        noticeView.postDelayed(new NoticeDismissTask(noticeClickTime), 2000);
    }

    public String getNotice() {
        if (noticeView.getVisibility() != VISIBLE) {
            return "";
        } else {
            return noticeView.getText().toString();
        }
    }

    private class NoticeDismissTask implements Runnable {

        private long createTime;

        public NoticeDismissTask(long createTime) {
            this.createTime = createTime;
        }

        @Override
        public void run() {
            if (noticeClickTime == createTime) {
                noticeView.setVisibility(GONE);
                noticeView.setText("");
            }
        }
    }

    public View getDialogProLayout() {
        return dialogProLayout;
    }

    public View getExoAudioLayout() {
        return exoAudioLayout;
    }

    public View getExoBrightnessLayout() {
        return exoBrightnessLayout;
    }
}
