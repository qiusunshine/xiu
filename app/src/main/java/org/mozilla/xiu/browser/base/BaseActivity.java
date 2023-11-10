package org.mozilla.xiu.browser.base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;
import org.mozilla.xiu.browser.R;
import org.mozilla.xiu.browser.utils.DisplayUtil;
import org.mozilla.xiu.browser.utils.MyStatusBarUtil;
import org.mozilla.xiu.browser.utils.PreferenceConstant;
import org.mozilla.xiu.browser.utils.PreferenceMgr;

import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "BaseActivity";
    protected Bundle extraDataBundle;
    private boolean hasInit = false;
    protected boolean drawStatusBar = true;

    @Override
    protected void onNewIntent(Intent intent) {
        Timber.d("onNewIntent===>%s", getClass().getSimpleName());
        super.onNewIntent(intent);
    }

    protected void setTranslucentNavigation() {
        boolean useNotch = PreferenceMgr.getBoolean(getContext(), PreferenceConstant.KEY_useNotch, true);
        if (!useNotch) {
            return;
        }
        //设置沉浸式虚拟键，在MIUI系统中，虚拟键背景透明。原生系统中，虚拟键背景半透明。
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        Timber.d("consume: activity(%s) onCreate start %s", getClass().getSimpleName(), (System.currentTimeMillis() - Application.start));
        Timber.d("onCreate===>%s", getClass().getSimpleName());
        checkForceDarkMode(getActivity());
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.getBoolean("recycleMe", false)) {
            finish();
            return;
        }
        setContentView(initLayout(savedInstanceState));
//        Timber.d("consume: activity(%s) onCreate after setContentView %s", getClass().getSimpleName(), (System.currentTimeMillis() - Application.start));
        extraDataBundle = getIntent().getBundleExtra("extraDataBundle");
        if (drawStatusBar) {
            MyStatusBarUtil.setColorNoTranslucent(this, getResources().getColor(R.color.white));
        }
        initView();
        //以下代码用于去除阴影
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(DisplayUtil.dpToPx(getContext(), 1) / 2);
        }
        initData(savedInstanceState);
//        Timber.d("consume: activity(%s) onCreate end %s", getClass().getSimpleName(), (System.currentTimeMillis() - Application.start));
    }

    public static void checkForceDarkMode(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                TypedValue outValue = new TypedValue();
                activity.getTheme().resolveAttribute(android.R.attr.forceDarkAllowed, outValue, true);
                if (outValue.data != 0) {
                    //开启了强制黑暗模式
                    boolean forceDark = PreferenceMgr.getBoolean(activity, "forceDark", true);
                    if (!forceDark) {
                        if (activity.getWindow() != null) {
                            activity.getWindow().getDecorView().setForceDarkAllowed(false);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        if (finishWhenRestore()) {
            outState.putBoolean("recycleMe", true);
            super.onSaveInstanceState(outState);
        }
    }

    protected boolean finishWhenRestore() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == android.R.id.home || super.onOptionsItemSelected(item);
    }

    /**
     * 初始化布局
     */
    protected abstract int initLayout(Bundle savedInstanceState);

    /**
     * 初始化布局以及View控件
     */
    protected abstract void initView();

    /**
     * 处理业务逻辑，状态恢复等操作
     *
     * @param savedInstanceState 鬼知道
     */
    protected abstract void initData(Bundle savedInstanceState);

    /**
     * 查找View
     *
     * @param id   控件的id
     * @param <VT> View类型
     * @return 鬼知道
     */
    protected <VT extends View> VT findView(@IdRes int id) {
        return (VT) findViewById(id);
    }

    protected Context getContext() {
        return this;
    }

    protected Activity getActivity() {
        return this;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
