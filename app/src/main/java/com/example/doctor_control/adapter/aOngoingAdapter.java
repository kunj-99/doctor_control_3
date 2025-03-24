package com.example.doctor_control.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctor_control.R;
import com.example.doctor_control.medical_report;

import java.util.ArrayList;

public class aOngoingAdapter extends RecyclerView.Adapter<aOngoingAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> patientNames;
    private ArrayList<String> problems;
    private ArrayList<String> distances;

    public aOngoingAdapter(Context context, ArrayList<String> patientNames,
                           ArrayList<String> problems, ArrayList<String> distances) {
        this.context = context;
        this.patientNames = patientNames;
        this.problems = problems;
        this.distances = distances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the card view layout (make sure the layout file name matches)
        View view = LayoutInflater.from(context).inflate(R.layout.item_ongoing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind data from separate arrays by index
        holder.tvPatientName.setText(patientNames.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText(distances.get(position));

        // Set up button click listeners (customize as needed)
        holder.btnComplete.setOnClickListener(v -> {
            // Handle the complete action
        });

        holder.btnTrack.setOnClickListener(v -> {
            // Handle the track action
        });
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, medical_report.class);
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return patientNames.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance;
        Button btnComplete, btnTrack, btnView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvDistance = itemView.findViewById(R.id.tv_distans);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnTrack = itemView.findViewById(R.id.btn_cancel); // Assuming "track" button uses this ID
            btnView = itemView.findViewById(R.id.btn_view);
        }
    }
}
