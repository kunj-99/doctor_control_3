package com.example.doctor_control.adapter;

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
import com.example.doctor_control.track_patient_location;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Adapter for pending appointments, showing name, problem, distance,
 * and Confirm / Track buttons.
 */
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
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_pending, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull apendingAdapter.ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.tvPatientName.setText(appointment.getName());
        holder.tvProblem.setText("Problem: " + appointment.getProblem());
        holder.tvDistance.setText("Distance: " + appointment.getDistance());

        holder.btnCanform.setOnClickListener(v -> {
            Log.d(TAG, "Confirm clicked for ID: " + appointment.getAppointmentId());
            updateAppointmentStatus(appointment.getAppointmentId(), "Confirmed", position);
        });

        holder.btnTrack.setOnClickListener(v -> {
            Log.d(TAG, "Track clicked for ID: " + appointment.getAppointmentId());
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance;
        Button btnCanform, btnTrack;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem     = itemView.findViewById(R.id.tv_problem);
            tvDistance    = itemView.findViewById(R.id.tv_distans);
            btnCanform    = itemView.findViewById(R.id.btn_canform);
            btnTrack      = itemView.findViewById(R.id.btn_track);
        }
    }

    /**
     * Model for a pending appointment.
     * Distance and mapLink can be updated externally.
     */
    public static class Appointment {
        private final String appointmentId;
        private final String name;
        private final String problem;
        private String distance;       // now mutable
        private final String mapLink;

        public Appointment(String appointmentId,
                           String name,
                           String problem,
                           String distance,
                           String mapLink) {
            this.appointmentId = appointmentId;
            this.name          = name;
            this.problem       = problem;
            this.distance      = distance;
            this.mapLink       = mapLink;
        }

        public String getAppointmentId() { return appointmentId; }
        public String getName()          { return name; }
        public String getProblem()       { return problem; }
        public String getDistance()      { return distance; }
        public String getMapLink()       { return mapLink; }

        /** Allows fragment to update driving distance when ready */
        public void setDistance(String distance) {
            this.distance = distance;
        }
    }

    private void updateAppointmentStatus(String appointmentId,
                                         String newStatus,
                                         int position) {
        String url = "http://sxm.a58.mytemp.website/Doctors/update_appointment_status.php";
        JSONObject payload = new JSONObject();
        try {
            payload.put("appointment_id", appointmentId);
            payload.put("status", newStatus);
        } catch (JSONException e) {
            Toast.makeText(context, "Error preparing request.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, payload,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context,
                                "Appointment confirmed", Toast.LENGTH_SHORT).show();
                        appointments.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, appointments.size());
                    } else {
                        Toast.makeText(context,
                                response.optString("message","Update failed"),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(context,
                            "Network error", Toast.LENGTH_SHORT).show();
                }
        );
        queue.add(req);
    }
}
