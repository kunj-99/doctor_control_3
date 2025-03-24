package com.example.doctor_control.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.doctor_control.R;
import com.example.doctor_control.medical_report;

import java.util.ArrayList;

public class aOngoingAdapter extends RecyclerView.Adapter<aOngoingAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> appointmentIds;
    private ArrayList<String> patientNames;
    private ArrayList<String> problems;
    private ArrayList<String> timeSlots;
    private ArrayList<Boolean> hasReport;

    public aOngoingAdapter(Context context,
                           ArrayList<String> appointmentIds,
                           ArrayList<String> patientNames,
                           ArrayList<String> problems,
                           ArrayList<String> timeSlots,
                           ArrayList<Boolean> hasReport) {
        this.context = context;
        this.appointmentIds = appointmentIds;
        this.patientNames = patientNames;
        this.problems = problems;
        this.timeSlots = timeSlots;
        this.hasReport = hasReport;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_ongoing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvPatientName.setText(patientNames.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText("Time: " + timeSlots.get(position));

        boolean reportSubmitted = hasReport.get(position);
        if (!reportSubmitted) {
            holder.btnComplete.setEnabled(false);
            holder.btnComplete.setBackgroundColor(Color.WHITE);
            holder.btnComplete.setTextColor(ContextCompat.getColor(context, R.color.gray));
        } else {
            holder.btnComplete.setEnabled(true);
        }

        holder.btnTrack.setOnClickListener(v -> {
            // TODO: track patient location
        });

        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, medical_report.class);
            intent.putExtra("appointment_id", appointmentIds.get(position));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return patientNames.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance;
        Button btnComplete, btnTrack, btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem     = itemView.findViewById(R.id.tv_problem);
            tvDistance    = itemView.findViewById(R.id.tv_distans);
            btnComplete   = itemView.findViewById(R.id.btn_complete);
            btnTrack      = itemView.findViewById(R.id.btn_cancel);
            btnView       = itemView.findViewById(R.id.btn_view);
        }
    }
}
