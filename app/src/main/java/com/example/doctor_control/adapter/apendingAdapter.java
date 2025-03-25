package com.example.doctor_control.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctor_control.R;

import java.util.ArrayList;

public class apendingAdapter extends RecyclerView.Adapter<apendingAdapter.ViewHolder> {

    private ArrayList<String> names;
    private ArrayList<String> problems;
    private ArrayList<String> distances;

    public apendingAdapter(ArrayList<String> names, ArrayList<String> problems, ArrayList<String> distances) {
        this.names = names;
        this.problems = problems;
        this.distances = distances;
    }

    @NonNull
    @Override
    public apendingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull apendingAdapter.ViewHolder holder, int position) {
        holder.tvPatientName.setText(names.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText(distances.get(position));

        // Button actions (optional)
        holder.btnCanform.setOnClickListener(v -> {
            // handle confirm click
        });

        holder.btnTrack.setOnClickListener(v -> {
            // handle track click
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvPatientName, tvProblem, tvDistance;
        Button btnCanform, btnTrack;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvDistance = itemView.findViewById(R.id.tv_distans);
            btnCanform = itemView.findViewById(R.id.btn_canform);
            btnTrack = itemView.findViewById(R.id.btn_track);
        }
    }
}
