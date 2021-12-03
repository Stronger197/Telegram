package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.Log;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;


public class ReactionDrawable extends Drawable {

    private RectF rect = new RectF();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint overlayColor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private int textWidth;
    private ImageReceiver pollAvatarImages;

    private int textHeight;
    private String text;
    private int currentType;
    private int viewCount;
    private float x;
    private float y;
    TLRPC.Document emojiImage = null;
    MessageObject object;


    public ReactionDrawable(int viewCount, String emoji, int textSize, int type, float x, float y, int accountInstance, MessageObject currentMessageObject, Paint backgroundPaint, int textColor) {
        this(viewCount, emoji, textSize, type, x, y, accountInstance, currentMessageObject, backgroundPaint, textColor, null, null);
    }

    public ReactionDrawable(int viewCount, String emoji, int textSize, int type, float x, float y, int accountInstance, MessageObject currentMessageObject, Paint backgroundPaint, int textColor, Paint overlayColor, Paint strokePaint) {
        super();

        this.object = currentMessageObject;
        this.paint = backgroundPaint;
        this.overlayColor = overlayColor;
        this.strokePaint = strokePaint;

        ArrayList<TLRPC.TL_availableReaction> reactionsList = MessagesController.getInstance(accountInstance).availableReactions;
        emojiImage = null;
        for(TLRPC.TL_availableReaction reaction : reactionsList) {
            if(reaction.reaction.equals(emoji)) {
                emojiImage = reaction.static_icon;
            }
        }
        pollAvatarImages = new ImageReceiver();
        currentType = type;
        textPaint.setColor(textColor);
        textPaint.setTextSize(AndroidUtilities.dp(textSize));
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));


        this.viewCount = viewCount;

        text = String.format("%s", LocaleController.formatShortNumber(Math.max(1, viewCount), null));


        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        textHeight = bounds.height();
        textWidth = bounds.width();

        this.x = x;
        this.y = y;
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
        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);


        Log.e("DEBUG_ABCD", "debugImage: " + emojiImage);
        Log.e("DEBUG_ABCD", "location: " + ImageLocation.getForDocument(emojiImage));

        pollAvatarImages.setImage(ImageLocation.getForDocument(emojiImage), "50_50", null, 0, null, object, 1);
        Log.e("DEBUG_ABCD", "drawable: " + pollAvatarImages.getDrawable());


        Drawable emojiDrawable = pollAvatarImages.getDrawable();






        if(emojiDrawable != null) {


            RectF rectf = new RectF();
            rectf.set(x, y, x + bounds.width() + AndroidUtilities.dp(45), y + AndroidUtilities.dp(29));
            canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), paint);
            if(overlayColor != null) {
                canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), overlayColor);
            }
            if(strokePaint != null) {
                canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), strokePaint);
            }

            canvas.drawText(text, x + AndroidUtilities.dp(31), y + AndroidUtilities.dp(20), textPaint);


            emojiDrawable.setBounds((int) x + AndroidUtilities.dp(4), (int) y + AndroidUtilities.dp(3), (int) (x + AndroidUtilities.dp(27)), (int) (y + AndroidUtilities.dp(26)));
            emojiDrawable.draw(canvas);
        }
//        pollAvatarImages.draw(canvas);

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}