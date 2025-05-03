package com.example.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

    private final Context context;
    private final ArrayList<Appointment> appointments;

    public aRequestAdapeter(Context context, ArrayList<Appointment> appointments) {
        this.context = context;
        this.appointments = appointments;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Appointment appointment = appointments.get(position);
        holder.tvPatientName.setText(appointment.getFullName());
        holder.tvProblem.setText("Problem: " + appointment.getProblem());
        holder.tvDistance.setText(appointment.getDistance());

        // ─── Accept: prompt for ETA (raw value + unit) ──────────────────────
        holder.btnAccept.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Enter ETA");

            // Build dialog layout
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int pad = (int)(16 * context.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad, pad, pad);

            // 1) Number input
            EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Value");
            layout.addView(input);

            // 2) Unit selector
            Spinner unitSpinner = new Spinner(context);
            String[] units = {"Minutes", "Hours"};
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    context,
                    android.R.layout.simple_spinner_item,
                    units
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            unitSpinner.setAdapter(spinnerAdapter);
            layout.addView(unitSpinner);

            builder.setView(layout);

            builder.setPositiveButton("OK", (dialog, which) -> {
                String valStr = input.getText().toString().trim();
                if (valStr.isEmpty()) {
                    Toast.makeText(context, "Please enter ETA value", Toast.LENGTH_SHORT).show();
                    return;
                }
                // ← RAW value, no *60 conversion
                int rawEta = Integer.parseInt(valStr);
                String unit  = ((String)unitSpinner.getSelectedItem()).toLowerCase();

                updateAppointmentStatus(
                        appointment.getAppointmentId(),
                        "Pending",
                        position,
                        rawEta,   // exactly what was entered
                        unit      // "minutes" or "hours"
                );
            });

            builder.setNegativeButton("Cancel", (d, which) -> d.dismiss());
            builder.show();
        });

        // ─── Reject: no ETA needed ───────────────────────────────────────────
        holder.btnReject.setOnClickListener(v ->
                updateAppointmentStatus(
                        appointment.getAppointmentId(),
                        "Cancelled_by_doctor",
                        position,
                        0,
                        "minutes"
                )
        );
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
            tvProblem     = itemView.findViewById(R.id.tv_problem);
            tvDistance    = itemView.findViewById(R.id.tv_distans);
            btnAccept     = itemView.findViewById(R.id.btn_accept);
            btnReject     = itemView.findViewById(R.id.btn_reject);
        }
    }

    // Appointment model
    public static class Appointment {
        private final String appointmentId, fullName, problem;
        private String distance;
        public Appointment(String id, String name, String prob, String dist) {
            this.appointmentId = id;
            this.fullName      = name;
            this.problem       = prob;
            this.distance      = dist;
        }
        public String getAppointmentId() { return appointmentId; }
        public String getFullName()      { return fullName; }
        public String getProblem()       { return problem; }
        public String getDistance()      { return distance; }
        public void setDistance(String d) { this.distance = d; }
    }

    private void updateAppointmentStatus(
            String appointmentId,
            String newStatus,
            int position,
            int etaValue,
            String etaUnit
    ) {
        String url = "http://sxm.a58.mytemp.website/Doctors/update_appointment_status.php";

        JSONObject postData = new JSONObject();
        try {
            postData.put("appointment_id", appointmentId);
            postData.put("status", newStatus);
            postData.put("eta", etaValue);
            postData.put("eta_unit", etaUnit);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error preparing data.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, postData,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context,
                                "Appointment updated successfully.",
                                Toast.LENGTH_SHORT).show();

                        appointments.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, appointments.size());

                    } else {
                        String msg = response.optString("message", "Failed to update.");
                        if (msg.toLowerCase().contains("already")) {
                            // Longer toast for conflict/block message
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(context,
                            "Error updating appointment.", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

}
