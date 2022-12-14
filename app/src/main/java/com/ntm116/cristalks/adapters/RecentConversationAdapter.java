package com.ntm116.cristalks.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ntm116.cristalks.databinding.ItemContainerRecentConversationBinding;
import com.ntm116.cristalks.listeners.ConversationListener;
import com.ntm116.cristalks.models.ChatMessage;
import com.ntm116.cristalks.models.User;
import com.ntm116.cristalks.utils.BitmapUtil;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder> {
    private final List<ChatMessage> chatMessages;
    private final ConversationListener conversationListener;

    public RecentConversationAdapter(List<ChatMessage> chatMessages, ConversationListener conversationListener) {
        this.chatMessages = chatMessages;
        this.conversationListener = conversationListener;
    }

    @NonNull
    @Override
    public RecentConversationAdapter.ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversationViewHolder(
                ItemContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull RecentConversationAdapter.ConversationViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversationViewHolder extends RecyclerView.ViewHolder {
        ItemContainerRecentConversationBinding binding;

        ConversationViewHolder(ItemContainerRecentConversationBinding _binding)
        {
            super(_binding.getRoot());
            binding = _binding;
        }

        void setData(ChatMessage chatMessage)
        {
            binding.textName.setText(chatMessage.conversationName);
            binding.imageProfile.setImageBitmap(BitmapUtil.stringToBitmap(chatMessage.conversationImage));
            binding.textRecentMessage.setText(chatMessage.message);

            binding.getRoot().setOnClickListener(view -> {
                User user = new User();
                user.id = chatMessage.conversationId;
                user.name = chatMessage.conversationName;
                user.image = chatMessage.conversationImage;

                conversationListener.OnConversationClicked(user);
            });
        }
    }

}
