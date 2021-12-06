package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;


public class ReactionFilterDrawable extends Drawable {

    private RectF rect = new RectF();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private int textWidth;
    private ImageReceiver pollAvatarImages;
    private Drawable fallback;

    private int textHeight;
    private String text;
    private int currentType;
    private int viewCount;
    private float x;
    private float y;
    TLRPC.Document emojiImage = null;
    MessageObject object;

    public float progress;

    public ReactionFilterDrawable(int viewCount, TLRPC.TL_availableReaction reaction, Drawable fallback) {
        super();

        progress = 0f;

        this.fallback = fallback;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff72b5e8);
        paint.setAlpha(64);


        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStrokeWidth(AndroidUtilities.dp(2));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(0xff72b5e8);
        strokePaint.setAlpha((int) (255 * progress));


        if(reaction != null) {
            emojiImage = reaction.static_icon;
        }

        pollAvatarImages = new ImageReceiver();
        textPaint.setColor(0xff72b5e8);

        textPaint.setTextSize(AndroidUtilities.dp(16));
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));


        this.viewCount = viewCount;

        text = String.format("%s", LocaleController.formatShortNumber(Math.max(1, viewCount), null));


        android.graphics.Rect bounds = new android.graphics.Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        textHeight = bounds.height();
        textWidth = bounds.width();
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public int getIntrinsicWidth() {
        return textWidth + AndroidUtilities.dp(47);
    }

    @Override
    public int getIntrinsicHeight() {
        return AndroidUtilities.dp(29);
    }

    @Override
    public void draw(Canvas canvas) {
        android.graphics.Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);


        if(emojiImage != null) {
            pollAvatarImages.setImage(ImageLocation.getForDocument(emojiImage), null, null, null, 0, null, object, 1);
        } else {
            pollAvatarImages.setImageBitmap(fallback);
        }
        Drawable emojiDrawable = pollAvatarImages.getDrawable();

        if(emojiDrawable != null) {


            RectF rectf = new RectF();
            rectf.set(x, y, x + bounds.width() + AndroidUtilities.dp(45), y + AndroidUtilities.dp(29));
            canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), paint);

            strokePaint.setAlpha((int) (255 * progress));
            canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), strokePaint);

            canvas.drawText(text, x + AndroidUtilities.dp(31), y + AndroidUtilities.dp(20), textPaint);


            emojiDrawable.setBounds((int) x + AndroidUtilities.dp(4), (int) y + AndroidUtilities.dp(3), (int) (x + AndroidUtilities.dp(27)), (int) (y + AndroidUtilities.dp(26)));
            emojiDrawable.draw(canvas);
        } else {
            RectF rectf = new RectF();
            rectf.set(x, y, x + bounds.width() + AndroidUtilities.dp(45), y + AndroidUtilities.dp(29));
            canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), paint);
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}