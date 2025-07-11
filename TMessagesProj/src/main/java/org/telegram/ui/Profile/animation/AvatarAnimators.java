package org.telegram.ui.Profile.animation;

import static org.telegram.ui.Components.CubicBezierInterpolator.EASE_BOTH;
import static org.telegram.ui.Components.quickforward.QuickShareSelectorDrawable.Interpolators.LINEAR_INTERPOLATOR;
import static org.telegram.ui.ProfileActivity.HEADER_HEIGHT_PX;
import static org.telegram.ui.ProfileActivity.BUTTONS_BLOCK_HEIGHT_PX;

import android.graphics.RectF;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.GooeyDynamicIslandView;
import org.telegram.ui.Components.Profile.BoxBlurView;
import org.telegram.ui.ProfileActivity;

public class AvatarAnimators {

    private static final float EXPANDED_AVATAR_SIZE = 98f;
    static final float EXPANDED_AVATAR_SIZE_PX = AndroidUtilities.dpf2(EXPANDED_AVATAR_SIZE);
    private static final float COLLAPSED_AVATAR_SIZE = AndroidUtilities.dpf2(42f);

    private static final float SHOW_HIDE_METABALL_VIEW_THRESHOLD = 0.14f;

    public static void animateAvatarV2(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        animateAvatarPosition(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateAvatarScale(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateAvatarBlur(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateAvatarAlpha(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateAvatarGifts(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animatePagerScale(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        animateStoriesInset(
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
                keypointCollapseExpandedThreshold,
                keypointExpanded,
                keypointExpandedOverscroll
        );
        if (!profileActivity.isTopic) {
            animateAvatarMetaballPosition(
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
                    keypointCollapseExpandedThreshold,
                    keypointExpanded,
                    keypointExpandedOverscroll
            );
            animateAvatarMetaballScale(
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
                    keypointCollapseExpandedThreshold,
                    keypointExpanded,
                    keypointExpandedOverscroll
            );
        } else if (profileActivity.amorphicLayerView != null) {
            profileActivity.amorphicLayerView.setVisibility(View.GONE);
        }
    }

    private static void animateAvatarBlur(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        final View avatarContainer = profileActivity.avatarContainer;
        final RectF avatarBounds = getAvatarBounds(avatarContainer);

        final BoxBlurView avatarBackgroundBlurView = profileActivity.avatarBackgroundBlurView;

        float translationY = avatarBounds.bottom - avatarBackgroundBlurView.getHeight() + AndroidUtilities.dp(1);
        float translationX = 0;


        float scaleXY = avatarBounds.width() / (avatarBackgroundBlurView.getWidth() - AndroidUtilities.dpf2(2f));

        if(progress < keypointDefault && avatarBackgroundBlurView.getVisibility() != View.GONE) {
            avatarBackgroundBlurView.setVisibility(View.GONE);
        } else if(avatarBackgroundBlurView.getVisibility() != View.VISIBLE) {
            avatarBackgroundBlurView.setVisibility(View.VISIBLE);
            profileActivity.updateBlurImageReceiver();
        }

        float gradientStart;
        float gradientStart0 = 1f;
        float gradientStart1 = 0f;
        float gradientEnd;
        float gradientEnd0 = 1f;
        float gradientEnd1 = 0.35f;
        final float gradientKeypointOffset = 0.1f;
        if(progress < keypointDefault + gradientKeypointOffset) { 
            gradientStart = gradientStart0;
            gradientEnd = gradientEnd0;
        } else {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault + gradientKeypointOffset, keypointExpanded - 0.1f);
            float interpolatedT = LINEAR_INTERPOLATOR.getInterpolation(localT);

            gradientStart = AndroidUtilities.lerpClamp(gradientStart0, gradientStart1, interpolatedT);
            gradientEnd = AndroidUtilities.lerpClamp(gradientEnd0, gradientEnd1, interpolatedT);
        }

        final float translationYKeypointOffset = 0.3f;
        float extraYTranslation;
        float extraYTranslation0 = 0f;
        float extraYTranslation1 = BUTTONS_BLOCK_HEIGHT_PX; 
        if(progress < keypointDefault + translationYKeypointOffset) {
            extraYTranslation = extraYTranslation0;
        } else {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault + translationYKeypointOffset, keypointExpanded);
            extraYTranslation = AndroidUtilities.lerpClamp(extraYTranslation0, extraYTranslation1, localT);
        }

        avatarBackgroundBlurView.setTranslationY(translationY + extraYTranslation);
        avatarBackgroundBlurView.setTranslationX(translationX);
        if(progress <= keypointExpanded) {
            avatarBackgroundBlurView.setPivotX(avatarBackgroundBlurView.getWidth() / 2f);
            avatarBackgroundBlurView.setPivotY(avatarBackgroundBlurView.getHeight());
        } else {
            avatarBackgroundBlurView.setPivotX(avatarBackgroundBlurView.getWidth() / 2f);
            avatarBackgroundBlurView.setPivotY(0f);
        }
        avatarBackgroundBlurView.setScaleX(scaleXY);
        avatarBackgroundBlurView.setScaleY(scaleXY);
        avatarBackgroundBlurView.setGradientAlpha(gradientStart, gradientEnd);
    }

    private static RectF getAvatarBounds(View avatarContainer) {
        final RectF avatarBounds = new RectF(0, 0, COLLAPSED_AVATAR_SIZE, COLLAPSED_AVATAR_SIZE);
        avatarContainer.getMatrix().mapRect(avatarBounds);
        return avatarBounds;
    }

    private static void animateAvatarPosition(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        final float SCREEN_WIDTH = profileActivity.getListView().getMeasuredWidth();
        final GooeyDynamicIslandView metaball = profileActivity.amorphicLayerView;


        float translationX;
        float translationY;

        float translationX0 = (SCREEN_WIDTH - COLLAPSED_AVATAR_SIZE) / 2f;
        float translationX1 = (SCREEN_WIDTH - COLLAPSED_AVATAR_SIZE) / 2f;
        float translationX2 = (SCREEN_WIDTH - COLLAPSED_AVATAR_SIZE) / 2f;

        float translationY0 = metaball.getIslandRect().centerY() - profileActivity.avatarContainer.getWidth() / 2f;
        float translationY1 = statusBarHeight + actionBarHeight / 2f + COLLAPSED_AVATAR_SIZE / 2f;
        float translationY2 = (profileActivity.getListView().getMeasuredWidth() - COLLAPSED_AVATAR_SIZE) / 2f;

        if (profileActivity.profileTransitionInProgress) {
            translationX0 = AndroidUtilities.dp(64);
            translationY0 = statusBarHeight + (actionBarHeight) / 2 - COLLAPSED_AVATAR_SIZE / 2 + AndroidUtilities.dpf2(0.5f);
        }

        if(progress <= keypointDefault) {
            float localT;
            if(profileActivity.profileTransitionInProgress) {
                localT = AndroidUtilities.ilerpClamp(progress, 0f, keypointDefault);
            } else {
                localT = AndroidUtilities.ilerpClamp(progress, 0.16371681f, keypointDefault);
            }
            float interpolatedT = localT;

            translationX = AndroidUtilities.lerpClamp(translationX0, translationX1, interpolatedT);
            translationY = AndroidUtilities.lerpClamp(translationY0, translationY1, interpolatedT);
        } else {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointExpanded);
            float interpolatedT = EASE_BOTH.getInterpolation(localT);

            translationX = AndroidUtilities.lerpClamp(translationX1, translationX2, interpolatedT);
            translationY = AndroidUtilities.lerpClamp(translationY1, translationY2, interpolatedT);
        }


        profileActivity.avatarContainer.setTranslationX(translationX);
        profileActivity.avatarContainer.setTranslationY(translationY);
    }

    private static void animateAvatarScale(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        final float SCREEN_WIDTH = profileActivity.getListView().getMeasuredWidth();

        float scaleXY;
        float avatarRadius;

        float scaleXY0 = 0f;
        float scaleXY1 = EXPANDED_AVATAR_SIZE_PX / COLLAPSED_AVATAR_SIZE;
        float scaleXY2 = SCREEN_WIDTH / COLLAPSED_AVATAR_SIZE;
        float scaleXY3 = (SCREEN_WIDTH + (expandedOverscrollHeight - expandedHeight)) / COLLAPSED_AVATAR_SIZE;

        float avatarRadius0 = AndroidUtilities.dp(21);
        float avatarRadius1 = profileActivity.getSmallAvatarRoundRadius();
        float avatarRadius2 = 0f;

        if (profileActivity.profileTransitionInProgress) {
            scaleXY0 = 1f;
            avatarRadius0 = profileActivity.getSmallAvatarRoundRadius();
        }

        if(progress <= keypointDefault) {
            float localT = AndroidUtilities.ilerpClamp(progress, SHOW_HIDE_METABALL_VIEW_THRESHOLD / 2f, keypointDefault);
            float radiusTime = AndroidUtilities.ilerpClamp(progress, 0.41f, keypointDefault);
            float interpolatedT = localT;

            scaleXY = AndroidUtilities.lerpClamp(scaleXY0, scaleXY1, interpolatedT);
            avatarRadius = AndroidUtilities.lerpClamp(avatarRadius0, avatarRadius1, radiusTime);
        } else if(progress <= keypointExpanded){
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointExpanded);
            float interpolatedT = EASE_BOTH.getInterpolation(localT);

            scaleXY = AndroidUtilities.lerpClamp(scaleXY1, scaleXY2, interpolatedT);
            avatarRadius = AndroidUtilities.lerpClamp(avatarRadius1, avatarRadius2, interpolatedT);
        } else {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointExpanded, keypointExpandedOverscroll);
            float interpolatedT = localT;
            scaleXY = AndroidUtilities.lerpClamp(scaleXY2, scaleXY3, interpolatedT);
            avatarRadius = 0;
        }

        float blurRadius;
        float blurKeypointOffset = 0.20f;
        if(progress < keypointDefault + blurKeypointOffset) {
            blurRadius = avatarRadius;
        }  else {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault + blurKeypointOffset, keypointExpanded);
            blurRadius = avatarRadius - (avatarRadius * localT);
        }

        
        profileActivity.avatarContainer.setPivotX(profileActivity.avatarContainer.getWidth() / 2f);
        profileActivity.avatarContainer.setPivotY(profileActivity.avatarContainer.getHeight() / 2f);
        profileActivity.avatarContainer.setScaleX(scaleXY);
        profileActivity.avatarContainer.setScaleY(scaleXY);
        profileActivity.avatarImage.setRoundRadius((int) avatarRadius);
        profileActivity.avatarBackgroundBlurView.setCornerRadiusBottomLeft(blurRadius * scaleXY);
        profileActivity.avatarBackgroundBlurView.setCornerRadiusBottomRight(blurRadius * scaleXY);
    }

