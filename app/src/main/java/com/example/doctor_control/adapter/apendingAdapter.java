package com.example.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.example.doctor_control.track_patient_location; // Ensure this activity exists

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class apendingAdapter extends RecyclerView.Adapter<apendingAdapter.ViewHolder> {

    private static final String TAG = "apendingAdapter";
    private final Context context;
    private final ArrayList<Appointment> appointments;

    public apendingAdapter(Context context, ArrayList<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
    }

    @NonNull
    @Override
    public apendingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull apendingAdapter.ViewHolder holder, final int position) {
        final Appointment appointment = appointments.get(position);
        holder.tvPatientName.setText(appointment.getName());
        holder.tvProblem.setText("Problem: " + appointment.getProblem());
        holder.tvDistance.setText(appointment.getDistance());

        // Confirm button action: update appointment status to "Confirmed"
        holder.btnCanform.setOnClickListener(v -> {
            Log.d(TAG, "Confirm clicked for appointment ID: " + appointment.getAppointmentId());
            updateAppointmentStatus(appointment.getAppointmentId(), "Confirmed", position);
        });

        // Track button action: launch track_patient_location activity with the map link
        holder.btnTrack.setOnClickListener(v -> {
            Log.d(TAG, "Track clicked for appointment ID: " + appointment.getAppointmentId());
            String mapLink = appointment.getMapLink();
            if (mapLink != null && !mapLink.isEmpty()) {
                Intent intent = new Intent(context, track_patient_location.class);
                intent.putExtra("map_link", mapLink);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Map link not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
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

    // Appointment model class to hold pending appointment details.
    public static class Appointment {
        private final String appointmentId;
        private final String name;
        private final String problem;
        private final String distance;
        private final String mapLink;  // New field for the map link

        // Updated constructor to include the map link
        public Appointment(String appointmentId, String name, String problem, String distance, String mapLink) {
            this.appointmentId = appointmentId;
            this.name = name;
            this.problem = problem;
            this.distance = distance;
            this.mapLink = mapLink;
        }

        public String getAppointmentId() {
            return appointmentId;
        }

        public String getName() {
            return name;
        }

        public String getProblem() {
            return problem;
        }

        public String getDistance() {
            return distance;
        }

        public String getMapLink() {
            return mapLink;
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
