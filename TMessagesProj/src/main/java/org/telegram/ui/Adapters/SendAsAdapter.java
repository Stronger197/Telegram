package org.telegram.ui.Adapters;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GroupCreateUserCell;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SendAsAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private MessagesController messagesController;
    private final Theme.ResourcesProvider resourcesProvider;
    public List<TLRPC.Peer> peers = new LinkedList<>();
    private TLRPC.Peer current;

    private OnClicked onClicked;

    public interface OnClicked {
        void onChatClicked(TLRPC.Chat chat);
    }

    public SendAsAdapter(Context context, Theme.ResourcesProvider resourcesProvider, MessagesController messagesController, OnClicked onClicked) {
        mContext = context;
        this.resourcesProvider = resourcesProvider;
        this.onClicked = onClicked;
        this.messagesController = messagesController;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return false;
    }

    public void setData(ArrayList<TLRPC.Peer> peers, TLRPC.Peer current) {

        ArrayList<TLRPC.Peer> filteredPeers = new ArrayList<>();
        for(int i = 0; i < peers.size(); i++) {
            TLRPC.Peer peer = peers.get(i);

            if(peer instanceof TLRPC.TL_peerChannel) {
                TLRPC.Chat chat = messagesController.getChat(peer.channel_id);
                if(chat != null) {
                    filteredPeers.add(peer);
                }
            } else if(peer instanceof TLRPC.TL_peerChat) {
                TLRPC.Chat chat = messagesController.getChat(-peer.chat_id);
                if(chat != null) {
                    filteredPeers.add(peer);
                }
            } else if(peer instanceof TLRPC.TL_peerUser) {
                TLRPC.User user = messagesController.getUser(peer.user_id);
                if(user != null) {
                    filteredPeers.add(peer);
                }
            }
        }

        if(filteredPeers.size() > 10) {
            this.peers = filteredPeers.subList(0, 10);
        } else {
            this.peers = filteredPeers;
        }
        this.current = current;
        notifyDataSetChanged();
    }

    public TLRPC.Peer getItem(int position) {
        return peers.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerListView.Holder(new GroupCreateUserCell(mContext, 2, 0, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        GroupCreateUserCell cell = (GroupCreateUserCell) holder.itemView;
        TLRPC.Peer peer = peers.get(position);

        TLObject chat;
        TLRPC.ChatFull chatFull = null;
        boolean isCurrent = false;
        if(peer instanceof TLRPC.TL_peerChannel) {
            chat = messagesController.getChat(peer.channel_id);
            chatFull = messagesController.getChatFull(peer.channel_id);
            if(current instanceof TLRPC.TL_peerUser) {
                isCurrent = peer.channel_id == current.user_id;
            } else {
                isCurrent = peer.channel_id == current.channel_id;
            }
        } else if(peer instanceof TLRPC.TL_peerChat) {
            chat = messagesController.getChat(-peer.chat_id);
            chatFull = messagesController.getChatFull(-peer.chat_id);
            if(current instanceof TLRPC.TL_peerUser) {
                isCurrent = peer.chat_id == current.user_id;
            } else {
                isCurrent = peer.chat_id == current.chat_id;
            }
        } else if(peer instanceof TLRPC.TL_peerUser) {
            chat = messagesController.getUser(peer.user_id);
            isCurrent = peer.user_id == current.user_id;
        } else {
            return;
        }

        String title = "";
        if(chat instanceof TLRPC.Chat) {
            title = ((TLRPC.Chat) chat).title;
        } else if(chat instanceof TLRPC.User) {
            title = ((TLRPC.User) chat).first_name + " " + ((TLRPC.User) chat).last_name;
        }

        String subtitle = "";
        if(chatFull != null) {
            if (((TLRPC.Chat) chat).megagroup) {
                subtitle = LocaleController.formatPluralString("Members", chatFull.participants_count);
            } else {
                subtitle = LocaleController.formatPluralString("Subscribers", chatFull.participants_count);
            }
        } else {
            subtitle = LocaleController.getString("ChatSendAsPersonalAccount", R.string.ChatSendAsPersonalAccount);
        }

        cell.setObject(chat, title, subtitle);
        cell.setChecked(isCurrent, false);
    }

    @Override
    public int getItemCount() {
        return this.peers.size();
    }

}
