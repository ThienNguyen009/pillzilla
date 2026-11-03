package com.example.pillzilla;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class NotificationsFragment extends Fragment {

    ListView listViewNotifications;
    Button btnClearAll;
    CircularProgressIndicator progressNotifications;
    View emptyNotifications;

    ArrayList<MedicationNotification> notificationList = new ArrayList<>();
    NotificationAdapter adapter;
    DatabaseReference databaseReference;
    DatabaseReference medicationsRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewNotifications = view.findViewById(R.id.listViewNotifications);
        btnClearAll = view.findViewById(R.id.btnClearAll);
        progressNotifications = view.findViewById(R.id.progressNotifications);
        emptyNotifications = view.findViewById(R.id.emptyNotifications);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("notifications").child(uid);
        medicationsRef = FirebaseDatabase.getInstance().getReference("medications").child(uid);

        adapter = new NotificationAdapter(requireContext(), notificationList, this::showMedicationForNotification);
        listViewNotifications.setAdapter(adapter);

        loadNotifications();

        btnClearAll.setOnClickListener(v -> confirmClearAll());
    }

    private void loadNotifications() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                notificationList.clear();

                for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                    MedicationNotification notification = notificationSnapshot.getValue(MedicationNotification.class);
                    notificationList.add(notification);
                }

                adapter.notifyDataSetChanged();
                progressNotifications.setVisibility(View.GONE);
                emptyNotifications.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressNotifications.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmClearAll() {
        if (notificationList.isEmpty()) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear All Notifications")
                .setMessage("This will remove all notifications. This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> databaseReference.removeValue()
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showMedicationForNotification(MedicationNotification notification) {
        String medicationId = notification.getMedicationId();

        if (medicationId == null) {
            Toast.makeText(requireContext(), "This notification isn't linked to a medication", Toast.LENGTH_SHORT).show();
            return;
        }

        medicationsRef.child(medicationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Medication medication = snapshot.getValue(Medication.class);

                if (medication == null) {
                    Toast.makeText(requireContext(), "This medication no longer exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                showMedicationDialog(medication);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(requireContext(), "Failed to load medication", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMedicationDialog(Medication medication) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_medication_details, null);

        TextInputEditText etDialogName    = dialogView.findViewById(R.id.etDialogName);
        TextInputEditText etDialogDosage  = dialogView.findViewById(R.id.etDialogDosage);
        SwitchMaterial switchDialogTaken  = dialogView.findViewById(R.id.switchDialogTaken);
        Button btnSaveChanges             = dialogView.findViewById(R.id.btnSaveChanges);
        Button btnDeleteMedication        = dialogView.findViewById(R.id.btnDeleteMedication);

        etDialogName.setText(medication.getName());
        etDialogDosage.setText(medication.getDosage());
        switchDialogTaken.setChecked(medication.isTaken());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Medication Details")
                .setView(dialogView)
                .setNegativeButton("Close", null)
                .create();

        switchDialogTaken.setOnCheckedChangeListener((buttonView, isChecked) ->
                medicationsRef.child(medication.getId()).child("taken").setValue(isChecked));

        btnSaveChanges.setOnClickListener(v -> {
            String newName   = etDialogName.getText().toString();
            String newDosage = etDialogDosage.getText().toString();

            if (newName.equals("") || newDosage.equals("")) {
                Toast.makeText(requireContext(), "Name and dosage cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            medicationsRef.child(medication.getId()).child("name").setValue(newName);
            medicationsRef.child(medication.getId()).child("dosage").setValue(newDosage);
            Toast.makeText(requireContext(), "Medication updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDeleteMedication.setOnClickListener(v -> {
            medicationsRef.child(medication.getId()).removeValue();
            cancelReminder(medication.getId());
            Toast.makeText(requireContext(), "Medication deleted", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void cancelReminder(String medicationId) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        Intent reminderIntent = new Intent(requireContext(), MedicationReminderReceiver.class);
        int notificationId = medicationId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                notificationId,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }
}