    private static void animatePagerScale(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        final View avatarContainer = profileActivity.avatarContainer;
        final RectF avatarBounds = getAvatarBounds(avatarContainer);
        final View tempContainer = profileActivity.avatarsPagerContainer;

        if(tempContainer == null || tempContainer.getWidth() <= 0) return;

        float scaleXY = avatarBounds.width() / tempContainer.getWidth();

        
        float normalizedYTranslation = -tempContainer.getHeight() / 2f;

        tempContainer.setTranslationY(normalizedYTranslation + avatarBounds.centerY());

        tempContainer.setPivotX(tempContainer.getWidth() / 2f);
        tempContainer.setPivotY(tempContainer.getHeight() / 2f);
        tempContainer.setScaleX(scaleXY);
        tempContainer.setScaleY(scaleXY);
    }

    private static void animateStoriesInset(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        if (profileActivity.needInsetForStories()) {
            float storiesProgress;

            if (progress <= keypointDefault) {
                storiesProgress = 1f;
            } else if (progress >= keypointDefault + 0.1f) {
                storiesProgress = 0f;
            } else {
                float t = AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointDefault + 0.1f);
                storiesProgress = 1f - t;
            }

            float alpha;
            if(progress <= keypointDefault) {
                if(profileActivity.profileTransitionInProgress) {
                    alpha = 1f;
                } else {
                    alpha = AndroidUtilities.ilerpClamp(progress, 0.41f, keypointDefault);
                }
            } else {
                alpha = 1 - AndroidUtilities.ilerpClamp(progress, keypointDefault, keypointExpandThreshold);
            }

            profileActivity.avatarImage.setProgressToStoriesInsets(storiesProgress);
            if (profileActivity.storyView != null) {
                profileActivity.storyView.setProgressToStoriesInsets(storiesProgress);
                profileActivity.storyView.setAlpha(alpha);
            }
        }
    }

    private static void animateAvatarMetaballPosition(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        GooeyDynamicIslandView metaball = profileActivity.amorphicLayerView;

        if (profileActivity.profileTransitionInProgress) {
            metaball.setVisibility(View.GONE);
            return;
        }

        final float ACTION_BAR_HEIGHT = ActionBar.getCurrentActionBarHeight();
        final float STATUS_BAR_OFFSET = profileActivity.getActionBar().getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0;

        if(progress == 0) {
            metaball.setVisibility(View.GONE);
        } else if(progress <= keypointDefault) {
            metaball.setVisibility(View.VISIBLE);
            final float translationYStart = metaball.getIslandRect().centerY();
            final float translationYEnd = STATUS_BAR_OFFSET + ACTION_BAR_HEIGHT / 2f + COLLAPSED_AVATAR_SIZE / 2f + profileActivity.avatarContainer.getWidth() / 2f;

            float localTime = AndroidUtilities.ilerp(progress, 0.16371681f, keypointDefault);
            float interpolatedTime = localTime;

            float y = AndroidUtilities.lerpClamp(translationYStart, translationYEnd, interpolatedTime);

            metaball.setAvatarY(y);
        } else {
            metaball.setVisibility(View.GONE);
        }
    }

    private static void animateAvatarMetaballScale(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float scaleAvatarXY;

        float scaleAvatarXY0 = 0f;
        float scaleAvatarXY1 = EXPANDED_AVATAR_SIZE_PX / COLLAPSED_AVATAR_SIZE;
        float scaleAvatarXY2 = 0f;

        if(progress < keypointDefault) {
            float localT = AndroidUtilities.ilerpClamp(progress, SHOW_HIDE_METABALL_VIEW_THRESHOLD / 2f, keypointDefault);
            float interpolatedT = localT;
            scaleAvatarXY = AndroidUtilities.lerpClamp(scaleAvatarXY0, scaleAvatarXY1, interpolatedT);
        } else {
            scaleAvatarXY = scaleAvatarXY2;
        }

        float islandScaleXY;
        float islandScaleXY0 = 0;
        float islandScaleXY1 = 1f;
        float islandScaleXY2 = 0f;

        if(progress <= SHOW_HIDE_METABALL_VIEW_THRESHOLD) {
            float localT = AndroidUtilities.ilerpClamp(progress, 0, SHOW_HIDE_METABALL_VIEW_THRESHOLD);
            float interpolatedT = localT;
            islandScaleXY = AndroidUtilities.lerpClamp(islandScaleXY0, islandScaleXY1, interpolatedT);
        } else if(progress <= keypointDefault - SHOW_HIDE_METABALL_VIEW_THRESHOLD) {
            islandScaleXY = islandScaleXY1;
        } else if(progress <= keypointDefault) {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointDefault - SHOW_HIDE_METABALL_VIEW_THRESHOLD, keypointDefault);
            float interpolatedT = localT;
            islandScaleXY = AndroidUtilities.lerpClamp(islandScaleXY1, islandScaleXY2, interpolatedT);
        } else {
            islandScaleXY = 0;
        }


        profileActivity.amorphicLayerView.setInsets(profileActivity.avatarImage.getInsets());
        profileActivity.amorphicLayerView.setScale(scaleAvatarXY);
        profileActivity.amorphicLayerView.setIslandScale(islandScaleXY);
    }

    private static void animateAvatarGifts(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float extraHeight = profileActivity.extraHeight;
        float localTime = AndroidUtilities.ilerp(extraHeight, 0, HEADER_HEIGHT_PX);
        float interpolatedTime = LINEAR_INTERPOLATOR.getInterpolation(localTime);

        float acceleratedProgress = AndroidUtilities.clamp((1 - interpolatedTime) * 1.2f, 0f, 1f);

        float giftsCenterY = statusBarHeight + actionBarHeight / 2f + COLLAPSED_AVATAR_SIZE / 2f + profileActivity.avatarContainer.getPivotY() / 2;
        float avatarCenterY = profileActivity.avatarContainer.getTranslationY()
                + profileActivity.avatarContainer.getPivotY();

        if (extraHeight >= HEADER_HEIGHT_PX) {
            float alpha = 1 - scaleProgress(interpolatedTime, 1f, 1.2f, 0f, 1f);
            profileActivity.topView.updateBackgroundEmojis(giftsCenterY, avatarCenterY, 0f);
            if(!profileActivity.profileTransitionInProgress) {
                profileActivity.giftsView.setAlpha(alpha);
            }
            profileActivity.giftsView.updateCoordinates(giftsCenterY, avatarCenterY, 0f);
        } else {
            profileActivity.topView.updateBackgroundEmojis(giftsCenterY, avatarCenterY, acceleratedProgress);
            if(!profileActivity.profileTransitionInProgress) {
                profileActivity.giftsView.setAlpha(1f);
            }
            profileActivity.giftsView.updateCoordinates(giftsCenterY, avatarCenterY, acceleratedProgress);
        }
    }

    private static void animateAvatarAlpha(
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
            float keypointCollapseExpandedThreshold,
            float keypointExpanded,
            float keypointExpandedOverscroll
    ) {
        float extraHeight = profileActivity.extraHeight;

        if (profileActivity.profileTransitionInProgress || extraHeight >= HEADER_HEIGHT_PX) {
            profileActivity.avatarContainer.setAlpha(1f);
            profileActivity.avatarImage.getBlurImageReceiver().setAlpha(0f);
            return;
        }


        float localTime = AndroidUtilities.ilerp(extraHeight, 0, HEADER_HEIGHT_PX);
        float interpolatedTime = LINEAR_INTERPOLATOR.getInterpolation(localTime);

        float alpha = calculateSubProgress(interpolatedTime, 0.35f, 0.5f);
        float blur = calculateSubProgress(interpolatedTime, 0.45f, 0.6f);

        profileActivity.avatarContainer.setAlpha(alpha);

        profileActivity.avatarImage.getImageReceiver().setAlpha(blur);
        profileActivity.avatarImage.getBlurImageReceiver().setAlpha(1f - blur);
        profileActivity.avatarImage.invalidate();
    }


    public static float calculateSubProgress(float mainProgress, float intervalStart, float intervalEnd) {
        
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

    public static float scaleProgress(float value, float minOrig, float maxOrig, float minTarget, float maxTarget) {
        
        if (value < minOrig) {
            value = minOrig;
        } else if (value > maxOrig) {
            value = maxOrig;
        }
        
        return ((value - minOrig) / (maxOrig - minOrig)) * (maxTarget - minTarget) + minTarget;
    }
}
