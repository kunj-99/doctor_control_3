package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.track_patient_location;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class apendingAdapter extends RecyclerView.Adapter<apendingAdapter.ViewHolder> {

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
            holder.tvAmount.setText("₹ " + appointment.getAmount() + " paid");
        } else if ("Offline".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvAmount.setText("₹ " + appointment.getAmount() + " (Cash collection)");
        } else {
            holder.tvAmount.setText("₹ " + appointment.getAmount());
        }

        // 💳 Payment method label
        holder.tvPaymentMethod.setText("Payment Method: " + appointment.getPaymentMethod());

        // ---- Vet fields visibility handling ----
        // Hide by default
        if (holder.tvAnimalCategory != null)  holder.tvAnimalCategory.setVisibility(View.GONE);
        if (holder.tvAnimalBreed != null)     holder.tvAnimalBreed.setVisibility(View.GONE);
        if (holder.tvVaccinationName != null) holder.tvVaccinationName.setVisibility(View.GONE);

        // Animal Category
        if (holder.tvAnimalCategory != null && !isMissing(appointment.getAnimalCategoryName())) {
            holder.tvAnimalCategory.setText("Animal Category: " + appointment.getAnimalCategoryName().trim());
            holder.tvAnimalCategory.setVisibility(View.VISIBLE);
        }

        // Breed
        if (holder.tvAnimalBreed != null && !isMissing(appointment.getAnimalBreed())) {
            holder.tvAnimalBreed.setText("Breed: " + appointment.getAnimalBreed().trim());
            holder.tvAnimalBreed.setVisibility(View.VISIBLE);
        }

        // Vaccination (hide when null/empty/"null"/"N/A"/whitespace)
        if (holder.tvVaccinationName != null && !isMissing(appointment.getVaccinationName())) {
            holder.tvVaccinationName.setText("Vaccination: " + appointment.getVaccinationName().trim());
            holder.tvVaccinationName.setVisibility(View.VISIBLE);
        } else if (holder.tvVaccinationName != null) {
            holder.tvVaccinationName.setVisibility(View.GONE);
        }
        // ---------------------------------------

        holder.btnCanform.setOnClickListener(v ->
                updateAppointmentStatus(appointment.getAppointmentId(), "Confirmed", position)
        );

        holder.btnTrack.setOnClickListener(v -> {
            String mapLink = appointment.getMapLink();
            if (mapLink != null && !mapLink.isEmpty()) {
                Intent intent = new Intent(context, track_patient_location.class);
                intent.putExtra("map_link", mapLink);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Patient location is not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance, tvAmount, tvPaymentMethod;
        TextView tvAnimalCategory, tvAnimalBreed, tvVaccinationName; // vet views
        Button btnCanform, btnTrack;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName    = itemView.findViewById(R.id.tv_patient_name);
            tvProblem        = itemView.findViewById(R.id.tv_problem);
            tvDistance       = itemView.findViewById(R.id.tv_distans);
            tvAmount         = itemView.findViewById(R.id.tv_price1);
            tvPaymentMethod  = itemView.findViewById(R.id.tvPaymentMethod2);
            btnCanform       = itemView.findViewById(R.id.btn_canform);
            btnTrack         = itemView.findViewById(R.id.btn_track);

            // Vet field IDs from item_pending.xml (second card layout)
            tvAnimalCategory   = itemView.findViewById(R.id.tvAnimalName2);
            tvAnimalBreed      = itemView.findViewById(R.id.tvAnimalBreed2);
            tvVaccinationName  = itemView.findViewById(R.id.tvVaccinationName2);
        }
    }

    public static class Appointment {
        private final String appointmentId, name, problem, mapLink, amount, paymentMethod;
        private String distance;
        // NEW vet fields
        private final String animalCategoryName;
        private final String animalBreed;
        private final String vaccinationName;

        // New full constructor (used by updated fragment)
        public Appointment(String appointmentId, String name, String problem,
                           String distance, String mapLink, String amount, String paymentMethod,
                           String animalCategoryName, String animalBreed, String vaccinationName) {
            this.appointmentId = appointmentId;
            this.name = name;
            this.problem = problem;
            this.distance = distance;
            this.mapLink = mapLink;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.animalCategoryName = animalCategoryName;
            this.animalBreed = animalBreed;
            this.vaccinationName = vaccinationName;
        }

        // Backward-compatible constructor (if any old calls exist)
        public Appointment(String appointmentId, String name, String problem,
                           String distance, String mapLink, String amount, String paymentMethod) {
            this(appointmentId, name, problem, distance, mapLink, amount, paymentMethod,
                    "", "", "");
        }

        public String getAppointmentId()        { return appointmentId; }
        public String getName()                 { return name; }
        public String getProblem()              { return problem; }
        public String getDistance()             { return distance; }
        public String getMapLink()              { return mapLink; }
        public String getAmount()               { return amount; }
        public String getPaymentMethod()        { return paymentMethod; }
        public void setDistance(String distance){ this.distance = distance; }

        // Vet getters
        public String getAnimalCategoryName()   { return animalCategoryName; }
        public String getAnimalBreed()          { return animalBreed; }
        public String getVaccinationName()      { return vaccinationName; }
    }

    private void updateAppointmentStatus(String appointmentId, String newStatus, int position) {
        String url = ApiConfig.endpoint("Doctors/update_appointment_status.php");

        JSONObject payload = new JSONObject();
        try {
            payload.put("appointment_id", appointmentId);
            payload.put("status", newStatus);
        } catch (JSONException e) {
            Toast.makeText(context, "Unable to process your request.", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, url, payload,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    String msg = response.optString("message", "Unable to update appointment.");
                    if (success) {
                        Toast.makeText(context, "Appointment confirmed successfully.", Toast.LENGTH_SHORT).show();
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
                error -> Toast.makeText(context, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
        );

        queue.add(req);
    }

    // Treat null/empty/"null"/"n/a"/"undefined" etc. as missing
    private static boolean isMissing(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        String v = t.toLowerCase();
        return v.equals("null") || v.equals("none") || v.equals("n/a") || v.equals("na") || v.equals("undefined");
    }
}
