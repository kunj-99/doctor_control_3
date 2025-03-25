package com.example.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;
import com.example.doctor_control.medical_report;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class aOngoingAdapter extends RecyclerView.Adapter<aOngoingAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> appointmentIds;
    private final ArrayList<String> patientNames;
    private final ArrayList<String> problems;
    private final ArrayList<String> timeSlots;
    private final ArrayList<Boolean> hasReport;
    private final ActivityResultLauncher<Intent> launcher;

    public aOngoingAdapter(Context context,
                           ArrayList<String> appointmentIds,
                           ArrayList<String> patientNames,
                           ArrayList<String> problems,
                           ArrayList<String> timeSlots,
                           ArrayList<Boolean> hasReport,
                           ActivityResultLauncher<Intent> launcher) {
        this.context = context;
        this.appointmentIds = appointmentIds;
        this.patientNames = patientNames;
        this.problems = problems;
        this.timeSlots = timeSlots;
        this.hasReport = hasReport;
        this.launcher = launcher;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ongoing, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvPatientName.setText(patientNames.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText("Time: " + timeSlots.get(position));

        boolean reportSubmitted = hasReport.get(position);

        // Enable or disable the "Complete" button based on the reportSubmitted flag.
        if (reportSubmitted) {
            holder.btnComplete.setEnabled(true);
            holder.btnComplete.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryColor));
            holder.btnComplete.setTextColor(Color.WHITE);
        } else {
            holder.btnComplete.setEnabled(false);
            holder.btnComplete.setBackgroundColor(Color.LTGRAY);
            holder.btnComplete.setTextColor(ContextCompat.getColor(context, R.color.gray));
        }

        holder.btnTrack.setOnClickListener(v -> {
            // Optional: Add tracking functionality if needed.
        });

        // When "View" is clicked, launch the medical_report activity.
        holder.btnView.setOnClickListener(v -> {
            Intent intent = new Intent(context, medical_report.class);
            intent.putExtra("appointment_id", appointmentIds.get(position));
            launcher.launch(intent); // Using ActivityResultLauncher to receive result.
        });

        // When "Complete" button is clicked, update status to Completed.
        holder.btnComplete.setOnClickListener(v -> {
            // Only allow update if the button is enabled.
            if (holder.btnComplete.isEnabled()) {
                updateAppointmentStatus(appointmentIds.get(position), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentIds.size();
    }

    // Helper method to update appointment status via Volley.
    private void updateAppointmentStatus(String appointmentId, int position) {
        String url = "http://sxm.a58.mytemp.website/Doctors/update_appointment_status.php"; // adjust URL if needed
        JSONObject payload = new JSONObject();
        try {
            payload.put("appointment_id", appointmentId);
            payload.put("action", "complete");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating request.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context, "Appointment marked as Completed", Toast.LENGTH_SHORT).show();
                        // Optionally, update the UI if needed.
                        hasReport.set(position, true);
                        notifyItemChanged(position);
                    } else {
                        Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(context, "Network error while updating status", Toast.LENGTH_SHORT).show();
                });
        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance;
        Button btnComplete, btnTrack, btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvDistance = itemView.findViewById(R.id.tv_distans);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnTrack = itemView.findViewById(R.id.btn_cancel);
            btnView = itemView.findViewById(R.id.btn_view);
        }
    }
}
