package org.telegram.ui.Components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.Interpolator;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.recyclerview.widget.RecyclerView;

public class UninterruptibleRecyclerView extends RecyclerView {

    public boolean mIsUninterruptibleScrollInProgress = false;
    public boolean mGestureShadowedDuringAnim = false;

    public void stopUninterruptibleScroll() {
        mIsUninterruptibleScrollInProgress = false;
        stopScroll();
    }

    public void smoothUninterruptibleScrollBy(@Px int dx, @Px int dy, @Nullable Interpolator interpolator, Boolean stopUninterruptibleScroll) {
        if (stopUninterruptibleScroll) {
            stopUninterruptibleScroll();
        } else if (mIsUninterruptibleScrollInProgress) {
            return;
        }

        mIsUninterruptibleScrollInProgress = true;
        smoothScrollBy(dx, dy, interpolator);
    }

    public void smoothUninterruptibleScrollBy(@Px int dx, @Px int dy, int duration, @Nullable Interpolator interpolator, Boolean stopUninterruptibleScroll) {
        if (stopUninterruptibleScroll) {
            stopUninterruptibleScroll();
        } else if (mIsUninterruptibleScrollInProgress) {
            return;
        }

        mIsUninterruptibleScrollInProgress = true;
        smoothScrollBy(dx, dy, duration, interpolator);
    }

    public UninterruptibleRecyclerView(@NonNull Context context) {
        super(context);
    }

    public UninterruptibleRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UninterruptibleRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void stopScroll() {
        if (!mIsUninterruptibleScrollInProgress) {
            super.stopScroll();
        }
    }

    @Override
    @CallSuper
    public void onScrollStateChanged(int state) {
        if (mIsUninterruptibleScrollInProgress && state == RecyclerView.SCROLL_STATE_IDLE) {
            mIsUninterruptibleScrollInProgress = false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mIsUninterruptibleScrollInProgress) {
            mGestureShadowedDuringAnim = true;
            return false;
        } else {
            boolean result = super.dispatchTouchEvent(ev);
            mGestureShadowedDuringAnim = false;
            return result;
        }
    }
}
