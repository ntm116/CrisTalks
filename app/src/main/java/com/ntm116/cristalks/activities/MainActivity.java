package com.ntm116.cristalks.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ntm116.cristalks.R;
import com.ntm116.cristalks.adapters.RecentConversationAdapter;
import com.ntm116.cristalks.databinding.ActivityMainBinding;
import com.ntm116.cristalks.listeners.ConversationListener;
import com.ntm116.cristalks.models.ChatMessage;
import com.ntm116.cristalks.models.User;
import com.ntm116.cristalks.utils.Constants;
import com.ntm116.cristalks.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ConversationListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter recentConversationAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setContentView(binding.getRoot());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
    }

    private void init()
    {
        conversations = new ArrayList<>();
        recentConversationAdapter = new RecentConversationAdapter(conversations, this);
        binding.recyclerViewRecentConversations.setAdapter(recentConversationAdapter);
        db = FirebaseFirestore.getInstance();
    }

    private void setListeners()
    {
        binding.imageSignOut.setOnClickListener(view -> signOut());
        binding.fabNewChat.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), UsersActivity.class));
        });
    }

    private void loadUserDetails()
    {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void getToken()
    {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token)
    {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }

    private void signOut()
    {
        showToast("Signing out...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                db.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Unable to sign out");
                });
    }

    private void listenConversations()
    {
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null)
            return;
        if (value != null)
        {
            for (DocumentChange documentChange : value.getDocumentChanges())
            {
                if (documentChange.getType() == DocumentChange.Type.ADDED)
                {
                    QueryDocumentSnapshot snapshot = documentChange.getDocument();
                    String senderId = snapshot.getString(Constants.KEY_SENDER_ID);
                    String receiverId = snapshot.getString(Constants.KEY_RECEIVER_ID);

                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = senderId;
                    chatMessage.receivedId = receiverId;
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId))
                    {
                        chatMessage.conversationImage = snapshot.getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversationName = snapshot.getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversationId = snapshot.getString(Constants.KEY_RECEIVER_ID);
                    } else {
                        chatMessage.conversationImage = snapshot.getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversationName = snapshot.getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversationId = snapshot.getString(Constants.KEY_SENDER_ID);
                    }

                    chatMessage.message = snapshot.getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = snapshot.getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    QueryDocumentSnapshot snapshot = documentChange.getDocument();
                    String senderId = snapshot.getString(Constants.KEY_SENDER_ID);
                    String receiverId = snapshot.getString(Constants.KEY_RECEIVER_ID);

                    for (int i = 0; i < conversations.size(); i++) {
                        ChatMessage conversation = conversations.get(i);
                        if (conversation.senderId.equals(senderId) && conversation.receivedId.equals(receiverId))
                        {
                            conversation.message = snapshot.getString(Constants.KEY_LAST_MESSAGE);
                            conversation.dateObject = snapshot.getDate(Constants.KEY_TIMESTAMP);
                            Log.d("WWWW", "" + conversation.message.equals(conversations.get(i).message));
                            break;
                        }
                    }
                }
            }

            Collections.sort(conversations, (obj1, obj2) -> obj2.dateObject.compareTo(obj1.dateObject));
            recentConversationAdapter.notifyDataSetChanged();
            binding.recyclerViewRecentConversations.smoothScrollToPosition(0);
            binding.recyclerViewRecentConversations.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    @Override
    public void OnConversationClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}