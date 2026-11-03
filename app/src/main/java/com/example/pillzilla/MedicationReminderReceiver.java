package com.example.pillzilla;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MedicationReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String medicationId = intent.getStringExtra("medicationId");
        String medicationName = intent.getStringExtra("medicationName");
        String medicationDosage = intent.getStringExtra("medicationDosage");
        String repeat = intent.getStringExtra("repeat");
        int notificationId = intent.getIntExtra("notificationId", 0);

        String title = "Time to take " + medicationName;

        Intent openAppIntent = new Intent(context, HomeActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.outline_notifications_24)
                .setContentTitle(title)
                .setContentText(medicationDosage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        }

        saveNotificationToFirebase(title, medicationDosage, medicationId);
        resetTakenStatus(medicationId);
        rescheduleIfRepeating(context, medicationId, medicationName, medicationDosage, repeat, notificationId);
    }

    private void saveNotificationToFirebase(String title, String message, String medicationId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        DatabaseReference notificationsRef = FirebaseDatabase.getInstance()
                .getReference("notifications")
                .child(user.getUid());

        String notificationDbId = notificationsRef.push().getKey();

        String time = new SimpleDateFormat("dd/MM/yyyy H:mm", Locale.getDefault()).format(new Date());

        MedicationNotification notification = new MedicationNotification(title, message, time, medicationId);
        notification.setId(notificationDbId);

        notificationsRef.child(notificationDbId).setValue(notification)
                .addOnSuccessListener(unused -> Log.d("MedicationReminder", "Notification saved: " + notificationDbId))
                .addOnFailureListener(e -> Log.e("MedicationReminder", "Failed to save notification", e));
    }

    private void resetTakenStatus(String medicationId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || medicationId == null) {
            return;
        }

        FirebaseDatabase.getInstance()
                .getReference("medications")
                .child(user.getUid())
                .child(medicationId)
                .child("taken")
                .setValue(false);
    }

    private void rescheduleIfRepeating(Context context, String medicationId, String medicationName,
                                        String medicationDosage, String repeat, int notificationId) {
        if (medicationId == null || repeat == null || repeat.equals("Once")) {
            return;
        }

        Calendar calendar = Calendar.getInstance();

        switch (repeat) {
            case "Daily":
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                break;
            case "Weekly":
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                break;
            case "Monthly":
                calendar.add(Calendar.MONTH, 1);
                break;
            default:

                return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return;
        }

        Intent reminderIntent = new Intent(context, MedicationReminderReceiver.class);
        reminderIntent.putExtra("medicationId", medicationId);
        reminderIntent.putExtra("medicationName", medicationName);
        reminderIntent.putExtra("medicationDosage", medicationDosage);
        reminderIntent.putExtra("repeat", repeat);
        reminderIntent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }
}
