package org.telegram.ui.Components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ReactionsSeenView;

import java.util.ArrayList;

public class ReactionsViewPager extends ViewPagerFixed{

    private boolean showOnlyOnePage;

    public ArrayList<ReactionsSeenView.UserAndReactions> users = new ArrayList<>();
    public ArrayList<ReactionsSeenView.UserAndReactions> onlyReactedUsers = new ArrayList<>();
    public ArrayList<TLRPC.TL_availableReaction> pages = new ArrayList<>();
    private ItemClickListener listener = null;

    public ArrayList<ReactionsSeenView.UserAndReactions> getFiltered(String reaction) {
        ArrayList<ReactionsSeenView.UserAndReactions> result = new ArrayList<>();

        for(ReactionsSeenView.UserAndReactions userAndReaction : users) {
            if(userAndReaction.reaction.reaction.equals(reaction)) {
                result.add(userAndReaction);
            }
        }

        return result;
    }
    public ReactionsViewPager(@NonNull Context context, ArrayList<ReactionsSeenView.UserAndReactions> users, ArrayList<ReactionsSeenView.UserAndReactions> onlyReactedUsers, ArrayList<TLRPC.TL_availableReaction> pages) {
        super(context);

        this.users.clear();
        this.users.addAll(users);
        this.pages.clear();
        this.pages.addAll(pages);
        this.onlyReactedUsers.clear();
        this.onlyReactedUsers.addAll(onlyReactedUsers);

        createTabsView();


        setAdapter(new ViewPagerFixed.Adapter() {

            @Override
            public TLRPC.TL_availableReaction getItemReaction(int position) {
                if(position == 0) {
                    return null;
                } else {
                    return pages.get(position - 1);
                }
            }

            @Override
            public int getReactionCount(int position) {
                if(position == 0) {
                    return onlyReactedUsers.size();
                } else {
                    return getFiltered(pages.get(position - 1).reaction).size();
                }
            }

            @Override
            public String getItemTitle(int position) {
                if (position == 0) {
                    return LocaleController.getString("SearchAllChatsShort", R.string.SearchAllChatsShort);
                } else {
                    return "Chats";
                }
            }

            @Override
            public int getItemCount() {
                return pages.size() + 1;
            }

            @Override
            public View createView(int viewType) {
                RecyclerView layout = createListView();


                layout.addOnItemTouchListener(new RecyclerItemClickListener(context, layout, new RecyclerItemClickListener.OnItemClickListener() {
                        @Override
                        public void onItemClick(View view, int position) {
                            if(currentPosition == 0) {
                                listener.onClick(users.get(position).user);
                            } else {
                                String currentReaction = pages.get(currentPosition - 1).reaction;
                                listener.onClick(getFiltered(currentReaction).get(position).user);
                            }
                        }

                        @Override
                        public void onLongItemClick(View view, int position) {

                        }
                    })
                );

                layout.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

                return layout;
            }

            @Override
            public int getItemViewType(int position) {
                return 1;
            }

            @Override
            public void bindView(View view, int position, int viewType) {
                final ArrayList<ReactionsSeenView.UserAndReactions> filteredList;
                if(position == 0) {
                    filteredList = users;
                } else {
                    filteredList = getFiltered(pages.get(position - 1).reaction);
                }


                ((RecyclerView) view).setAdapter(new RecyclerListView.SelectionAdapter() {

                    @Override
                    public boolean isEnabled(RecyclerView.ViewHolder holder) {
                        return true;
                    }

                    @Override
                    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        UserCell userCell = new UserCell(parent.getContext());
                        userCell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                        return new RecyclerListView.Holder(userCell);
                    }

                    @Override
                    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                        UserCell cell = (UserCell) holder.itemView;
                        cell.setUser(filteredList.get(position));
                    }

                    @Override
                    public int getItemCount() {
                        return filteredList.size();
                    }
                });
            }
        });
    }

    public interface ItemClickListener {
        void onClick(TLRPC.User user);
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public TabsView createTabsView() {
        tabsView = new ReactionsTabView(getContext());
        tabsView.setDelegate(new TabsView.TabsViewDelegate() {
            @Override
            public void onPageSelected(int page, boolean forward) {
                animatingForward = forward;
                nextPosition = page;
                updateViewForIndex(1);

                if (forward) {
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth());
                } else {
                    viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth());
                }
            }

            @Override
            public void onPageScrolled(float progress) {
                if (progress == 1f) {
                    if (viewPages[1] != null) {
                        swapViews();
                        viewsByType.put(viewTypes[1], viewPages[1]);
                        removeView(viewPages[1]);
                        viewPages[0].setTranslationX(0);
                        viewPages[1] = null;
                    }
                    return;
                }
                if (viewPages[1] == null) {
                    return;
                }
                if (animatingForward) {
                    viewPages[1].setTranslationX(viewPages[0].getMeasuredWidth() * (1f - progress));
                    viewPages[0].setTranslationX(-viewPages[0].getMeasuredWidth() * progress);
                } else {
                    viewPages[1].setTranslationX(-viewPages[0].getMeasuredWidth() * (1f - progress));
                    viewPages[0].setTranslationX(viewPages[0].getMeasuredWidth() * progress);
                }
            }

            @Override
            public void onSamePageSelected() {

            }

            @Override
            public boolean canPerformActions() {
                return !tabsAnimationInProgress && !startedTracking;
            }
        });
        fillTabs();
        return tabsView;
    }

    public RecyclerView createListView() {
        RecyclerView recyclerListView = new RecyclerView(getContext());
        recyclerListView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerListView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int p = parent.getChildAdapterPosition(view);
                if (p == 0) {
                    outRect.top = AndroidUtilities.dp(4);
                }
                if (p == users.size() - 1) {
                    outRect.bottom = AndroidUtilities.dp(4);
                }
            }
        });
        recyclerListView.setAdapter(new RecyclerListView.SelectionAdapter() {
            @Override
            public boolean isEnabled(RecyclerView.ViewHolder holder) {
                return true;
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                UserCell userCell = new UserCell(parent.getContext());
                userCell.setLayoutParams(new RecyclerView.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
                return new RecyclerListView.Holder(userCell);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                UserCell cell = (UserCell) holder.itemView;
                cell.setUser(users.get(position));
            }

            @Override
            public int getItemCount() {
                return users.size();
            }
        });
        return recyclerListView;
    }

    class ReactionsTabView extends TabsView {

        public ReactionsTabView(Context context) {
            super(context);
        }


        @Override
        public TabsView.ListAdapter getAdapterForPages() {
            return new ReactionsListAdapter(getContext());
        }


        class ReactionsListAdapter extends TabsView.ListAdapter {

            public ReactionsListAdapter(Context context) {
                super(context);
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return new RecyclerListView.Holder(new ReactionTabView(getContext()));
            }
        }

        @Override
        public void drawIndicator(int x, int height, int width, Canvas canvas) {
            // do nothing
        }

        @Override
        public int getTabId(TabView tabView) {
            return tabView.getId();
        }

        public class ReactionTabView extends TabView {

            private Tab currentTab;
            private int textHeight;
            private int tabWidth;
            private int currentPosition;
            private RectF rect = new RectF();
            private String currentText;
            private StaticLayout textLayout;
            private int textOffsetX;
            private ReactionFilterDrawable drawable;


            public ReactionTabView(Context context) {
                super(context);
            }

            public void setTab(Tab tab, int position) {
                currentTab = tab;
                drawable = new ReactionFilterDrawable(currentTab.reactionCount, currentTab.reaction, getResources().getDrawable(R.drawable.reactions_all));
                currentPosition = position;
                setContentDescription(tab.title);
                requestLayout();
            }

            @Override
            public int getId() {
                return currentTab.id;
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int w = drawable.getIntrinsicWidth() + AndroidUtilities.dp(8);
                setMeasuredDimension(w, MeasureSpec.getSize(heightMeasureSpec));
            }

            @SuppressLint("DrawAllocation")
            @Override
            protected void onDraw(Canvas canvas) {
                String key;
                String animateToKey;
                String otherKey;
                String animateToOtherKey;
                String unreadKey;
                String unreadOtherKey;
                int id1;
                int id2;
                if (manualScrollingToId != -1) {
                    id1 = manualScrollingToId;
                    id2 = selectedTabId;
                } else {
                    id1 = selectedTabId;
                    id2 = previousId;
                }
                if (currentTab.id == id1) {
                    key = activeTextColorKey;
                    otherKey = unactiveTextColorKey;
                    unreadKey = Theme.key_chats_tabUnreadActiveBackground;
                    unreadOtherKey = Theme.key_chats_tabUnreadUnactiveBackground;
                } else {
                    key = unactiveTextColorKey;
                    otherKey = activeTextColorKey;
                    unreadKey = Theme.key_chats_tabUnreadUnactiveBackground;
                    unreadOtherKey = Theme.key_chats_tabUnreadActiveBackground;
                }

                if ((animatingIndicator || manualScrollingToId != -1) && (currentTab.id == id1 || currentTab.id == id2)) {

                    if (currentTab.id == id1) {
                        drawable.setProgress(animatingIndicatorProgress);

                    } else {
                        drawable.setProgress(1f - animatingIndicatorProgress);

                    }
                    textPaint.setColor(ColorUtils.blendARGB(Theme.getColor(otherKey), Theme.getColor(key), animatingIndicatorProgress));
                } else {
                    if(currentTab.id == id1) {
                        drawable.setProgress(1f);
                    } else {
                        drawable.setProgress(0f);
                    }
                    textPaint.setColor(Theme.getColor(key));
                }

                drawable.getIntrinsicHeight();
                int offsetY = (getHeight() - drawable.getIntrinsicHeight()) / 2;
                int offsetX = (getWidth() - drawable.getIntrinsicWidth()) / 2;
                canvas.save();
                canvas.translate(offsetX, offsetY);
                drawable.setBounds(offsetX, offsetY, getWidth() - offsetX, getHeight() - offsetY);
                drawable.draw(canvas);
                canvas.restore();

            }

            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                info.setSelected(currentTab != null && selectedTabId != -1 && currentTab.id == selectedTabId);
            }
        }
    }

    public static class UserCell extends FrameLayout {

        BackupImageView avatarImageView;
        BackupImageView reactionImageView;
        TextView nameView;
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        Drawable reactionDrawable = null;


        public UserCell(Context context) {
            super(context);
            avatarImageView = new BackupImageView(context);
            addView(avatarImageView, LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL, 13, 0, 0, 0));
            avatarImageView.setRoundRadius(AndroidUtilities.dp(16));

            reactionImageView = new BackupImageView(context);
            addView(reactionImageView, LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL | Gravity.END, 0, 0, 13, 0));


            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setLines(1);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 59, 0, 59, 0));

            nameView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        }


        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        }

        public void setUser(ReactionsSeenView.UserAndReactions user) {
            if (user != null) {
                if(user.reaction != null) {
                    reactionImageView.setImage(ImageLocation.getForDocument(user.reaction.static_icon), null, new BitmapDrawable(), null);
                }

                avatarDrawable.setInfo(user.user);
                ImageLocation imageLocation = ImageLocation.getForUser(user.user, ImageLocation.TYPE_SMALL);
                avatarImageView.setImage(imageLocation, "50_50", avatarDrawable, user);
                nameView.setText(ContactsController.formatName(user.user.first_name, user.user.last_name));
            }
        }
    }

    public void showOnlyOnePage(boolean showOnlyOnePage) {
        this.showOnlyOnePage = showOnlyOnePage;
    }


}
