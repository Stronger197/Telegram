package org.telegram.ui.Profile.animation;

import static org.telegram.ui.Components.quickforward.QuickShareSelectorDrawable.Interpolators.LINEAR_INTERPOLATOR;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ProfileActivity;

public class ActionButtonsAnimators {

    public static final float ACTION_BUTTONS_HEIGHT = 60;
    private static final int ACTION_BUTTONS_HEIGHT_PX = AndroidUtilities.dp(ACTION_BUTTONS_HEIGHT);
    public static final float ACTION_BUTTONS_PADDING = 14f;
    private static final int ACTION_BUTTONS_PADDING_PX = AndroidUtilities.dp(ACTION_BUTTONS_PADDING);

    public static void animateActionButtonsV2(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        animateActionButtonsPosition(
                profileActivity,
                progress,
                statusBarHeight,
                actionBarHeight,
                defaultHeight,
                expandThresholdHeight,
                expandedHeight,
                expandedOverscrollHeight,
                keypointCollapsed,
                keypointDefault,
                keypointExpandThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateActionButtonsContainerAlpha(
                profileActivity,
                progress,
                statusBarHeight,
                actionBarHeight,
                defaultHeight,
                expandThresholdHeight,
                expandedHeight,
                expandedOverscrollHeight,
                keypointCollapsed,
                keypointDefault,
                keypointExpandThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateActionButtonsBackgroundScale(
                profileActivity,
                progress,
                statusBarHeight,
                actionBarHeight,
                defaultHeight,
                expandThresholdHeight,
                expandedHeight,
                expandedOverscrollHeight,
                keypointCollapsed,
                keypointDefault,
                keypointExpandThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateActionButtonsIconScale(
                profileActivity,
                progress,
                statusBarHeight,
                actionBarHeight,
                defaultHeight,
                expandThresholdHeight,
                expandedHeight,
                expandedOverscrollHeight,
                keypointCollapsed,
                keypointDefault,
                keypointExpandThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateActionButtonsTextScale(
                profileActivity,
                progress,
                statusBarHeight,
                actionBarHeight,
                defaultHeight,
                expandThresholdHeight,
                expandedHeight,
                expandedOverscrollHeight,
                keypointCollapsed,
                keypointDefault,
                keypointExpandThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
    }

    private static void animateActionButtonsPosition(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float translationX = 0;
        float translationY = profileActivity.extraHeight + profileActivity.getStatusBarWithActionBarHeight() - ACTION_BUTTONS_HEIGHT_PX - ACTION_BUTTONS_PADDING_PX;

        profileActivity.actionButtonsLayout.setTranslationX(translationX);
        profileActivity.actionButtonsLayout.setTranslationY(translationY);
    }

    private static void animateActionButtonsContainerAlpha(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float alpha;

        float localTime = AndroidUtilities.ilerpClamp(
                progress,
                0.063f,
                0.2f
        );

        alpha = AndroidUtilities.lerpClamp(0.0f, 1.0f, localTime);

        profileActivity.actionButtonsLayout.setAlpha(alpha);
        if (alpha <= 0f) {
            if (profileActivity.actionButtonsLayout.getVisibility() != View.GONE) {
                profileActivity.actionButtonsLayout.setVisibility(View.GONE);
            }
        } else if (profileActivity.actionButtonsLayout.getVisibility() != View.VISIBLE) {
            profileActivity.actionButtonsLayout.setVisibility(View.VISIBLE);
        }
    }

    private static void animateActionButtonsBackgroundScale(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float scaleY;

        if(progress <= 0.063f) {
            scaleY = 0;
        } else if(progress <= 0.2) {
            float localTime = AndroidUtilities.ilerpClamp(progress, 0.063f, 0.2f);
            scaleY = AndroidUtilities.lerpClamp(0.2f, 1.0f, localTime);
        } else {
            scaleY = 1f;
        }

        for(int i = 0; i < profileActivity.actionButtonsLayout.getChildCount(); i++) {
            
            ViewGroup frameLayout = (ViewGroup) profileActivity.actionButtonsLayout.getChildAt(i);
            
            View background = frameLayout.getChildAt(0);
            
            background.setPivotY(background.getHeight());
            background.setScaleY(scaleY);
        }
    }

    private static void animateActionButtonsIconScale(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float scaleXY;

        if(progress <= 0.1f) {
            scaleXY = 0;
        } else if(progress <= 0.2) {
            float localTime = AndroidUtilities.ilerpClamp(progress, 0.11f, 0.2f);
            scaleXY = AndroidUtilities.lerpClamp(0f, 1.0f, localTime);
        } else {
            scaleXY = 1f;
        }

        for(int i = 0; i < profileActivity.actionButtonsLayout.getChildCount(); i++) {
            
            ViewGroup frameLayout = (ViewGroup) profileActivity.actionButtonsLayout.getChildAt(i);
            View background = frameLayout.getChildAt(0);
            
            ViewGroup contentContainer = (ViewGroup) frameLayout.getChildAt(1);
            View icon = contentContainer.getChildAt(0);
            final RectF backgroundBounds = new RectF(0, 0, background.getWidth(), background.getHeight());
            background.getMatrix().mapRect(backgroundBounds);

            icon.setPivotX(icon.getWidth() / 2f);
            icon.setPivotY(icon.getHeight() / 2f);
            icon.setTranslationY(backgroundBounds.top / 2f);

            
            icon.setScaleX(scaleXY);
            icon.setScaleY(scaleXY);
        }
    }

    private static void animateActionButtonsTextScale(
            ProfileActivity profileActivity,
            float progress,
            float statusBarHeight,
            float actionBarHeight,
            float defaultHeight,
            float expandThresholdHeight,
            float expandedHeight,
            float expandedOverscrollHeight,
            float keypointCollapsed,
            float keypointDefault,
            float keypointExpandThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float scaleXY;
        float alpha;

        if(progress <= 0.05f) {
            scaleXY = 0.5f;
            alpha = 0f;
        } else if(progress <= 0.1f) {
            scaleXY = 0.5f;

            float localTime = AndroidUtilities.ilerpClamp(progress, 0.072f, 0.1f);
            alpha = AndroidUtilities.lerpClamp(0f, 1f, localTime);
        } else if(progress <= 0.2) {
            float localTime = AndroidUtilities.ilerpClamp(progress, 0.1f, 0.2f);
            scaleXY = AndroidUtilities.lerpClamp(0.5f, 1f, localTime);
            alpha = 1f;
        } else {
            scaleXY = 1f;
            alpha = 1f;
        }

        for(int i = 0; i < profileActivity.actionButtonsLayout.getChildCount(); i++) {
            
            ViewGroup frameLayout = (ViewGroup) profileActivity.actionButtonsLayout.getChildAt(i);
            
            ViewGroup contentContainer = (ViewGroup) frameLayout.getChildAt(1);
            View text = contentContainer.getChildAt(1);

            text.setPivotX(text.getWidth() / 2f);
            text.setPivotY(text.getHeight());

            
            text.setScaleX(scaleXY);
            text.setScaleY(scaleXY);
            text.setAlpha(alpha);
        }
    }
}
