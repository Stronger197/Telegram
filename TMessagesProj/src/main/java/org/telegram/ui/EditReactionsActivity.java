package org.telegram.ui;


import android.content.Context;
import android.graphics.Canvas;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class EditReactionsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private TLRPC.ChatFull info;
    private RecyclerListView reactionsList;
    private ListAdapter listViewAdapter;
    private  TextCheckCell cell;

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }

    public void setInfo(TLRPC.ChatFull chatFull) {
        info = chatFull;
    }

    @Override
    public View createView(Context context) {

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("ChannelReactions", R.string.ChannelReactions));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        View rootView = LayoutInflater.from(context).inflate(R.layout.activity_edit_reactions, parentLayout, false);
        rootView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        reactionsList = new RecyclerListView(context);
        reactionsList.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listViewAdapter = new ListAdapter(context);


        if(getMessagesController().availableReactions != null) {
            listViewAdapter.setData(getMessagesController().availableReactions, info.available_reactions);
        }

        reactionsList.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ReactionCheckCell reactionCheckCell = ((ReactionCheckCell) view);
                reactionCheckCell.setChecked(!reactionCheckCell.isChecked());

                String reaction = reactionCheckCell.reaction;
                if(reactionCheckCell.isChecked()) {
                    listViewAdapter.checkedReactions.add(reaction);
                } else {
                    listViewAdapter.checkedReactions.remove(reaction);
                }
            }
        });
        reactionsList.setAdapter(listViewAdapter);

        cell = new TextCheckCell(context);
        cell.setColors(Theme.key_windowBackgroundCheckText, Theme.key_switchTrackBlue, Theme.key_switchTrackBlueChecked, Theme.key_switchTrackBlueThumb, Theme.key_switchTrackBlueThumbChecked);
        cell.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        cell.setHeight(56);
        cell.setDrawCheckRipple(true);
        boolean isChecked = info.available_reactions.size() > 0;
        cell.setTextAndCheck(LocaleController.getString("EnableReactions", R.string.EnableReactions), isChecked, false);
        cell.setBackgroundColor(Theme.getColor(isChecked ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));

        if(!cell.isChecked()) {
            reactionsList.setAlpha(0);
        }
        cell.setOnClickListener(v -> {

            boolean newValue = !cell.isChecked();
            cell.setBackgroundColorAnimated(newValue, Theme.getColor(newValue ? Theme.key_windowBackgroundChecked : Theme.key_windowBackgroundUnchecked));
            cell.setChecked(newValue);

            if(newValue) {
                reactionsList.animate().alphaBy(0f).alpha(1f).setDuration(100).start();
            } else {
                reactionsList.animate().alphaBy(1f).alpha(0f).setDuration(100).start();
            }
        });
        ((ViewGroup) rootView).addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));


        TextInfoPrivacyCell textInfoPrivacyCell = new TextInfoPrivacyCell(context);
        textInfoPrivacyCell.setText("Allow subscribers to react to channel posts.");
        ((ViewGroup) rootView).addView(textInfoPrivacyCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        ((ViewGroup) rootView).addView(reactionsList, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


        frameLayout.addView(rootView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));


        return fragmentView;
    }

    @Override
    public boolean onBackPressed() {
        finishFragment();
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();


        getMessagesController().setAvailableReactions(cell.isChecked() ? listViewAdapter.checkedReactions : new ArrayList<>(), info.id);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;
        private ArrayList<TLRPC.TL_availableReaction> data = new ArrayList<>();
        private ArrayList<String> inCurrentChat = new ArrayList<>();
        private ArrayList<String> checkedReactions = new ArrayList<>();

        public ListAdapter(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new ReactionCheckCell(mContext));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {


            TLRPC.TL_availableReaction reaction = data.get(position);

            boolean isChecked = inCurrentChat.contains(reaction.reaction);
            ((ReactionCheckCell) holder.itemView).setTextAndCheck(reaction.title, isChecked, true, reaction.static_icon, reaction.reaction);
            if(isChecked) {
                checkedReactions.add(reaction.reaction);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        public void setData(ArrayList<TLRPC.TL_availableReaction> availableReactions, ArrayList<String> inCurrentChat) {
            data.clear();
            data.addAll(availableReactions);
            this.inCurrentChat.clear();
            this.inCurrentChat.addAll(inCurrentChat);
            notifyDataSetChanged();
        }
    }
}