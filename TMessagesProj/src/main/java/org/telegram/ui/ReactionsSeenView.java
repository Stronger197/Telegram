package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.AvatarsImageView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.HideViewAfterAnimation;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;


public class ReactionsSeenView extends FrameLayout {

    ArrayList<Long> peerIds = new ArrayList<>();
    public ArrayList<TLRPC.User> users = new ArrayList<>();
    public ArrayList<UserAndReactions> usersAndReactions = new ArrayList<>();
    AvatarsImageView avatarsImageView;
    BackupImageView singleReactionImage;
    TextView titleView;
    ImageView iconView;
    int currentAccount;
    boolean isVoice;
    private boolean usersRequestFinished;
    private boolean reacionsRequestFinished;

    FlickerLoadingView flickerLoadingView;

    public ArrayList<TLRPC.TL_availableReaction> getAllReactions() {

        ArrayList<TLRPC.TL_availableReaction> result = new ArrayList<>();
        ArrayList<String> addedReactions = new ArrayList<>();

        for(UserAndReactions userAndReaction : usersAndReactions) {
            if(!addedReactions.contains(userAndReaction.reaction.reaction)) {
                addedReactions.add(userAndReaction.reaction.reaction);
                result.add(userAndReaction.reaction);
            }
        }

        return result;
    }

