package org.telegram.ui.Profile.animation;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ProfileActivity;

public class SubtitleAnimators {

    private static final float START_SCALE_XY = 1f;
    private static final float END_SCALE_XY = (float) ProfileActivity.STATUS_TEXT_SIZE_DEFAULT_DP / ProfileActivity.STATUS_TEXT_SIZE_MIN_DP;

    public static void animateSubtitle(
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
        animateSubtitleScale(
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
        
        animateSubtitlePosition(
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

    private static void animateSubtitlePosition(
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
        for (int i = 0; i < profileActivity.onlineTextView.length; i++) {
            if(i == 2 || i == 3) continue;

            final SimpleTextView textView = profileActivity.onlineTextView[i];
            final float textWidth = textView.getTextWidth();
            final float screenWidth = profileActivity.getListView().getMeasuredWidth();

            float transitionOffset = 0f;
            if (profileActivity.profileTransitionInProgress) {
                transitionOffset = AndroidUtilities.dpf2(54);
            }

            float translationX;
            float translationY;

            float translationX0 = AndroidUtilities.dpf2(64f) + transitionOffset;
            float translationY0 = statusBarHeight + actionBarHeight / 2f + AndroidUtilities.dpf2(3f);
            float translationX1 = screenWidth / 2f - textWidth / 2f;
            float translationY1 = statusBarHeight + actionBarHeight + AvatarAnimators.EXPANDED_AVATAR_SIZE_PX + textView.getHeight() * END_SCALE_XY / 2f;
            float translationX2 = AndroidUtilities.dpf2(20f);
            float translationY2 = expandedHeight + statusBarHeight + actionBarHeight - ProfileActivity.BUTTONS_BLOCK_HEIGHT_PX - textView.getHeight() - AndroidUtilities.dp(12);;


            if(progress <= keypointDefault) {
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

            if (i == 1 || i == 0) {
                profileActivity.mediaCounterTextView.setTranslationX(translationX);
                profileActivity.mediaCounterTextView.setTranslationY(translationY);
            }
        }
    }

    private static void animateSubtitleScale(
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
        for (int i = 0; i < profileActivity.onlineTextView.length; i++) {
            final SimpleTextView textView = profileActivity.onlineTextView[i];

            float scaleXY;

            float scaleXY0 = START_SCALE_XY;
            float scaleXY1 = END_SCALE_XY;

            if(progress <= keypointDefault) {
                float localT = AndroidUtilities.ilerpClamp(progress, 0.19101022f, keypointDefault);
                float interpolatedT = localT;

                scaleXY = AndroidUtilities.lerpClamp(scaleXY0, scaleXY1, interpolatedT);
            } else {
                scaleXY = scaleXY1;
            }

            textView.setPivotX(0f);
            textView.setPivotY(profileActivity.onlineTextView[i].getHeight());
            textView.setScaleX(scaleXY);
            textView.setScaleY(scaleXY);
        }
    }
}
