package org.telegram.messenger;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ReactionsPlaceholderDrawable;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Random;


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

        addView(emojiRecycler, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL, 0, 0, 0, 0));

        setEnabled(false);
    }


    public void setData(ArrayList<TLRPC.TL_availableReaction> data) {

        adapter.setData(data);
    }

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

        public void runAnim(BackupImageView imageView, TLRPC.TL_availableReaction reaction) {
            imageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!ViewCompat.isAttachedToWindow(imageView)) {
                        return;
                    }

                    Random r = new Random();
                    int low = 1;
                    int high = 5;
                    int result = r.nextInt(high-low) + low;

                    if(result == 1) {
                        imageView.getImageReceiver().getLottieAnimation().start();
                        imageView.getImageReceiver().getLottieAnimation().setOnFinishCallback(() -> {

                            imageView.getImageReceiver().getLottieAnimation().stop();

                            runAnim(imageView, reaction);
                        }, 118);
                    } else {
                        runAnim(imageView, reaction);
                    }
                }
            }, 500);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FrameLayout cell = (FrameLayout) holder.itemView;

            TLRPC.TL_availableReaction reaction = reactions.get(position);
            BackupImageView imageView =(BackupImageView) cell.getChildAt(0);

            ReactionsPlaceholderDrawable reactionsPlaceholderDrawable = new ReactionsPlaceholderDrawable();
            reactionsPlaceholderDrawable.setBounds(cell.getLeft(), cell.getTop(), cell.getRight(), cell.getBottom());

            imageView.setImage(ImageLocation.getForDocument(reaction.select_animation), "40_40", reactionsPlaceholderDrawable, 0, this);

            ObjectAnimator animator = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0.0f, 0.5f);
            animator.setDuration(1000);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();


            imageView.getImageReceiver().setDelegate(new ImageReceiver.ImageReceiverDelegate() {
                @Override
                public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {

                }

                @Override
                public void onAnimationReady(ImageReceiver imageReceiver) {
                    animator.cancel();

                    imageView.setAlpha(1);

                    imageView.getImageReceiver().getLottieAnimation().setCurrentFrame(0);
                    imageReceiver.getLottieAnimation().stop();
                    runAnim(imageView, reaction);
                }
            });
        }

        @Override
        public int getItemCount() {
            return reactions.size();
        }

    }
}
