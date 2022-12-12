package com.ntm116.cristalks.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.ntm116.cristalks.adapters.UsersAdapter;
import com.ntm116.cristalks.databinding.ActivityUsersBinding;
import com.ntm116.cristalks.listeners.UserListener;
import com.ntm116.cristalks.models.User;
import com.ntm116.cristalks.utils.Constants;
import com.ntm116.cristalks.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity implements UserListener {
    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
    }

    private void setListeners()
    {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
    }

    private void getUsers()
    {
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null)
                    {
                        List<User> userList = new ArrayList<>();
                        User user = new User();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult())
                        {
                            if (currentUserId.equals(queryDocumentSnapshot.getId()))
                            {
                                continue;
                            }
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);

                            userList.add(user);
                        }

                        if (userList.size() > 0)
                        {
                            UsersAdapter usersAdapter = new UsersAdapter(userList, this);
                            binding.recyclerViewUsers.setAdapter(usersAdapter);
                            binding.recyclerViewUsers.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage("Unable to load user");
                        }
                    } else {
                        showErrorMessage("Unable to load user");
                    }
                });
    }

    private void showErrorMessage(String e)
    {
        binding.textError.setText(String.format("%s", e));
        binding.textError.setVisibility(View.VISIBLE);
    }

    private void loading(boolean isLoading)
    {
        if (isLoading)
        {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void OnUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }
}