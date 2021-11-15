package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.Theme;

public class SendMessageAsView extends BackupImageView {

    private float progress;
    private boolean deleting;
    private Drawable deleteDrawable;
    private RectF rect = new RectF();
    private AvatarDrawable avatarDrawable;
    private int[] colors = new int[8];
    private long lastUpdateTime = 0;
    private static Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);



    public SendMessageAsView(final Context context) {
        super(context);
        deleteDrawable = getResources().getDrawable(R.drawable.delete);
    }

    @Override
    public void setForUserOrChat(TLObject object, AvatarDrawable avatarDrawable) {
        super.setForUserOrChat(object, avatarDrawable);
        this.avatarDrawable = avatarDrawable;

        updateColors();
    }

    @Override
    public void setForUserOrChat(TLObject object, AvatarDrawable avatarDrawable, Object parent) {
        super.setForUserOrChat(object, avatarDrawable, parent);
        this.avatarDrawable = avatarDrawable;

        updateColors();
    }

    public void updateColors() {
        int color = avatarDrawable.getColor();
        int back = Theme.getColor(Theme.key_groupcreate_spanBackground);
        int delete = Theme.getColor(Theme.key_groupcreate_spanDelete);
        colors[0] = Color.red(back);
        colors[1] = Color.red(color);
        colors[2] = Color.green(back);
        colors[3] = Color.green(color);
        colors[4] = Color.blue(back);
        colors[5] = Color.blue(color);
        colors[6] = Color.alpha(back);
        colors[7] = Color.alpha(color);
        deleteDrawable.setColorFilter(new PorterDuffColorFilter(delete, PorterDuff.Mode.MULTIPLY));
        backPaint.setColor(back);
    }

    public void setAccount() {

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (deleting && progress != 1.0f || !deleting && progress != 0.0f) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - lastUpdateTime;
            if (dt < 0 || dt > 17) {
                dt = 17;
            }
            if (deleting) {
                progress += dt / 120.0f;
                if (progress >= 1.0f) {
                    progress = 1.0f;
                }
            } else {
                progress -= dt / 120.0f;
                if (progress < 0.0f) {
                    progress = 0.0f;
                }
            }
            invalidate();
        }
        canvas.save();
//        rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
//        backPaint.setColor(Color.argb(colors[6] + (int) ((colors[7] - colors[6]) * progress), colors[0] + (int) ((colors[1] - colors[0]) * progress), colors[2] + (int) ((colors[3] - colors[2]) * progress), colors[4] + (int) ((colors[5] - colors[4]) * progress)));
//        canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), backPaint);
        imageReceiver.draw(canvas);
        if (progress != 0) {
            int color = avatarDrawable.getColor();
            float alpha = Color.alpha(color) / 255.0f;
            backPaint.setColor(color);
            backPaint.setAlpha((int) (255 * progress * alpha));
            canvas.drawCircle(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(16), backPaint);
            canvas.save();
            canvas.rotate(45 * (1.0f - progress), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            deleteDrawable.setBounds(AndroidUtilities.dp(18), AndroidUtilities.dp(18), AndroidUtilities.dp(30), AndroidUtilities.dp(30));
            deleteDrawable.setAlpha((int) (255 * progress));
            deleteDrawable.draw(canvas);
            canvas.restore();
        }

        canvas.restore();
    }

    public void startDeleteAnimation() {
        if (deleting) {
            return;
        }
        deleting = true;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void cancelDeleteAnimation() {
        if (!deleting) {
            return;
        }
        deleting = false;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }
}
