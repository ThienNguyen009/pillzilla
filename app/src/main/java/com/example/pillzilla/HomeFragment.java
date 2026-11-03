package com.example.pillzilla;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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

public class HomeFragment extends Fragment {

    private static final String[] FILTER_OPTIONS = {"All", "Not Taken", "Taken"};

    ListView listViewMedications;
    FloatingActionButton btnFloatingAction;
    TextInputEditText etSearchMedications;
    CircularProgressIndicator progressMedications;
    View emptyMedications;
    TextView tvEmptyMedicationsTitle;
    TextView tvEmptyMedicationsSubtitle;

    ArrayList<Medication> medicationList = new ArrayList<>();
    ArrayList<Medication> displayedList = new ArrayList<>();
    MedicationAdapter adapter;
    DatabaseReference databaseReference;
    int filterIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewMedications = view.findViewById(R.id.listViewMedications);
        btnFloatingAction = view.findViewById(R.id.btnFloatingAction);
        etSearchMedications = view.findViewById(R.id.etSearchMedications);
        progressMedications = view.findViewById(R.id.progressMedications);
        emptyMedications = view.findViewById(R.id.emptyMedications);
        tvEmptyMedicationsTitle = view.findViewById(R.id.tvEmptyMedicationsTitle);
        tvEmptyMedicationsSubtitle = view.findViewById(R.id.tvEmptyMedicationsSubtitle);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("medications").child(uid);

        adapter = new MedicationAdapter(requireContext(), displayedList, databaseReference, this::showMedicationDialog);
        listViewMedications.setAdapter(adapter);

        loadMedications();

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_home_filter, menu);

                Spinner spinnerFilter = (Spinner) menu.findItem(R.id.action_filter).getActionView();

                ArrayAdapter<String> filterAdapter = new ArrayAdapter<String>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, FILTER_OPTIONS) {
                    @NonNull
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.spinner_icon_toolbar, parent, false);
                        }
                        return convertView;
                    }
                };
                spinnerFilter.setAdapter(filterAdapter);
                spinnerFilter.setSelection(filterIndex);

                spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        filterIndex = position;
                        filterMedications(etSearchMedications.getText().toString());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        btnFloatingAction.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddingMedication.class);
            startActivity(intent);
        });

        etSearchMedications.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMedications(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadMedications() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                medicationList.clear();

                for (DataSnapshot medicationSnapshot : snapshot.getChildren()) {
                    Medication medication = medicationSnapshot.getValue(Medication.class);
                    medicationList.add(medication);
                }

                filterMedications(etSearchMedications.getText().toString());
                progressMedications.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressMedications.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMedications(String query) {
        displayedList.clear();

        for (Medication medication : medicationList) {
            boolean matchesQuery = query.isEmpty()
                    || medication.getName().toLowerCase().contains(query.toLowerCase());

            boolean matchesFilter = filterIndex == 0
                    || (filterIndex == 1 && !medication.isTaken())
                    || (filterIndex == 2 && medication.isTaken());

            if (matchesQuery && matchesFilter) {
                displayedList.add(medication);
            }
        }

        adapter.notifyDataSetChanged();

        if (displayedList.isEmpty()) {
            emptyMedications.setVisibility(View.VISIBLE);

            if (medicationList.isEmpty()) {
                tvEmptyMedicationsTitle.setText("No medications");
                tvEmptyMedicationsSubtitle.setText("Tap + to add your first medication!");
            } else {
                tvEmptyMedicationsTitle.setText("No matching medications");
                tvEmptyMedicationsSubtitle.setText("Try a different search or filter");
            }
        } else {
            emptyMedications.setVisibility(View.GONE);
        }
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
                databaseReference.child(medication.getId()).child("taken").setValue(isChecked));

        btnSaveChanges.setOnClickListener(v -> {
            String newName   = etDialogName.getText().toString();
            String newDosage = etDialogDosage.getText().toString();

            if (newName.equals("") || newDosage.equals("")) {
                Toast.makeText(requireContext(), "Name and dosage cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            databaseReference.child(medication.getId()).child("name").setValue(newName)
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to update name: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            databaseReference.child(medication.getId()).child("dosage").setValue(newDosage)
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to update dosage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            Toast.makeText(requireContext(), "Medication updated", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnDeleteMedication.setOnClickListener(v -> {
            databaseReference.child(medication.getId()).removeValue();
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
