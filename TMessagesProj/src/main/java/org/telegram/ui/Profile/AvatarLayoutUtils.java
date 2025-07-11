package org.telegram.ui.Profile;

import static org.telegram.ui.ProfileActivity.HEADER_HEIGHT_PX;
import static org.telegram.ui.ProfileActivity.OVERSCROLL_HEIGHT_PX;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Profile.animation.ActionButtonsAnimators;
import org.telegram.ui.Profile.animation.AvatarAnimators;
import org.telegram.ui.Profile.animation.SubtitleAnimators;
import org.telegram.ui.Profile.animation.TitleAnimators;
import org.telegram.ui.ProfileActivity;

public class AvatarLayoutUtils {
    public enum AvatarState {
        COLLAPSED,
        HALF_EXPANDED,
        EXPANDED,
    }

    private static final float REFERENCE_SCREEN_WIDTH_DP = 392f;
    private static final float REFERENCE_SCREEN_WIDTH_PX = AndroidUtilities.dpf2(REFERENCE_SCREEN_WIDTH_DP);

    private static boolean isAnimatedToExpanded = false;
    private static boolean isAnimatedFromExpanded = false;

    private static final float EXPAND_THRESHOLD = 0.25f;
    private static final float EXPAND_TO_HALF_THRESHOLD = 0.82f;

