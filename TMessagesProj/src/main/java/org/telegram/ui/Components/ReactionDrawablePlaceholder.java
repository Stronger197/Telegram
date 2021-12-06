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
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;


public class ReactionDrawablePlaceholder extends Drawable {

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


    public ReactionDrawablePlaceholder(int viewCount, String emoji, int textSize, int type, float x, float y, int accountInstance, MessageObject currentMessageObject, Paint backgroundPaint, int textColor) {
        this(viewCount, emoji, textSize, type, x, y, accountInstance, currentMessageObject, backgroundPaint, textColor, null, null);
    }

    public ReactionDrawablePlaceholder(int viewCount, String emoji, int textSize, int type, float x, float y, int accountInstance, MessageObject currentMessageObject, Paint backgroundPaint, int textColor, Paint overlayColor, Paint strokePaint) {
        super();

        this.object = currentMessageObject;
        this.paint = backgroundPaint;
        this.overlayColor = overlayColor;
        this.strokePaint = strokePaint;

        ArrayList<TLRPC.TL_availableReaction> reactionsList;



        if(MessagesController.getInstance(accountInstance).availableReactions != null) {
            reactionsList = MessagesController.getInstance(accountInstance).availableReactions;
        } else {
            reactionsList = new ArrayList<>();
        }

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


        android.graphics.Rect bounds = new android.graphics.Rect();
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
        android.graphics.Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);

        RectF rectf = new RectF();
        rectf.set(x, y, x + bounds.width() + AndroidUtilities.dp(45), y + AndroidUtilities.dp(29));
        if(overlayColor != null) {
            canvas.drawRoundRect(rectf, AndroidUtilities.dp(20f), AndroidUtilities.dp(20f), overlayColor);
        }

        Paint newPaint = new Paint();
        newPaint.setStyle(Paint.Style.FILL);
        newPaint.setColor(Color.BLUE);

        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), newPaint);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }
}