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

public class aRequestAdapeter extends RecyclerView.Adapter<aRequestAdapeter.ViewHolder> {

    private ArrayList<String> names;
    private ArrayList<String> problems;
    private ArrayList<String> distances;

    public aRequestAdapeter(ArrayList<String> names, ArrayList<String> problems, ArrayList<String> distances) {
        this.names = names;
        this.problems = problems;
        this.distances = distances;
    }

    @NonNull
    @Override
    public aRequestAdapeter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull aRequestAdapeter.ViewHolder holder, int position) {
        holder.tvPatientName.setText(names.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText(distances.get(position));

        holder.btnAccept.setOnClickListener(v -> {
            // Handle Accept
            names.remove(position);
            problems.remove(position);
            distances.remove(position);
            notifyItemRemoved(position);
        });

        holder.btnReject.setOnClickListener(v -> {
            // Handle Reject
            names.remove(position);
            problems.remove(position);
            distances.remove(position);
            notifyItemRemoved(position);
        });
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvPatientName, tvProblem, tvDistance;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvDistance = itemView.findViewById(R.id.tv_distans);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
