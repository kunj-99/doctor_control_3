package com.example.doctor_control.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class aRequestAdapeter extends RecyclerView.Adapter<aRequestAdapeter.ViewHolder> {

    private Context context;
    private ArrayList<Appointment> appointments;

    public aRequestAdapeter(Context context, ArrayList<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
    }

    @NonNull
    @Override
    public aRequestAdapeter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull aRequestAdapeter.ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.tvPatientName.setText(appointment.getFullName());
        holder.tvProblem.setText("Problem: " + appointment.getProblem());
        holder.tvDistance.setText(appointment.getDistance());

        holder.btnAccept.setOnClickListener(v -> {
            // Update appointment status to "Pending"
            updateAppointmentStatus(appointment.getAppointmentId(), "Pending", position);
        });

        holder.btnReject.setOnClickListener(v -> {
            // Update appointment status to "Cancelled_by_doctor"
            updateAppointmentStatus(appointment.getAppointmentId(), "Cancelled_by_doctor", position);
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
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

    // Appointment model to hold appointment details
    public static class Appointment {
        private String appointmentId;
        private String fullName;
        private String problem;
        private String distance;

        public Appointment(String appointmentId, String fullName, String problem, String distance) {
            this.appointmentId = appointmentId;
            this.fullName = fullName;
            this.problem = problem;
            this.distance = distance;
        }

        public String getAppointmentId() {
            return appointmentId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getProblem() {
            return problem;
        }

        public String getDistance() {
            return distance;
        }
    }

    private void updateAppointmentStatus(String appointmentId, String newStatus, int position) {
        String url = "http://sxm.a58.mytemp.website/Doctors/update_appointment_status.php";

        JSONObject postData = new JSONObject();
        try {
            postData.put("appointment_id", appointmentId);
            postData.put("status", newStatus);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error preparing data.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, postData,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context, "Appointment updated successfully.", Toast.LENGTH_SHORT).show();
                        // Remove the appointment from the list and update the adapter
                        appointments.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, appointments.size());
                    } else {
                        String message = response.optString("message", "Failed to update appointment.");
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(context, "Error updating appointment.", Toast.LENGTH_SHORT).show();
                });

        queue.add(request);
    }
}
