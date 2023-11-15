package org.mozilla.xiu.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.material.R;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.internal.ThemeEnforcement;
import com.google.android.material.resources.MaterialResources;

/**
 * 作者：By 15968
 * 日期：On 2023/11/14
 * 时间：At 14:13
 */

public class TintableShapeableImageView extends ShapeableImageView {

    private static final int DEF_STYLE_RES = R.style.Widget_MaterialComponents_Button;
    @Nullable
    private ColorStateList iconTint;
    public TintableShapeableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.materialButtonStyle);
    }

    @SuppressLint("RestrictedApi")
    public TintableShapeableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        TypedArray attributes =
                ThemeEnforcement.obtainStyledAttributes(
                        context, attrs, R.styleable.MaterialButton, defStyleAttr, DEF_STYLE_RES);
        iconTint = MaterialResources.getColorStateList(getContext(), attributes, R.styleable.MaterialButton_iconTint);
        attributes.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            DrawableCompat.setTintList(drawable, iconTint);
        }
        super.onDraw(canvas);
    }
}