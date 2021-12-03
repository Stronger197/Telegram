package org.telegram.messenger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GroupCreateUserCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ChatAttachAlertPhotoLayout;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class MessageReactionsView extends FrameLayout {

    int currentAccount;
    boolean isVoice;
    public RecyclerListView emojiRecycler;
    EmojiAdapter adapter;

    FlickerLoadingView flickerLoadingView;

    public interface OnReactionClicked {
        void reactionClicked(TLRPC.TL_availableReaction reaction, TLRPC.Chat chat, MessageObject messageObject);
    }

    public MessageReactionsView(@NonNull Context context, int currentAccount, MessageObject messageObject, TLRPC.Chat chat, OnReactionClicked listener) {
        super(context);
        this.currentAccount = currentAccount;
        emojiRecycler = new RecyclerListView(context) {
            @Override
            public void onDraw(Canvas c) {
                Path path = new Path();
                RectF rectF = new RectF();
                rectF.set(0f, 0f, getWidth(), getHeight());
                path.addRoundRect(rectF, getHeight(), getHeight(), Path.Direction.CW);

                c.clipPath(path);
                super.onDraw(c);
            }
        };

        adapter = new EmojiAdapter(context);
        adapter.setData(new ArrayList<>());

        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        emojiRecycler.setLayoutManager(layoutManager);
        emojiRecycler.setAdapter(adapter);
        emojiRecycler.setClipToPadding(false);
        emojiRecycler.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        emojiRecycler.setItemAnimator(null);
        emojiRecycler.setLayoutAnimation(null);
        emojiRecycler.setOverScrollMode(RecyclerListView.OVER_SCROLL_IF_CONTENT_SCROLLS);
        emojiRecycler.setOnItemClickListener((view, position) -> {
            TLRPC.TL_availableReaction reaction = adapter.getItem(position);
            // TODO sendReaction
            listener.reactionClicked(reaction, chat, messageObject);
        });

//        flickerLoadingView = new FlickerLoadingView(context);
//        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
//        flickerLoadingView.setViewType(FlickerLoadingView.MESSAGE_SEEN_TYPE);
//        flickerLoadingView.setIsSingleCell(false);
//        addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));
//
        addView(emojiRecycler, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

//        setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(4), AndroidUtilities.dp(4)));
        setEnabled(false);
    }


    public void setData(ArrayList<TLRPC.TL_availableReaction> data) {
        adapter.setData(data);
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        if (flickerLoadingView.getVisibility() == View.VISIBLE) {
//            ignoreLayout = true;
//            flickerLoadingView.setVisibility(View.GONE);
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//            flickerLoadingView.getLayoutParams().width = getMeasuredWidth();
//            flickerLoadingView.setVisibility(View.VISIBLE);
//            ignoreLayout = false;
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        } else {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//    }

    public class EmojiAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;
        private ImageReceiver photoImage;

        public ArrayList<TLRPC.TL_availableReaction> reactions = new ArrayList<>();

        public EmojiAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        public void setData(ArrayList<TLRPC.TL_availableReaction> data) {
            reactions.clear();
            reactions.addAll(data);
            notifyDataSetChanged();
        }

        public TLRPC.TL_availableReaction getItem(int position) {
            return reactions.get(position);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            FrameLayout rootView = new FrameLayout(mContext);
            rootView.setLayoutParams(LayoutHelper.createLinearRelatively(40, LayoutHelper.MATCH_PARENT, Gravity.CENTER | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            BackupImageView imageView = new BackupImageView(mContext);

            rootView.addView(imageView, LayoutHelper.createFrame(38, 38, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

            return new RecyclerListView.Holder(rootView);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FrameLayout cell = (FrameLayout) holder.itemView;

            TLRPC.TL_availableReaction reaction = reactions.get(position);
            ((BackupImageView) cell.getChildAt(0)).setImage(ImageLocation.getForDocument(reaction.select_animation), "40_40", null, 0, this);
        }

        @Override
        public int getItemCount() {
            return reactions.size();
        }

    }
}
