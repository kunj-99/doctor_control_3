package com.infowave.doctor_control.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.R;

public class homeadapter extends RecyclerView.Adapter<homeadapter.ViewHolder> {

    private String[] activityTitles;
    private String[] activityTimes;

    // Constructor receives two arrays: one for titles and one for times
    public homeadapter(String[] activityTitles, String[] activityTimes) {
        this.activityTitles = activityTitles;
        this.activityTimes = activityTimes;
    }

    @NonNull
    @Override
    public homeadapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull homeadapter.ViewHolder holder, int position) {
        holder.activityTitle.setText(activityTitles[position]);
        holder.activityTime.setText(activityTimes[position]);
    }

    @Override
    public int getItemCount() {
        return activityTitles.length;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView activityIcon;
        public TextView activityTitle;
        public TextView activityTime;

        public ViewHolder(View itemView) {
            super(itemView);
            activityIcon = itemView.findViewById(R.id.activity_icon);
            activityTitle = itemView.findViewById(R.id.activity_title);
            activityTime = itemView.findViewById(R.id.activity_time);
        }
    }
}