    public static void updateAvatarLayout(ProfileActivity profileActivity, float diff, int statusBarWithActionBarHeight) {
        if(profileActivity.getListView().getMeasuredWidth() <= 0) return;
        if(profileActivity.searchListView != null) {
            if (profileActivity.searchListView.getVisibility() == View.VISIBLE) return;
        }

        float extraHeight = profileActivity.extraHeight;
        float maxExtraHeight = profileActivity.getListView().getMeasuredWidth() - profileActivity.getStatusBarWithActionBarHeight() + ProfileActivity.BUTTONS_BLOCK_HEIGHT_PX + OVERSCROLL_HEIGHT_PX;
        float progress = extraHeight / maxExtraHeight;
        if(Float.isNaN(progress) || Float.isInfinite(progress)) return;

        float scaleFactor = profileActivity.getListView().getMeasuredWidth() / REFERENCE_SCREEN_WIDTH_PX;

        
        float statusBarHeight = getStatusBarExtraInCaseOfEdgeToEdge(profileActivity);
        float actionBarHeight = ActionBar.getCurrentActionBarHeight();
        float defaultHeight = ProfileActivity.HEADER_HEIGHT_PX;
        float expandedHeight = maxExtraHeight - OVERSCROLL_HEIGHT_PX;
        float expandThresholdHeight = ProfileActivity.HEADER_HEIGHT_PX + ((expandedHeight - defaultHeight) * EXPAND_THRESHOLD);
        float collapseExpandedThresholdHeight = ProfileActivity.HEADER_HEIGHT_PX + ((expandedHeight - defaultHeight) * EXPAND_TO_HALF_THRESHOLD);
        float expandedOverscrollHeight = maxExtraHeight;


        
        float keypointCollapsed = 0;
        float keypointDefault = defaultHeight / maxExtraHeight;
        float keypointExpandThreshold = expandThresholdHeight / maxExtraHeight;
        float keypointCollapseExpandedThreshold = collapseExpandedThresholdHeight / maxExtraHeight;
        float keypointExpanded = expandedHeight / maxExtraHeight;
        float keypointExpandedOverscroll = 1;

        progress *= scaleFactor;
        keypointDefault *= scaleFactor;
        keypointExpandThreshold *= scaleFactor;
        keypointCollapseExpandedThreshold *= scaleFactor;
        keypointExpanded *= scaleFactor;
        keypointExpandedOverscroll *= scaleFactor;

        AvatarState prevState = profileActivity.avatarState;
        AvatarState newState = calculateNewState(
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
        profileActivity.avatarState = newState;

        if (prevState != newState) {
            switch (newState) {
                case EXPANDED:
                    expandAvatar(profileActivity);
                    break;
                case HALF_EXPANDED:
                    halfExpandAvatar(profileActivity);
                    break;
                case COLLAPSED:
                    collapseAvatar(profileActivity);
                default:
                    break;
            }
        }

        newScroll(
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
    }

    private static void newScroll(
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

        ActionButtonsAnimators.animateActionButtonsV2(
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
        AvatarAnimators.animateAvatarV2(
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
        TitleAnimators.animateTitle(
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
        SubtitleAnimators.animateSubtitle(
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

        if(progress >= keypointCollapseExpandedThreshold && profileActivity.avatarsPagerContainer.getVisibility() != View.VISIBLE) {
            profileActivity.avatarsPagerContainer.setVisibility(View.VISIBLE);

            profileActivity.avatarsViewPager.setCreateThumbFromParent(true);
            profileActivity.avatarsViewPager.getAdapter().notifyDataSetChanged();
            profileActivity.avatarsViewPager.setAnimatedFileMaybe(profileActivity.avatarImage.getImageReceiver().getAnimation());
            profileActivity.avatarsViewPager.resetCurrentItem();

            if(!profileActivity.avatarBackgroundBlurView.renderSourceView) {
                profileActivity.avatarBackgroundBlurView.renderSourceView = true;
            }
            profileActivity.avatarBackgroundBlurView.requestUpdate();
        } else if(progress < keypointCollapseExpandedThreshold && profileActivity.avatarsPagerContainer.getVisibility() != View.GONE) {
            profileActivity.avatarsPagerContainer.setVisibility(View.GONE);

            if(profileActivity.avatarBackgroundBlurView.renderSourceView) {
                profileActivity.avatarBackgroundBlurView.renderSourceView = false;
            }
            profileActivity.avatarBackgroundBlurView.requestUpdate();
        }

        if(progress > keypointExpandThreshold) {
            float localT = AndroidUtilities.ilerpClamp(progress, keypointExpandThreshold, keypointExpanded);
            profileActivity.updateAvatarExpandProgress(localT);

            final ViewGroup.LayoutParams overlaysLp = profileActivity.overlaysView.getLayoutParams();
            overlaysLp.width = profileActivity.getListView().getMeasuredWidth();
            overlaysLp.height = (int) (profileActivity.extraHeight + actionBarHeight + statusBarHeight);
            profileActivity.overlaysView.requestLayout();
        } else {
            profileActivity.updateAvatarExpandProgress(0);
        }

        if (profileActivity.storyView != null) {
            profileActivity.storyView.invalidate();
        }

        if(progress >= keypointCollapseExpandedThreshold && !profileActivity.avatarsViewPagerIndicatorView.isIndicatorVisible()) {
            profileActivity.avatarsViewPagerIndicatorView.refreshVisibility(getDurationFactor(profileActivity));
        } else if(progress < keypointExpandThreshold && profileActivity.avatarsViewPagerIndicatorView.isIndicatorVisible()) {
            profileActivity.avatarsViewPagerIndicatorView.refreshVisibility(getDurationFactor(profileActivity));
        }

        if(progress > keypointCollapseExpandedThreshold) {
            if(profileActivity.avatarContainer.getVisibility() != View.INVISIBLE) {
                profileActivity.avatarContainer.setVisibility(View.INVISIBLE);
            }
        } else if(profileActivity.avatarContainer.getVisibility() != View.VISIBLE) {
            profileActivity.avatarContainer.setVisibility(View.VISIBLE);
        }
    }

    private static AvatarState calculateNewState(
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
        if (progress < keypointExpandThreshold) {
            isAnimatedFromExpanded = false;
        }

        if (progress > keypointCollapseExpandedThreshold) {
            isAnimatedToExpanded = false;
        }

        if (profileActivity.profileTransitionInProgress) {
            return profileActivity.avatarState;
        } else if (progress <= keypointDefault / 2f) {
            return AvatarState.COLLAPSED;
        } else if (isAnimatedFromExpanded) {
            return AvatarState.HALF_EXPANDED;
        } else if (isAnimatedToExpanded) {
            return AvatarState.EXPANDED;
        } else if (progress <= keypointDefault) {
            return AvatarState.HALF_EXPANDED;
        } else if(profileActivity.isPulledDown && progress <= keypointCollapseExpandedThreshold) {
            return AvatarState.HALF_EXPANDED;
        } else if (progress >= keypointExpandThreshold) {
            return AvatarState.EXPANDED;
        } else if(profileActivity.isPulledDown) {
            return AvatarState.EXPANDED;
        } else {
            return AvatarState.HALF_EXPANDED;
        }
    }

    private static void collapseAvatar(ProfileActivity profileActivity) {
        profileActivity.isPulledUp = true;
        profileActivity.isPulledDown = false;
    }

    private static void halfExpandAvatar(ProfileActivity profileActivity) {
        final View view = profileActivity.layoutManager.findViewByPosition(0);
        if(profileActivity.isPulledDown && view != null) {
            int scrollPosition = view.getTop() - (int) HEADER_HEIGHT_PX;
            if (scrollPosition != 0) {
                isAnimatedFromExpanded = true;
                profileActivity.getListView().smoothUninterruptibleScrollBy(0, scrollPosition, (int) (500 / getDurationFactor(profileActivity)), CubicBezierInterpolator.EASE_OUT_QUINT, true);
            }
        }

        profileActivity.isPulledUp = false;
        profileActivity.isPulledDown = false;
        profileActivity.avatarBackgroundBlurView.setVisibility(View.VISIBLE);
    }

    
    private static void expandAvatar(ProfileActivity profileActivity) {
        if(!profileActivity.isPulledDown) {
            final View view = profileActivity.layoutManager.findViewByPosition(0); 

            if(view == null) return;

            int targetScrollPosition = view.getTop() - profileActivity.getListView().getMeasuredWidth() + profileActivity.getStatusBarWithActionBarHeight() - ProfileActivity.BUTTONS_BLOCK_HEIGHT_PX;

            if(targetScrollPosition < 0) {
                isAnimatedToExpanded = true;

                profileActivity.getListView().smoothUninterruptibleScrollBy(0, targetScrollPosition, (int) (500 / getDurationFactor(profileActivity)), CubicBezierInterpolator.EASE_OUT_QUINT, true);
            }
        }
        profileActivity.isPulledDown = true;
        profileActivity.isPulledUp = false;
        profileActivity.avatarBackgroundBlurView.setVisibility(View.VISIBLE);
    }

    private static float getDurationFactor(ProfileActivity profileActivity) {
        float velocity = Math.abs(profileActivity.listViewVelocityY);
        float clampedVelocity = MathUtils.clamp(velocity, AndroidUtilities.dpf2(1100f), AndroidUtilities.dpf2(2000f));
        return clampedVelocity / AndroidUtilities.dpf2(1100f);
    }

    
    private static int getStatusBarExtraInCaseOfEdgeToEdge(ProfileActivity profileActivity) {
        return (profileActivity.getActionBar().getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0);
    }
}
