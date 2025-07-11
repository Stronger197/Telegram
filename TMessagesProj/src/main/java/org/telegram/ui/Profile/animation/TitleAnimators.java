package org.telegram.ui.Profile.animation;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ProfileActivity;

public class TitleAnimators {

    private static final float COLLAPSED_SCALE_XY = (float) ProfileActivity.NAME_TEXT_SIZE_MIN_DP / ProfileActivity.NAME_TEXT_SIZE_DEFAULT_DP;;
    private static final float DEFAULT_SCALE_XY = 1f;
    private static final float EXPANDED_SCALE_XY = (float) ProfileActivity.NAME_TEXT_SIZE_EXPANDED_DP / ProfileActivity.NAME_TEXT_SIZE_DEFAULT_DP;

    private static final float DEFAULT_DRAWABLE_2_SCALE = 1.3f;

    public static void animateTitle(
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
        animateTitleScale(
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
        animateTitlePosition(
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

    
    private static void animateTitlePosition(
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
        for(int i = 0; i < profileActivity.nameTextView.length; i++) {
            final SimpleTextView textView = profileActivity.nameTextView[i];
            if(textView == null) continue; 

            final float textHeight = textView.getTextHeight();
            final float textWidth = textView.getTextWidth();
            final float screenWidth = profileActivity.getListView().getMeasuredWidth();

            float transitionOffset = 0f;
            if (profileActivity.profileTransitionInProgress) {
                transitionOffset = AndroidUtilities.dpf2(54);
            }

            
            final float translationX0 = AndroidUtilities.dp(16) + transitionOffset;
            final float translationY0 = statusBarHeight + actionBarHeight / 2f - textHeight + AndroidUtilities.dpf2(0.5f);
            

            final float translationX1;
            if(textWidth > textView.getWidth()) {
                translationX1 = screenWidth / 2f - textView.getWidth() / 2f - AndroidUtilities.dp(ProfileActivity.NAME_TEXT_LEFT_MARGIN_DP) - textView.getRightDrawableEndPadding();
            } else {
                translationX1 = screenWidth / 2f - textWidth / 2f - AndroidUtilities.dp(ProfileActivity.NAME_TEXT_LEFT_MARGIN_DP) + textView.getRightDrawableEndPadding() - textView.getLeftDrawableWidth() / 2f - textView.getRightDrawable2Width() / 2f - textView.getRightDrawableWidth() / 2f;
            }
            final float translationY1 = statusBarHeight + actionBarHeight + AvatarAnimators.EXPANDED_AVATAR_SIZE_PX - textView.getHeight() + AndroidUtilities.dp(14);
            
            final float translationX2 = AndroidUtilities.dp(20f) - AndroidUtilities.dp(ProfileActivity.NAME_TEXT_LEFT_MARGIN_DP);
            final float translationY2 = expandedHeight + statusBarHeight + actionBarHeight - ProfileActivity.BUTTONS_BLOCK_HEIGHT_PX - textView.getHeight() - AndroidUtilities.dp(24);

            
            float translationX;
            float translationY;

            if (progress <= keypointDefault) {
                float localT = AndroidUtilities.ilerpClamp(progress, 0.19101022f, keypointDefault);
                float interpolatedT = localT;

                translationX = AndroidUtilities.lerpClamp(translationX0, translationX1, interpolatedT);
                translationY = AndroidUtilities.lerpClamp(translationY0, translationY1, interpolatedT);
            } else if (progress <= keypointExpanded) {
                float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointExpanded);
                float interpolatedT = localT;

                translationX = AndroidUtilities.lerpClamp(translationX1, translationX2, interpolatedT);
                translationY = AndroidUtilities.lerpClamp(translationY1, translationY2, interpolatedT);
            } else {
                translationX = translationX2;
                float overscroll = profileActivity.extraHeight - expandedHeight;
                translationY = translationY2 + Math.max(0, overscroll);
            }

            textView.setTranslationX(translationX);
            textView.setTranslationY(translationY);

            textView.setScrollNonFitTextNonBlocking(progress >= keypointDefault - 0.02);
        }
    }

    
    private static void animateTitleScale(
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
        for(int i = 0; i < profileActivity.nameTextView.length; i++) {
            final SimpleTextView textView = profileActivity.nameTextView[i];
            if(textView == null) continue; 

            
            final float scaleXY0 = COLLAPSED_SCALE_XY;
            
            final float scaleXY1 = DEFAULT_SCALE_XY;
            
            final float scaleXY2 = EXPANDED_SCALE_XY;

            
            float scaleXY;

            float drawableScale;
            float drawableScale0 = 1f;
            float drawableScale1 = DEFAULT_DRAWABLE_2_SCALE;

            if (progress <= keypointDefault) {
                float localT = AndroidUtilities.ilerpClamp(progress, 0.19101022f, keypointDefault);
                float interpolatedT = localT;

                scaleXY = AndroidUtilities.lerpClamp(scaleXY0, scaleXY1, interpolatedT);
                drawableScale = AndroidUtilities.lerpClamp(drawableScale0 / scaleXY, drawableScale1, interpolatedT);
            } else {
                float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointExpanded - 0.05f);
                float interpolatedT = localT;

                scaleXY = AndroidUtilities.lerpClamp(scaleXY1, scaleXY2, interpolatedT);
                drawableScale = DEFAULT_DRAWABLE_2_SCALE / scaleXY;
            }

            textView.setPivotX(0f);
            textView.setPivotY(textView.getHeight());
            textView.setScaleX(scaleXY);
            textView.setScaleY(scaleXY);


            textView.setRightDrawableScale(drawableScale);
            textView.setRightDrawable2Scale(drawableScale);
            textView.setLeftDrawableScale(drawableScale);
        }
    }
}
