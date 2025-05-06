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
import com.example.doctor_control.track_patient_location;

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
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_pending, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull apendingAdapter.ViewHolder holder, int position) {
        Appointment appointment = appointments.get(position);
        holder.tvPatientName.setText(appointment.getName());
        holder.tvProblem.setText("Problem: " + appointment.getProblem());
        holder.tvDistance.setText("Distance: " + appointment.getDistance());

        // 💰 Show formatted payment
        if ("Online".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvAmount.setText("₹ " + appointment.getAmount() + " Paid");
        } else if ("Offline".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvAmount.setText("₹ " + appointment.getAmount() + " (Collect in cash)");
        } else {
            holder.tvAmount.setText("₹ " + appointment.getAmount());
        }

        // Show method separately too
        holder.tvPaymentMethod.setText("Payment Method: " + appointment.getPaymentMethod());

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
        TextView tvPatientName, tvProblem, tvDistance, tvAmount, tvPaymentMethod;
        Button btnCanform, btnTrack;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName    = itemView.findViewById(R.id.tv_patient_name);
            tvProblem        = itemView.findViewById(R.id.tv_problem);
            tvDistance       = itemView.findViewById(R.id.tv_distans);
            tvAmount         = itemView.findViewById(R.id.tv_price1); // 💰 Add in layout
            tvPaymentMethod  = itemView.findViewById(R.id.tvPaymentMethod2); // 💳 Add in layout
            btnCanform       = itemView.findViewById(R.id.btn_canform);
            btnTrack         = itemView.findViewById(R.id.btn_track);
        }
    }

    public static class Appointment {
        private final String appointmentId, name, problem, mapLink, amount, paymentMethod;
        private String distance;

        public Appointment(String appointmentId, String name, String problem,
                           String distance, String mapLink, String amount, String paymentMethod) {
            this.appointmentId = appointmentId;
            this.name = name;
            this.problem = problem;
            this.distance = distance;
            this.mapLink = mapLink;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
        }

        public String getAppointmentId() { return appointmentId; }
        public String getName()          { return name; }
        public String getProblem()       { return problem; }
        public String getDistance()      { return distance; }
        public String getMapLink()       { return mapLink; }
        public String getAmount()        { return amount; }
        public String getPaymentMethod() { return paymentMethod; }

        public void setDistance(String distance) { this.distance = distance; }
    }

    private void updateAppointmentStatus(String appointmentId, String newStatus, int position) {
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
                    String msg = response.optString("message", "Update failed.");
                    if (success) {
                        Toast.makeText(context, "Appointment confirmed", Toast.LENGTH_SHORT).show();
                        appointments.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, appointments.size());
                    } else {
                        if (msg.toLowerCase().contains("already")) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
        );

        queue.add(req);
    }
}
