package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;

public class GooeyDynamicIslandView extends View implements AndroidUtilities.OnRootWindowInsetsChangeListener {

    private static final float ISLAND_WIDTH_DP = 150f;
    private static final float ISLAND_HEIGHT_DP = 30;
    private static final float ISLAND_TOP_MARGIN_DP = -ISLAND_HEIGHT_DP - 5;
    private static final float AVATAR_RADIUS_DP = 19f;
    private static final float CENTER_TOLERANCE_DP = 10;
    private static final float CIRCLE_TOLERANCE_PX = 10;


    private float islandWidthPx;
    private float islandHeightPx;
    private float islandTopMarginPx;

    private boolean hasCircleSystemCutout = false;


    private Paint blurPaint;
    private Paint alphaThresholdPaint;

    private float avatarScale = 1f;
    private float islandScale = 1f;
    private float avatarInsets = 0f;

    private RectF islandRect = new RectF();
    private RectF scaledIslandRect = new RectF();
    private float avatarRadiusPx;
    private float avatarCurrentY;
    private float viewCenterX;

    private Bitmap offscreenBitmap;
    private Canvas offscreenCanvas;

    public GooeyDynamicIslandView(Context context) {
        super(context);
        init();
    }

    public GooeyDynamicIslandView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RectF getIslandRect() {
        return islandRect;
    }

    public boolean getHasCircleSystemCutout() {
        return hasCircleSystemCutout;
    }

    public void setScale(float scale) {
        avatarScale = scale;
        invalidate();
    }

    public void setInsets(float insets) {
        avatarInsets = insets;
        invalidate();
    }

    public void setIslandScale(float scale) {
        islandScale = scale;
        recalculateScaledRect();
        invalidate();
    }

    public void setAvatarY(float y) {
        avatarCurrentY = y;
        invalidate();
    }

    @Override
    public void onChanged(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            calculateIslandCoordinates(insets.getDisplayCutout());
            requestLayout();
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AndroidUtilities.addOnRootWindowInsetsChangeListener(this);
        WindowInsets currentInsets = AndroidUtilities.getRootWindowInsets();
        if (currentInsets != null) {
            onChanged(currentInsets);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AndroidUtilities.removeOnRootWindowInsetsChangeListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewCenterX = w / 2f;

        if (offscreenBitmap != null) {
            offscreenBitmap.recycle();
        }

        offscreenBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        offscreenCanvas = new Canvas(offscreenBitmap);

        float islandLeft = (w - islandWidthPx) / 2;
        islandRect.set(islandLeft, islandTopMarginPx, islandLeft + islandWidthPx, islandTopMarginPx + islandHeightPx);
        recalculateScaledRect();

        avatarRadiusPx = AndroidUtilities.dp(AVATAR_RADIUS_DP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (offscreenCanvas == null) {
            return;
        }

        offscreenCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        float islandRectXY = scaledIslandRect.height();
        offscreenCanvas.drawRoundRect(scaledIslandRect, islandRectXY, islandRectXY, blurPaint);

        offscreenCanvas.drawCircle(viewCenterX, avatarCurrentY, (avatarRadiusPx - avatarInsets) * avatarScale, blurPaint);

        canvas.drawBitmap(offscreenBitmap, 0, 0, alphaThresholdPaint);
    }

    private void init() {
        setBackgroundColor(Color.TRANSPARENT);

        islandWidthPx = AndroidUtilities.dp(ISLAND_WIDTH_DP);
        islandHeightPx = AndroidUtilities.dp(ISLAND_HEIGHT_DP);
        islandTopMarginPx = AndroidUtilities.dp(ISLAND_TOP_MARGIN_DP);

        blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blurPaint.setColor(Color.BLACK);
        float blurRadius = AndroidUtilities.dp(20);
        blurPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));

        alphaThresholdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[]{
                1, 0, 0, 0, 0,
                0, 1, 0, 0, 0,
                0, 0, 1, 0, 0,
                0, 0, 0, 30, -10 * 255,
        });
        alphaThresholdPaint.setColorFilter(new ColorMatrixColorFilter(matrix));
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void calculateIslandCoordinates(DisplayCutout cutout) {
        hasCircleSystemCutout = false;

        Rect centralCutout = findFirstCentralCutout(cutout);
        Path centralCutoutPath = null;

        if (centralCutout != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            centralCutoutPath = cutout.getCutoutPath();
        }

        if (centralCutout == null) {
            islandWidthPx = AndroidUtilities.dp(ISLAND_WIDTH_DP);
            islandHeightPx = AndroidUtilities.dp(ISLAND_HEIGHT_DP);
            islandTopMarginPx = AndroidUtilities.dp(ISLAND_TOP_MARGIN_DP);
        } else if (centralCutoutPath != null) {
            RectF bounds = new RectF();
            centralCutoutPath.computeBounds(bounds, true);

            if (Math.abs(bounds.width() - bounds.height()) < CIRCLE_TOLERANCE_PX) {
                hasCircleSystemCutout = false;
            }

            if (hasCircleSystemCutout) {
                float diameter = Math.min(centralCutout.width(), centralCutout.height());
                islandWidthPx = diameter;
                islandHeightPx = diameter;
                islandTopMarginPx = bounds.centerY() - diameter / 2;
            } else  {
                islandWidthPx = bounds.width();
                islandHeightPx = bounds.height();
                islandTopMarginPx = bounds.top;
            }
        } else  {
            float diameter = Math.min(centralCutout.width(), centralCutout.height());
            islandWidthPx = diameter;
            islandHeightPx = diameter;
            islandTopMarginPx = (float) centralCutout.bottom - diameter;
            hasCircleSystemCutout = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private Rect findFirstCentralCutout(DisplayCutout cutout) {
        if (cutout == null) {
            return null;
        }

        float screenCenterX = getContext().getResources().getDisplayMetrics().widthPixels / 2f;
        float screenCenterY = getContext().getResources().getDisplayMetrics().heightPixels / 2f;
        float centerTolerancePx = AndroidUtilities.dp(CENTER_TOLERANCE_DP);

        for (Rect rect : cutout.getBoundingRects()) {
            if (rect.centerY() < screenCenterY && Math.abs(rect.centerX() - screenCenterX) <= centerTolerancePx) {
                return rect;
            }
        }

        return null;
    }

    private void recalculateScaledRect() {
        if (islandScale == 1f) {
            scaledIslandRect.set(islandRect);
            return;
        }

        float centerX = islandRect.centerX();
        float centerY = islandRect.centerY();

        float newWidth = islandRect.width() * islandScale;
        float newHeight = islandRect.height() * islandScale;

        float halfWidth = newWidth / 2f;
        float halfHeight = newHeight / 2f;

        scaledIslandRect.set(
                centerX - halfWidth,
                centerY - halfHeight,
                centerX + halfWidth,
                centerY + halfHeight
        );
    }
}