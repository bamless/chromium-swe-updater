package com.bamless.chromiumsweupdater.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bamless.chromiumsweupdater.R;

/**
 * Custom view that implements a button with an animation. The animation is played on click.
 * The animation can be set directly from the xml with custom:animation="id of animation".
 * You can still use the {@link AppCompatImageButton#startAnimation(Animation)} and other
 * {@link AppCompatImageButton} animations method with this class, but it is recommended to use
 * the new *ButtonAnimation* methods. For example to start an animation you should first set it with
 * {@link #setButtonAnimation(Animation)} and then play it with {@link #startButtonAnimation()}.
 */
public class AnimatedImageButton extends AppCompatImageButton {
    private Animation animation;
    private int defaultRepeatCount;

    public AnimatedImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        //get attributes from xml
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AnimatedImageButton,
                0, 0);

        try {
            if(!a.hasValue(R.styleable.AnimatedImageButton_animation))
                throw new RuntimeException("animation attribute missing from " + getClass().getSimpleName()
                        + ". This attribute is mandatory.");
            animation = AnimationUtils.loadAnimation(context, a.
                    getResourceId(R.styleable.AnimatedImageButton_animation, 0));
            defaultRepeatCount = animation.getRepeatCount();
        } finally {
            a.recycle();
        }
        defaultRepeatCount = animation.getRepeatCount();
    }

    public AnimatedImageButton(Context context) {
        super(context);
    }


    @Override
    public boolean performClick() {
        final boolean result = super.performClick();
        if(result)
            startButtonAnimation();
        return result;
    }

    /**Starts the button's animation (if not already started).*/
    public void startButtonAnimation() {
            animation.setRepeatCount(defaultRepeatCount);
            startAnimation(animation);
    }

    /**Stops immediately any animation*/
    public void stopButtonAnimation() {
        clearAnimation();
    }

    /**Stops the button's animation at the end of the current cycle. It is equivalent to set
     * {@link Animation#setRepeatCount(int)} to 0.*/
    public void stopButtonAnimationSmooth() {
        animation.setRepeatCount(0);
    }

    /**Sets the button's animation that will be played on click or with {@link com.bamless.chromiumsweupdater.views.AnimatedImageButton#startButtonAnimation()}.*/
    public void setButtonAnimation(Animation animation) {
        stopButtonAnimation();
        this.animation = animation;
        invalidate();
        requestLayout();
    }

    public void setButtonAnimationListener(Animation.AnimationListener listener) {
        animation.setAnimationListener(listener);
    }

    /**Set a new repeat count for the button's animation*/
    public void setButtonAnimationRepeatcount(int repeatcount) {
        animation.setRepeatCount(repeatcount);
        defaultRepeatCount = repeatcount;
    }

    /**Returns the button's animation*/
    public Animation getButtonAnimation() {
        return animation;
    }
}
