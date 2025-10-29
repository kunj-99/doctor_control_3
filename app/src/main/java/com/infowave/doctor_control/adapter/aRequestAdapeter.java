package com.infowave.doctor_control.adapter;

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
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.R;

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

        // Price + payment method label (kept same logic as before)
        if ("Online".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment() + " paid");
        } else if ("Offline".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment() + " (Cash collection)");
        } else {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment());
        }
        holder.tvPaymentMethod.setText("Payment Method: " + appointment.getPaymentMethod());

        // Hide optional vet fields by default
        if (holder.tvAnimalCategory != null)   holder.tvAnimalCategory.setVisibility(View.GONE);
        if (holder.tvAnimalBreed != null)      holder.tvAnimalBreed.setVisibility(View.GONE);
        if (holder.tvVaccinationName != null)  holder.tvVaccinationName.setVisibility(View.GONE);

        // Show Animal Category if present
        if (holder.tvAnimalCategory != null) {
            String acn = appointment.getAnimalCategoryName();
            if (!isMissing(acn)) {
                holder.tvAnimalCategory.setText("Animal Category: " + acn.trim());
                holder.tvAnimalCategory.setVisibility(View.VISIBLE);
            }
        }

        // Show Animal Breed if present
        if (holder.tvAnimalBreed != null) {
            String breed = appointment.getAnimalBreed();
            if (!isMissing(breed)) {
                holder.tvAnimalBreed.setText("Breed: " + breed.trim());
                holder.tvAnimalBreed.setVisibility(View.VISIBLE);
            }
        }

        // Show Vaccination only when truly present (hide for null/"null"/"N/A"/empty/whitespace)
        if (holder.tvVaccinationName != null) {
            String vxn = appointment.getVaccinationName();
            if (!isMissing(vxn)) {
                holder.tvVaccinationName.setText("Vaccination: " + vxn.trim());
                holder.tvVaccinationName.setVisibility(View.VISIBLE);
            } else {
                holder.tvVaccinationName.setVisibility(View.GONE);
            }
        }

        // Accept -> enter ETA then move to Pending
        holder.btnAccept.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Enter ETA");

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int pad = (int)(16 * context.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad, pad, pad);

            EditText input = new EditText(context);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setHint("Value");
            layout.addView(input);

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
                    Toast.makeText(context, "Please enter an ETA value.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int rawEta = Integer.parseInt(valStr);
                String unit = ((String) unitSpinner.getSelectedItem()).toLowerCase();

                updateAppointmentStatus(
                        appointment.getAppointmentId(),
                        "Pending",
                        position,
                        rawEta,
                        unit
                );
            });

            builder.setNegativeButton("Cancel", (d, which) -> d.dismiss());
            builder.show();
        });

        // Reject
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
        TextView tvPatientName, tvProblem, tvDistance, tvPrice, tvPaymentMethod;
        TextView tvAnimalCategory, tvAnimalBreed, tvVaccinationName;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName   = itemView.findViewById(R.id.tv_patient_name);
            tvProblem       = itemView.findViewById(R.id.tv_problem);
            tvDistance      = itemView.findViewById(R.id.tv_distans);
            tvPrice         = itemView.findViewById(R.id.tv_price);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method1);
            btnAccept       = itemView.findViewById(R.id.btn_accept);
            btnReject       = itemView.findViewById(R.id.btn_reject);

            // IDs must match your XML
            tvAnimalCategory  = itemView.findViewById(R.id.tv_animal_name);
            tvAnimalBreed     = itemView.findViewById(R.id.tv_animal_breed);
            tvVaccinationName = itemView.findViewById(R.id.tv_vaccination_name);
        }
    }

    public static class Appointment {
        private final String appointmentId, fullName, problem, totalPayment, paymentMethod;
        private String distance;
        private final String animalCategoryName;
        private final String animalBreed;
        private final String vaccinationName;

        public Appointment(String id,
                           String name,
                           String prob,
                           String dist,
                           String totalPayment,
                           String paymentMethod,
                           String animalCategoryName,
                           String animalBreed,
                           String vaccinationName) {
            this.appointmentId = id;
            this.fullName = name;
            this.problem = prob;
            this.distance = dist;
            this.totalPayment = totalPayment;
            this.paymentMethod = paymentMethod;
            this.animalCategoryName = animalCategoryName;
            this.animalBreed = animalBreed;
            this.vaccinationName = vaccinationName;
        }

        public String getAppointmentId()       { return appointmentId; }
        public String getFullName()            { return fullName; }
        public String getProblem()             { return problem; }
        public String getDistance()            { return distance; }
        public String getTotalPayment()        { return totalPayment; }
        public String getPaymentMethod()       { return paymentMethod; }
        public void setDistance(String d)      { this.distance = d; }
        public String getAnimalCategoryName()  { return animalCategoryName; }
        public String getAnimalBreed()         { return animalBreed; }
        public String getVaccinationName()     { return vaccinationName; }
    }

    private void updateAppointmentStatus(
            String appointmentId,
            String newStatus,
            int position,
            int etaValue,
            String etaUnit
    ) {
        String url = ApiConfig.endpoint("Doctors/update_appointment_status.php");

        JSONObject postData = new JSONObject();
        try {
            postData.put("appointment_id", appointmentId);
            postData.put("status", newStatus);
            postData.put("eta", etaValue);
            postData.put("eta_unit", etaUnit);
        } catch (JSONException e) {
            Toast.makeText(context, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
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
                        String msg = response.optString("message", "Failed to update appointment.");
                        if (msg.toLowerCase().contains("already")) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> Toast.makeText(context,
                        "Unable to update appointment. Please check your network and try again.",
                        Toast.LENGTH_SHORT).show()
        );

        queue.add(request);
    }

    // Treat null/empty/"null"/"N/A"/"undefined" etc. as missing
    private static boolean isMissing(String s) {
        if (s == null) return true;
        String t = s.trim();
        if (t.isEmpty()) return true;
        String v = t.toLowerCase();
        return v.equals("null") || v.equals("none") || v.equals("n/a") || v.equals("na") || v.equals("undefined");
    }
}
