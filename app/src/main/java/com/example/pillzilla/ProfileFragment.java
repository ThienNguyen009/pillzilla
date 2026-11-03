package com.example.pillzilla;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    TextView tvProfileName;
    TextView tvProfileEmail;
    Button btnEditUsername;
    Button btnLogout;
    Button btnDeleteAccount;

    DatabaseReference userRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvProfileName    = view.findViewById(R.id.tvProfileName);
        tvProfileEmail   = view.findViewById(R.id.tvProfileEmail);
        btnEditUsername  = view.findViewById(R.id.btnEditUsername);
        btnLogout        = view.findViewById(R.id.btnLogout);
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        tvProfileEmail.setText(user.getEmail());

        userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        loadProfile();

        btnEditUsername.setOnClickListener(v -> showEditUsernameDialog());
        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());
    }

    private void loadProfile() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User profileUser = snapshot.getValue(User.class);
                if (profileUser != null) {
                    tvProfileName.setText(profileUser.name);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditUsernameDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_username, null);
        TextInputEditText etDialogUsername = dialogView.findViewById(R.id.etDialogUsername);
        etDialogUsername.setText(tvProfileName.getText().toString());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Username")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etDialogUsername.getText().toString();

                    if (newName.equals("")) {
                        Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    userRef.child("name").setValue(newName);
                    Toast.makeText(requireContext(), "Username updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        goToLoginScreen();
    }

    private void confirmDeleteAccount() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all your data. This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = user.getUid();

        FirebaseDatabase.getInstance().getReference("users").child(uid).removeValue();
        FirebaseDatabase.getInstance().getReference("medications").child(uid).removeValue();
        FirebaseDatabase.getInstance().getReference("notifications").child(uid).removeValue();

        user.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                goToLoginScreen();
            } else {
                Toast.makeText(requireContext(), "Could not delete account, please log in again and retry", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToLoginScreen() {
        Intent intent = new Intent(requireContext(), LoginScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
