package org.telegram.ui.Components.Profile;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;

public class ProfileHeaderActionButton extends FrameLayout {
    private LayerCrossFadeDrawable backgroundDrawable;


    public ProfileHeaderActionButton(Context context) {
        super(context);
    }

    public void setExpandProgress(float progress) {
        backgroundDrawable.setCrossfadeProgress(progress);
    }

    public void initialize(Context context, ProfileActivity.ActionType action, View.OnClickListener onClickListener) {
        if(context == null) return;

        setOnClickListener(onClickListener);

        int[] collapsedBackgroundColors = calculateBackgroundColors(0);
        int[] expandedBackgroundColors = calculateBackgroundColors(1);
        backgroundDrawable = createBackgroundDrawable(
                collapsedBackgroundColors[0],
                collapsedBackgroundColors[1],
                expandedBackgroundColors[0],
                expandedBackgroundColors[1]
        );

        View background = new View(getContext());
        background.setDuplicateParentStateEnabled(true);
        background.setBackground(backgroundDrawable);

        LinearLayout contentContainer = new LinearLayout(getContext());
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setGravity(Gravity.CENTER);

        ImageView icon = new ImageView(context);
        icon.setImageResource(action.iconRes);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY));

        TextView label = new TextView(context);
        label.setText(getString(action.key, action.titleRes));
        label.setTextColor(Color.WHITE);
        label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        label.setLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setGravity(Gravity.CENTER);

        contentContainer.addView(icon, LayoutHelper.createLinear(ICON_SIZE, ICON_SIZE, Gravity.CENTER_HORIZONTAL, 4, 0, 4, 4));
        contentContainer.addView(label, LayoutHelper.createLinear(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        addView(background, LayoutHelper.createFrame(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER, 0, 0, 0, 0));
        addView(contentContainer, LayoutHelper.createFrame(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private int[] calculateBackgroundColors(int mediaProgress) {
        int verifiedColor;
        if (mediaProgress == 1 || !Theme.isCurrentThemeDark()) {
            verifiedColor = Color.BLACK;
        } else  {
            verifiedColor = 0xbfbfbf;
        }

        int pressedColor = ColorUtils.setAlphaComponent(verifiedColor, 40);
        int backgroundColor = ColorUtils.setAlphaComponent(verifiedColor, 30);

        return new int[] { backgroundColor, pressedColor };
    }

    private LayerCrossFadeDrawable createBackgroundDrawable(
            int collapsedBackgroundColor,
            int collapsedRippleColor,
            int expandedBackgroundColor,
            int expandedRippleColor
    ) {
        float roundedCorners = AndroidUtilities.dp(ROUNDED_CORNERS);
        Drawable collapsedDrawable = new RoundedColorDrawable(collapsedBackgroundColor, roundedCorners);
        Drawable collapsedRippleDrawable = createRippleDrawableOrStub(collapsedRippleColor, collapsedDrawable);

        Drawable expandedDrawable = new RoundedColorDrawable(expandedBackgroundColor, roundedCorners);
        Drawable expandedRippleDrawable = createRippleDrawableOrStub(expandedRippleColor, expandedDrawable);

        return LayerCrossFadeDrawable.create(collapsedRippleDrawable, expandedRippleDrawable);
    }

    private Drawable createRippleDrawableOrStub(int rippleColor, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorStateList rippleColorList = ColorStateList.valueOf(rippleColor);
            return new RippleDrawable(rippleColorList, drawable, null);
        } else {
            Drawable pressedDrawable = new RoundedColorDrawable(rippleColor, AndroidUtilities.dp(ROUNDED_CORNERS));
            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, pressedDrawable);
            stateListDrawable.addState(StateSet.WILD_CARD, drawable);
            return stateListDrawable;
        }
    }


    private static class LayerCrossFadeDrawable extends LayerDrawable {
        private float crossfadeProgress = 0f;


        public LayerCrossFadeDrawable(Drawable[] layers) {
            super(layers);
            updateAlphas();
        }

        public static LayerCrossFadeDrawable create(Drawable d0, Drawable d1) {
            Drawable[] layers = new Drawable[]{ d0,  d1 };
            return new LayerCrossFadeDrawable(layers);
        }

        public void setCrossfadeProgress(@FloatRange(from = 0.0, to = 1.0) float progress) {
            this.crossfadeProgress = Math.max(0f, Math.min(1f, progress));
            updateAlphas();
        }

        public float getCrossfadeProgress() {
            return crossfadeProgress;
        }

        private void updateAlphas() {
            int alpha1 = (int) ((1.0f - crossfadeProgress) * 255);
            int alpha2 = (int) (crossfadeProgress * 255);

            Drawable d0 = getDrawable(0);
            Drawable d1 = getDrawable(1);

            d0.setAlpha(alpha1);
            d1.setAlpha(alpha2);

            d0.setVisible(alpha1 > 0, false);
            d1.setVisible(alpha2 > 0, false);

            invalidateSelf();
        }
    }

    public static class RoundedColorDrawable extends Drawable {

        private final Paint paint;
        private final int baseColor;
        private float radius;
        private int currentAlpha = 255;

        private final RectF rectF = new RectF();

        public RoundedColorDrawable(int color, float radius) {
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.baseColor = color;
            this.radius = radius;
            this.paint.setColor(this.baseColor);
        }

        public void setRadius(float radius) {
            this.radius = radius;
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            rectF.set(getBounds());
            canvas.drawRoundRect(rectF, radius, radius, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            if (this.currentAlpha != alpha) {
                this.currentAlpha = alpha;
                int baseAlpha = baseColor >>> 24;
                int finalAlpha = (baseAlpha * (alpha + (alpha >> 7))) >> 8;
                int rgb = baseColor & 0x00FFFFFF;
                int finalColor = rgb | (finalAlpha << 24);
                paint.setColor(finalColor);
                invalidateSelf();
            }
        }

        @Override
        public int getAlpha() {
            return this.currentAlpha;
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private static final int ICON_SIZE = 24;
    private static final int ROUNDED_CORNERS = 12;
}
