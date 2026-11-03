package com.example.pillzilla;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DatabaseReference;

import java.util.ArrayList;

public class MedicationAdapter extends ArrayAdapter<Medication> {

    public interface OnMedicationClickListener {
        void onMedicationClick(Medication medication);
    }

    DatabaseReference databaseReference;
    OnMedicationClickListener clickListener;

    public MedicationAdapter(Context context, ArrayList<Medication> medications, DatabaseReference databaseReference,
                              OnMedicationClickListener clickListener) {
        super(context, 0, medications);
        this.databaseReference = databaseReference;
        this.clickListener = clickListener;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_medication, parent, false);
        }

        Medication medication = getItem(position);

        TextView tvName            = view.findViewById(R.id.tvName);
        TextView tvDosage          = view.findViewById(R.id.tvDosage);
        TextView tvTime            = view.findViewById(R.id.tvTime);
        SwitchMaterial switchTaken = view.findViewById(R.id.switchTaken);

        tvName.setText(medication.getName());
        tvDosage.setText(medication.getDosage() + " · " + medication.getRepeat());
        tvTime.setText(medication.getScheduleTime());

        switchTaken.setOnCheckedChangeListener(null);
        switchTaken.setChecked(medication.isTaken());
        switchTaken.setOnCheckedChangeListener((buttonView, isChecked) -> {
            databaseReference.child(medication.getId()).child("taken").setValue(isChecked);
        });

        view.setOnClickListener(v -> clickListener.onMedicationClick(medication));

        return view;
    }
}
