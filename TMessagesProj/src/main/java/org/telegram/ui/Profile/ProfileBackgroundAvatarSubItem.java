package org.telegram.ui.Profile;

import static org.telegram.messenger.AndroidUtilities.lerp;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class ProfileBackgroundAvatarSubItem {
    public float x;
    public float y;
    public float alpha;
    public float scale;
    public final float centerOffsetX;
    public final float centerOffsetY;
    private final float animateEndInterval;
    private final float animateStartInterval;

    private final float defaultAlpha;
    private final float defaultScale;
    private final boolean enableFadeOut;

    public static final CubicBezierInterpolator EASE_IN_OUT =
            new CubicBezierInterpolator(0.42f, 0f, 0.58f, 1f);

    public ProfileBackgroundAvatarSubItem(
            float centerOffsetXDp,
            float centerOffsetYDp,
            float animateStartInterval,
            float animateEndInterval
    ) {
        defaultAlpha = 1f;
        defaultScale = 1f;
        enableFadeOut = false;
        centerOffsetX = AndroidUtilities.dp(centerOffsetXDp);
        centerOffsetY = AndroidUtilities.dp(centerOffsetYDp);
        this.animateEndInterval = animateEndInterval;
        this.animateStartInterval = animateStartInterval;
    }

    public ProfileBackgroundAvatarSubItem(
            float centerOffsetXDp,
            float centerOffsetYDp,
            float animateStartInterval,
            float animateEndInterval,
            float defaultAlpha,
            float defaultScale,
            boolean enableFadeOut
    ) {
        centerOffsetX = AndroidUtilities.dp(centerOffsetXDp);
        centerOffsetY = AndroidUtilities.dp(centerOffsetYDp);
        this.defaultAlpha = defaultAlpha;
        this.defaultScale = defaultScale;
        this.enableFadeOut = enableFadeOut;
        this.animateEndInterval = animateEndInterval;
        this.animateStartInterval = animateStartInterval;
    }

    public void calculateCoord(
            float centerX,
            float centerY,
            float targetX,
            float targetY,
            float progress
    ) {
        float internalProgress = calculateSubProgress(progress, animateStartInterval, animateEndInterval);
        float alphaFraction = 1f;

        if (enableFadeOut) {
            alphaFraction = (1 - internalProgress);
        }

        x = lerp(centerX + centerOffsetX, targetX, internalProgress);
        y = lerp(centerY + centerOffsetY, targetY, internalProgress);
        alpha = calculateAlpha(internalProgress) * alphaFraction;
        scale = calculateScale(internalProgress);
    }

    
    public void calculateCoordWithAnimOptions(
            float centerX,
            float centerY,
            float targetX,
            float targetY,
            float progress,
            float endIntervalFraction,
            float startIntervalOffset
    ) {
        float internalProgress = calculateSubProgress(progress, startIntervalOffset + animateStartInterval, animateEndInterval * endIntervalFraction);
        float alphaFraction = 1f;

        if (enableFadeOut) {
            alphaFraction = (1 - internalProgress);
        }

        x = lerp(centerX + centerOffsetX, targetX, internalProgress);
        y = lerp(centerY + centerOffsetY, targetY, internalProgress);
        alpha = calculateAlpha(internalProgress) * alphaFraction;
        scale = calculateScale(internalProgress);
    }

    public void calculateCoordWithSInterpolator(
            float centerX,
            float centerY,
            float targetX,
            float targetY,
            float progress
    ) {
        float internalProgress = calculateSubProgress(progress, animateStartInterval, animateEndInterval);
        float t = EASE_IN_OUT.getInterpolation(internalProgress);
        float c = t * t; 
        float alphaFraction = 1f;

        if (enableFadeOut) {
            alphaFraction = (1 - internalProgress);
        }

        x = lerp(centerX + centerOffsetX, targetX, t);
        y = lerp(centerY + centerOffsetY, targetY, c);
        alpha = calculateAlpha(internalProgress) * alphaFraction;
        scale = calculateScale(internalProgress);
    }


    private float calculateAlpha(float internalProgress) {
        if (defaultAlpha == 0) {
            return 0;
        }

        float progress = 1 - calculateSubProgress(internalProgress, 0.85f, 0.95f);

        if (defaultAlpha == 1) {
            return progress;
        } else {
            return scaleProgress(progress, 0f, 1f, 0f, defaultAlpha);
        }
    }

    private float calculateScale(float internalProgress) {
        if (defaultScale == 0) {
            return 0;
        }

        float progress = 1 - calculateSubProgress(internalProgress, 0.6f, 1f);

        if (defaultScale == 1) {
            return progress;
        } else {
            return scaleProgress(progress, 0f, 1f, 0f, defaultScale);
        }
    }

    private float scaleProgress(float value, float minOrig, float maxOrig, float minTarget, float maxTarget) {
        if (value < minOrig) {
            value = minOrig;
        } else if (value > maxOrig) {
            value = maxOrig;
        }

        return ((value - minOrig) / (maxOrig - minOrig)) * (maxTarget - minTarget) + minTarget;
    }

    private float calculateSubProgress(float mainProgress, float intervalStart, float intervalEnd) {
        if (mainProgress <= intervalStart) {
            return 0.0f;
        }

        if (mainProgress >= intervalEnd) {
            return 1.0f;
        }

        float intervalLength = intervalEnd - intervalStart;
        float progressInInterval = mainProgress - intervalStart;

        return progressInInterval / intervalLength;
    }
}
