package com.example.pillzilla;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class NotificationAdapter extends ArrayAdapter<MedicationNotification> {

    public interface OnNotificationClickListener {
        void onNotificationClick(MedicationNotification notification);
    }

    OnNotificationClickListener clickListener;

    public NotificationAdapter(Context context, ArrayList<MedicationNotification> notifications,
                                OnNotificationClickListener clickListener) {
        super(context, 0, notifications);
        this.clickListener = clickListener;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);
        }

        MedicationNotification notification = getItem(position);

        TextView tvTitle   = view.findViewById(R.id.tvTitle);
        TextView tvMessage = view.findViewById(R.id.tvMessage);
        TextView tvTime    = view.findViewById(R.id.tvTime);

        tvTitle.setText(notification.getTitle());
        tvMessage.setText(notification.getMessage());
        tvTime.setText(notification.getTime());

        view.setOnClickListener(v -> clickListener.onNotificationClick(notification));

        return view;
    }
}