    public ArrayList<UserAndReactions> getAllItems() {

        ArrayList<UserAndReactions> result = new ArrayList<>(usersAndReactions);

        for(TLRPC.User user : users) {
            boolean alreadyInList = false;
            for(UserAndReactions reactions : result) {
                if(user.id == reactions.user.id) {
                    alreadyInList = true;
                }
            }

            if(!alreadyInList) {
                UserAndReactions userAndReactions = new UserAndReactions();
                userAndReactions.user = user;
                result.add(userAndReactions);
            }
        }

        return result;
    }
    public ReactionsSeenView(@NonNull Context context, int currentAccount, MessageObject messageObject, TLRPC.Chat chat) {
        super(context);
        this.currentAccount = currentAccount;
        isVoice = (messageObject.isRoundVideo() || messageObject.isVoice());
        flickerLoadingView = new FlickerLoadingView(context);
        flickerLoadingView.setColors(Theme.key_actionBarDefaultSubmenuBackground, Theme.key_listSelector, null);
        flickerLoadingView.setViewType(FlickerLoadingView.MESSAGE_SEEN_TYPE);
        flickerLoadingView.setIsSingleCell(false);
        addView(flickerLoadingView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);

        addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 44, 0, 62, 0));

        avatarsImageView = new AvatarsImageView(context, false);
        avatarsImageView.setStyle(AvatarsImageView.STYLE_MESSAGE_SEEN);
        addView(avatarsImageView, LayoutHelper.createFrame(24 + 12 + 12 + 8, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL, 0, 0, 0, 0));


        singleReactionImage = new BackupImageView(context);
        addView(singleReactionImage, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));
        singleReactionImage.setVisibility(View.GONE);

        titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));

        TLRPC.TL_messages_getMessageReadParticipants req = new TLRPC.TL_messages_getMessageReadParticipants();
        req.msg_id = messageObject.getId();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.getDialogId());

        iconView = new ImageView(context);
        addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.actions_reactions).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
        iconView.setImageDrawable(drawable);

        avatarsImageView.setAlpha(0);
        titleView.setAlpha(0);
        long fromId = 0;
        if (messageObject.messageOwner.from_id != null) {
            fromId = messageObject.messageOwner.from_id.user_id;
        }
        long finalFromId = fromId;

        usersRequestFinished = false;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            FileLog.e("MessageSeenView request completed");

            if (error == null) {
                TLRPC.Vector vector = (TLRPC.Vector) response;
                ArrayList<Long> unknownUsers = new ArrayList<>();
                HashMap<Long, TLRPC.User> usersLocal = new HashMap<>();
                ArrayList<Long> allPeers = new ArrayList<>();
                for (int i = 0, n = vector.objects.size(); i < n; i++) {
                    Object object = vector.objects.get(i);
                    if (object instanceof Long) {
                        Long peerId = (Long) object;
                        if (finalFromId == peerId) {
                            continue;
                        }
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(peerId);
                        allPeers.add(peerId);
                        if (true || user == null) {
                            unknownUsers.add(peerId);
                        } else {
                            usersLocal.put(peerId, user);
                        }
                    }
                }

                if (unknownUsers.isEmpty()) {
                    for (int i = 0; i < allPeers.size(); i++) {
                        peerIds.add(allPeers.get(i));
                        users.add(usersLocal.get(allPeers.get(i)));
                    }
                    usersRequestFinished = true;
                    updateView();
                } else {
                    if (ChatObject.isChannel(chat)) {
                        TLRPC.TL_channels_getParticipants usersReq = new TLRPC.TL_channels_getParticipants();
                        usersReq.limit = 50;
                        usersReq.offset = 0;
                        usersReq.filter = new TLRPC.TL_channelParticipantsRecent();
                        usersReq.channel = MessagesController.getInstance(currentAccount).getInputChannel(chat.id);
                        ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                            if (response1 != null) {
                                TLRPC.TL_channels_channelParticipants users = (TLRPC.TL_channels_channelParticipants) response1;
                                for (int i = 0; i < users.users.size(); i++) {
                                    TLRPC.User user = users.users.get(i);
                                    MessagesController.getInstance(currentAccount).putUser(user, false);
                                    usersLocal.put(user.id, user);
                                }
                                for (int i = 0; i < allPeers.size(); i++) {
                                    peerIds.add(allPeers.get(i));
                                    this.users.add(usersLocal.get(allPeers.get(i)));
                                }
                            }

                            usersRequestFinished = true;
                            updateView();
                        }));
                    } else {
                        TLRPC.TL_messages_getFullChat usersReq = new TLRPC.TL_messages_getFullChat();
                        usersReq.chat_id = chat.id;
                        ConnectionsManager.getInstance(currentAccount).sendRequest(usersReq, (response1, error1) -> AndroidUtilities.runOnUIThread(() -> {
                            if (response1 != null) {
                                TLRPC.TL_messages_chatFull chatFull = (TLRPC.TL_messages_chatFull) response1;
                                for (int i = 0; i < chatFull.users.size(); i++) {
                                    TLRPC.User user = chatFull.users.get(i);
                                    MessagesController.getInstance(currentAccount).putUser(user, false);
                                    usersLocal.put(user.id, user);
                                }
                                for (int i = 0; i < allPeers.size(); i++) {
                                    peerIds.add(allPeers.get(i));
                                    this.users.add(usersLocal.get(allPeers.get(i)));
                                }
                            }
                            usersRequestFinished = true;
                            updateView();
                        }));
                    }
                }
            } else {
                usersRequestFinished = true;
                updateView();
            }
        }));


        reacionsRequestFinished = false;
        TLRPC.TL_messages_getMessageReactionsList messagesRequest = new TLRPC.TL_messages_getMessageReactionsList();
        messagesRequest.id = messageObject.getId();
        messagesRequest.peer = MessagesController.getInstance(currentAccount).getInputPeer(messageObject.getDialogId());
        ConnectionsManager.getInstance(currentAccount).sendRequest(messagesRequest, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            reacionsRequestFinished = true;
            if(error != null) {

            }
            if(response != null) {
                TLRPC.TL_messages_messageReactionsList list = (TLRPC.TL_messages_messageReactionsList) response;

                usersAndReactions.clear();
                for(TLRPC.TL_messageUserReaction reaction : list.reactions) {
                    long userId = reaction.user_id;
                    TLRPC.User user = null;

                    for (TLRPC.User a : list.users) {
                        if(a.id == userId) {
                            user = a;
                        }
                    }

                    if(user != null) {
                        UserAndReactions b = new UserAndReactions();
                        b.reaction = MessagesController.getInstance(currentAccount).findAvailableReaction(reaction.reaction);
                        b.user = user;
                        usersAndReactions.add(b);
                    }
                }
            }
            updateView();

        }));


        setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), AndroidUtilities.dp(4), AndroidUtilities.dp(4)));
        setEnabled(false);
    }

    public static class UserAndReactions {
        public TLRPC.User user;
        public TLRPC.TL_availableReaction reaction;
    }

    boolean ignoreLayout;

    @Override
    public void requestLayout() {
        if (ignoreLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (flickerLoadingView.getVisibility() == View.VISIBLE) {
            ignoreLayout = true;
            flickerLoadingView.setVisibility(View.GONE);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            flickerLoadingView.getLayoutParams().width = getMeasuredWidth();
            flickerLoadingView.setVisibility(View.VISIBLE);
            ignoreLayout = false;
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private void updateView() {
        singleReactionImage.setVisibility(View.GONE);
        avatarsImageView.setVisibility(View.VISIBLE);
        iconView.setVisibility(View.VISIBLE);
        if(!usersRequestFinished || !reacionsRequestFinished) {
            return;
        }


        setEnabled(users.size() > 0 || usersAndReactions.size() > 0);


        if(usersAndReactions.size() == 0) {
            for (int i = 0; i < 3; i++) {
                if (i < users.size()) {
                    avatarsImageView.setObject(i, currentAccount, users.get(i));
                } else {
                    avatarsImageView.setObject(i, currentAccount, null);
                }
            }
            if (users.size() == 1) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(24));
            } else if (users.size() == 2) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(12));
            } else {
                avatarsImageView.setTranslationX(0);
            }

            avatarsImageView.commitTransition(false);
            if (peerIds.size() == 1 && users.get(0) != null) {
                titleView.setText(ContactsController.formatName(users.get(0).first_name, users.get(0).last_name));
            } else {
                titleView.setText(LocaleController.formatPluralString(isVoice ? "MessagePlayed" : "MessageSeen", peerIds.size()));
            }

            Drawable drawable = ContextCompat.getDrawable(getContext(), isVoice ? R.drawable.actions_reactions : R.drawable.msg_seen).mutate();
            drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
            iconView.setImageDrawable(drawable);

            titleView.animate().alpha(1f).setDuration(220).start();
            avatarsImageView.animate().alpha(1f).setDuration(220).start();
            flickerLoadingView.animate().alpha(0f).setDuration(220).setListener(new HideViewAfterAnimation(flickerLoadingView)).start();
        } else {
            // TODO show reactions info

            for (int i = 0; i < 3; i++) {
                if (i < usersAndReactions.size()) {
                    avatarsImageView.setObject(i, currentAccount, usersAndReactions.get(i).user);
                } else {
                    avatarsImageView.setObject(i, currentAccount, null);
                }
            }

            if (usersAndReactions.size() == 1) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(24));
            } else if (usersAndReactions.size() == 2) {
                avatarsImageView.setTranslationX(AndroidUtilities.dp(12));
            } else {
                avatarsImageView.setTranslationX(0);
            }

            avatarsImageView.commitTransition(false);

            if (usersAndReactions.size() == 1) {


                titleView.setText(ContactsController.formatName(usersAndReactions.get(0).user.first_name, usersAndReactions.get(0).user.last_name));


                if(usersAndReactions.get(0).reaction != null) {
                    singleReactionImage.setImage(ImageLocation.getForDocument(usersAndReactions.get(0).reaction.static_icon), "50_50", new BitmapDrawable(), null);
                    singleReactionImage.setVisibility(View.VISIBLE);
                }

                iconView.setVisibility(View.GONE);

            } else {
                titleView.setText(usersAndReactions.size() + "/" + getAllItems().size() + " Reacted");
            }

            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.actions_reactions).mutate();
            drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon), PorterDuff.Mode.MULTIPLY));
            iconView.setImageDrawable(drawable);

            titleView.animate().alpha(1f).setDuration(220).start();
            avatarsImageView.animate().alpha(1f).setDuration(220).start();
            flickerLoadingView.animate().alpha(0f).setDuration(220).setListener(new HideViewAfterAnimation(flickerLoadingView)).start();
        }
    }

    public RecyclerListView createListView() {
        RecyclerListView recyclerListView = new RecyclerListView(getContext());
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

    private static class UserCell extends FrameLayout {

        BackupImageView avatarImageView;
        TextView nameView;
        AvatarDrawable avatarDrawable = new AvatarDrawable();

        public UserCell(Context context) {
            super(context);
            avatarImageView = new BackupImageView(context);
            addView(avatarImageView, LayoutHelper.createFrame(32, 32, Gravity.CENTER_VERTICAL, 13, 0, 0, 0));
            avatarImageView.setRoundRadius(AndroidUtilities.dp(16));
            nameView = new TextView(context);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            nameView.setLines(1);
            nameView.setEllipsize(TextUtils.TruncateAt.END);
            addView(nameView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 59, 0, 13, 0));

            nameView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(44), View.MeasureSpec.EXACTLY));
        }

        public void setUser(TLRPC.User user) {
            if (user != null) {
                avatarDrawable.setInfo(user);
                ImageLocation imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL);
                avatarImageView.setImage(imageLocation, "50_50", avatarDrawable, user);
                nameView.setText(ContactsController.formatName(user.first_name, user.last_name));
            }
        }
    }
}
