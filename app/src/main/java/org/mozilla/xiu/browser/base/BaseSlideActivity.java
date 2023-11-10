package org.mozilla.xiu.browser.base;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import org.mozilla.xiu.browser.R;
import org.mozilla.xiu.browser.utils.AndroidBarUtils;
import org.mozilla.xiu.browser.utils.DisplayUtil;

/**
 * 作者：By 15968
 * 日期：On 2021/1/23
 * 时间：At 22:07
 */

public abstract class BaseSlideActivity extends BaseActivity {

    //    protected MyShadowBgAnimator shadowBgAnimator;
    private boolean isFinished;
    private View bgView;

    protected void clearFullScreen() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    protected void showAnimation() {
        if (bgView == null) {
            return;
        }
        bgView.setAlpha(0f);
        bgView.post(() -> {
            float start = DisplayUtil.dpToPx(getContext(), 120);
            float end = 0;
            ObjectAnimator animator = ObjectAnimator.ofFloat(bgView, "translationY", start, end);
            animator.setDuration(300);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    bgView.setAlpha(1f);
                }

                @Override
                public void onAnimationEnd(Animator animation) {

                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
            ViewCompat.postInvalidateOnAnimation(bgView);
        });
    }

    protected void showCloseAnimation() {
        if (bgView == null) {
            return;
        }
        float start = 0;
        float end = DisplayUtil.dpToPx(getContext(), 120);
        ObjectAnimator animator = ObjectAnimator.ofFloat(bgView, "translationY", start, end);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(250);
        animator.start();
    }

    protected abstract View getBackgroundView();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        drawStatusBar = false;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView() {
//        MyStatusBarUtil.setColorNoTranslucent(this, getResources().getColor(R.color.half_transparent));
        clearFullScreen();
        AndroidBarUtils.setTranslucentStatusBar3(this, true);
        bgView = getBackgroundView();
        showAnimation();
//        shadowBgAnimator = new MyShadowBgAnimator(bgView);
//        shadowBgAnimator.initAnimator();
        initView2();
    }


//    @Override
//    public void startActivity(Intent intent) {
//        try {
//            if (intent.getComponent() != null) {
//                Class c = Class.forName(intent.getComponent().getClassName());
//                if (c.getSuperclass() == BaseSlideActivity.class) {
//                    ActivityOptions compat = ActivityOptions.makeSceneTransitionAnimation(this);
//                    startActivity(new Intent(getContext(), c), compat.toBundle());
//                    return;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        super.startActivity(intent);
//    }

    protected abstract void initView2();


    @Override
    public void finish() {
        if (isFinished) {
            return;
        }
        isFinished = true;
        super.finish();
        showCloseAnimation();
        overridePendingTransition(0, R.anim.alpha_exit);
    }
}