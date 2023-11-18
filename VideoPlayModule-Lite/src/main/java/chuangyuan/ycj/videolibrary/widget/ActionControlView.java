package chuangyuan.ycj.videolibrary.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ui.PlayerView;

import chuangyuan.ycj.videolibrary.R;

/**
 * author  yangc
 * date 2018/3/23
 * E-Mail:yangchaojiang@outlook.com
 * Deprecated:  动作管理view  错误布局，完成布局，提示布局
 */

class ActionControlView extends FrameLayout {
    /***视频加载页,错误页,进度控件,锁屏按布局,自定义预览布局*/
    private View exoPlayErrorLayout, playReplayLayout, playBtnHintLayout;

    private ActionControlView(@NonNull Context context) {
        this(context, null, null);
    }

    public ActionControlView(@NonNull Context context, @Nullable AttributeSet attrs, PlayerView playerView) {
        this(context, attrs, 0, playerView);
    }

    public ActionControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, @NonNull PlayerView playerView) {
        super(context, attrs, defStyleAttr);
        int replayId = R.layout.simple_exo_play_replay;
        int errorId = R.layout.simple_exo_play_error;
        int playerHintId = R.layout.simple_exo_play_btn_hint;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VideoPlayerView, 0, 0);
            try {
                replayId = a.getResourceId(R.styleable.VideoPlayerView_player_replay_layout_id, replayId);
                errorId = a.getResourceId(R.styleable.VideoPlayerView_player_error_layout_id, errorId);
                playerHintId = a.getResourceId(R.styleable.VideoPlayerView_player_hint_layout_id, playerHintId);
            } finally {
                a.recycle();
            }
        }
        exoPlayErrorLayout = inflate(context, errorId, null);
        playReplayLayout = inflate(context, replayId, null);
        playBtnHintLayout = inflate(context, playerHintId, null);
        exoPlayErrorLayout.setVisibility(GONE);
        playReplayLayout.setVisibility(GONE);
        playBtnHintLayout.setVisibility(GONE);
        addView(exoPlayErrorLayout, getChildCount());
        addView(playReplayLayout, getChildCount());
        addView(playBtnHintLayout, getChildCount());
    }


    /***
     * 显示隐藏错误页
     *
     * @param visibility 状态
     */
    public void showErrorState(int visibility) {
        if (exoPlayErrorLayout != null) {
            exoPlayErrorLayout.setVisibility(visibility);
        }
    }

    /***
     * 显示按钮提示页
     *
     * @param visibility 状态
     */
    public void showBtnContinueHint(int visibility) {
        if (playBtnHintLayout != null) {
            playBtnHintLayout.setVisibility(visibility);
        }
    }

    /***
     * 显示按钮提示页
     *
     * @param visibility 状态
     */
    public void showBtnContinueHint(int visibility, String msg) {
        if (playBtnHintLayout != null) {
            playBtnHintLayout.setVisibility(visibility);
            TextView textView = playBtnHintLayout.findViewById(R.id.exo_player_btn_hint_tv_id);
            textView.setText(msg);
            if (msg == null || msg.isEmpty()) {
                textView.setVisibility(GONE);
            } else {
                textView.setVisibility(VISIBLE);
            }
        }
    }

    /***
     * 显示隐藏重播页
     *
     * @param visibility 状态
     */
    public void showReplay(int visibility) {
        if (playReplayLayout != null) {
            playReplayLayout.setVisibility(visibility);
        }
    }

    public void hideAllView() {
        playBtnHintLayout.setVisibility(GONE);
        exoPlayErrorLayout.setVisibility(GONE);
        playReplayLayout.setVisibility(GONE);
    }

    public View getExoPlayErrorLayout() {
        return exoPlayErrorLayout;
    }

    public View getPlayReplayLayout() {
        return playReplayLayout;
    }

    public View getPlayBtnHintLayout() {
        return playBtnHintLayout;
    }
}
