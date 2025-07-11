package org.telegram.ui.Components.Profile;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.FrameLayout;

import org.telegram.ui.ActionBar.Theme;

public class BanViewFrameLayout extends FrameLayout {
    public BanViewFrameLayout(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int bottom = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
        Theme.chat_composeShadowDrawable.setBounds(0, 0, getMeasuredWidth(), bottom);
        Theme.chat_composeShadowDrawable.draw(canvas);
        canvas.drawRect(0, bottom, getMeasuredWidth(), getMeasuredHeight(), Theme.chat_composeBackgroundPaint);
    }
}
