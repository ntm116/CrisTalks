package com.ntm116.cristalks.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.ntm116.cristalks.adapters.ChatAdapter;
import com.ntm116.cristalks.databinding.ActivityChatBinding;
import com.ntm116.cristalks.models.ChatMessage;
import com.ntm116.cristalks.models.User;
import com.ntm116.cristalks.utils.Constants;
import com.ntm116.cristalks.utils.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        init();
        setListeners();
        listenMessages();
    }

    private void init()
    {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                stringToBitmap(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.recyclerView.setAdapter(chatAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void listenMessages()
    {
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void sendMessage()
    {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());

        db.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        binding.inputMessage.setText(null);
    }

    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
       if (error != null)
           return;
       if (value != null)
       {
           int count = chatMessages.size();
           for (DocumentChange documentChange : value.getDocumentChanges())
           {
               ChatMessage chatMessage = new ChatMessage();
               chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
               chatMessage.receivedId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
               chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
               chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
               chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
               chatMessages.add(chatMessage);
           }
           Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject) );
           if (count == 0)
           {
               chatAdapter.notifyDataSetChanged();
           }
           else {
               chatAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
               binding.recyclerView.smoothScrollToPosition(chatMessages.size() - 1);
           }
           binding.recyclerView.setVisibility(View.VISIBLE);

       }
       binding.progressBar.setVisibility(View.GONE);
    });

    private Bitmap stringToBitmap(String encodedString)
    {
        byte[] bytes = Base64.decode(encodedString, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void loadReceiverDetails()
    {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        Bitmap bitmap = stringToBitmap(receiverUser.image);
        binding.textUserName.setText(receiverUser.name);
        binding.imageUser.setImageBitmap(bitmap);
    }

    private void setListeners()
    {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.layoutSend.setOnClickListener(view -> sendMessage());
    }

    private String getReadableDateTime(Date date)
    {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm:ss", Locale.getDefault()).format(date);
    }
}