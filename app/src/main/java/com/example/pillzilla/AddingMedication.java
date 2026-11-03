package com.example.pillzilla;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class AddingMedication extends AppCompatActivity {

    TextInputEditText etMedicationName;
    TextInputEditText etDosage;
    TextInputEditText etScheduleTime;
    TextInputEditText etDate;
    TextInputEditText etNotes;
    AutoCompleteTextView spinnerRepeat;
    Button btnSaveMedication;
    Calendar calendar;
    DatabaseReference databaseReference;
    MaterialToolbar materialToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_adding_medication);

        materialToolbar = findViewById(R.id.toolbar);

        materialToolbar.setNavigationIcon(R.drawable.outline_arrow_back_ios_24);
        materialToolbar.setNavigationOnClickListener(v -> finish());
        materialToolbar.setTitle("Add Medications");

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            materialToolbar.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);



        calendar = Calendar.getInstance();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference("medications").child(uid);

        etMedicationName  = findViewById(R.id.etMedicationName);
        etDosage          = findViewById(R.id.etDosage);
        etScheduleTime    = findViewById(R.id.etScheduleTime);
        etDate            = findViewById(R.id.etDate);
        etNotes           = findViewById(R.id.etNotes);
        spinnerRepeat     = findViewById(R.id.spinnerRepeat);
        btnSaveMedication = findViewById(R.id.btnSaveMedication);

        String[] repeatOptions = {"Daily", "Weekly", "Monthly", "Once"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, repeatOptions);
        spinnerRepeat.setAdapter(adapter);

        etDate.setOnClickListener(v -> showDatePicker());
        etScheduleTime.setOnClickListener(v -> showTimePicker());
        btnSaveMedication.setOnClickListener(v -> saveMedication());
    }

    public void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String selectedDate = sdf.format(new Date(selection));
            etDate.setText(selectedDate);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    public void showTimePicker() {
        int hour   = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, h, m) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            etScheduleTime.setText(time);
        }, hour, minute, true);

        timePickerDialog.show();
    }

    public void saveMedication() {
        String name   = etMedicationName.getText().toString();
        String dosage = etDosage.getText().toString();
        String time   = etScheduleTime.getText().toString();
        String date   = etDate.getText().toString();
        String repeat = spinnerRepeat.getText().toString();
        String notes  = etNotes.getText().toString();

        if (name.equals("")) {
            Toast.makeText(this, "Please enter medication name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dosage.equals("")) {
            Toast.makeText(this, "Please enter dosage", Toast.LENGTH_SHORT).show();
            return;
        }

        if (time.equals("")) {
            Toast.makeText(this, "Please enter time", Toast.LENGTH_SHORT).show();
            return;
        }

        if (date.equals("")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        if (repeat.equals("")) {
            Toast.makeText(this, "Please select repeat option", Toast.LENGTH_SHORT).show();
            return;
        }

        String medicationId = databaseReference.push().getKey();

        Medication medication = new Medication(name, dosage, time, date, repeat, notes);
        medication.setId(medicationId);

        databaseReference.child(medicationId).setValue(medication);

        scheduleReminder(medicationId, name, dosage, date, time, repeat);

        Toast.makeText(this, "Medication Saved!", Toast.LENGTH_SHORT).show();

        finish();
    }

    private void scheduleReminder(String medicationId, String name, String dosage, String date, String time, String repeat) {
        Date scheduledDateTime;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy H:m", Locale.getDefault());
            scheduledDateTime = sdf.parse(date + " " + time);
        } catch (ParseException e) {
            Toast.makeText(this, "Could not schedule reminder", Toast.LENGTH_SHORT).show();
            return;
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Please allow exact alarms to get medication reminders", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            return;
        }

        Intent reminderIntent = new Intent(this, MedicationReminderReceiver.class);
        reminderIntent.putExtra("medicationId", medicationId);
        reminderIntent.putExtra("medicationName", name);
        reminderIntent.putExtra("medicationDosage", dosage);
        reminderIntent.putExtra("repeat", repeat);

        int notificationId = medicationId.hashCode();
        reminderIntent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                scheduledDateTime.getTime(),
                pendingIntent
        );
    }
}