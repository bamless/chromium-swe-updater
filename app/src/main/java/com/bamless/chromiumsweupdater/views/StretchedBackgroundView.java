package com.bamless.chromiumsweupdater.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class StretchedBackgroundView extends android.support.v7.widget.AppCompatImageView {

    public StretchedBackgroundView(Context context) {
        super(context);
    }

    public StretchedBackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StretchedBackgroundView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable d = getDrawable();
        if (d != null) {
            int w = MeasureSpec.getSize(widthMeasureSpec) * 3 / 2;
            int h = w * d.getIntrinsicHeight() / d.getIntrinsicWidth();
            setMeasuredDimension(w, h);
        }
        else super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
