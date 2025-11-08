package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.InputType;
import android.util.Log; // ✅ added for logging
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

    private static final String TAG_REQ  = "REQ:update_status";
    private static final String TAG_RESP = "RESP:update_status";
    private static final String TAG_DBG  = "RESP:debug";
    private static final String TAG_REF  = "RESP:refund";

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

        if ("Online".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment() + " paid");
        } else if ("Offline".equalsIgnoreCase(appointment.getPaymentMethod())) {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment() + " (Cash collection)");
        } else {
            holder.tvPrice.setText("₹ " + appointment.getTotalPayment());
        }
        holder.tvPaymentMethod.setText("Payment Method: " + appointment.getPaymentMethod());

        if (holder.tvAnimalCategory != null)   holder.tvAnimalCategory.setVisibility(View.GONE);
        if (holder.tvAnimalBreed != null)      holder.tvAnimalBreed.setVisibility(View.GONE);
        if (holder.tvVaccinationName != null)  holder.tvVaccinationName.setVisibility(View.GONE);

        if (holder.tvAnimalCategory != null) {
            String acn = appointment.getAnimalCategoryName();
            if (!isMissing(acn)) {
                holder.tvAnimalCategory.setText("Animal Category: " + acn.trim());
                holder.tvAnimalCategory.setVisibility(View.VISIBLE);
            }
        }

        if (holder.tvAnimalBreed != null) {
            String breed = appointment.getAnimalBreed();
            if (!isMissing(breed)) {
                holder.tvAnimalBreed.setText("Breed: " + breed.trim());
                holder.tvAnimalBreed.setVisibility(View.VISIBLE);
            }
        }

        if (holder.tvVaccinationName != null) {
            String vxn = appointment.getVaccinationName();
            if (!isMissing(vxn)) {
                holder.tvVaccinationName.setText("Vaccination: " + vxn.trim());
                holder.tvVaccinationName.setVisibility(View.VISIBLE);
            } else {
                holder.tvVaccinationName.setVisibility(View.GONE);
            }
        }

        // Accept → enter ETA then move to Pending
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

    // ----------------------- SAFE REMOVAL HELPERS -----------------------

    /** Find current index of an appointment by id in the backing list. */
    private int indexOfAppointment(String appointmentId) {
        if (appointmentId == null) return -1;
        for (int i = 0; i < appointments.size(); i++) {
            Appointment a = appointments.get(i);
            if (appointmentId.equals(a.getAppointmentId())) {
                return i;
            }
        }
        return -1;
    }

    /** Remove by id with bounds checks. */
    private void removeAppointmentSafely(String appointmentId, int suggestedPosition) {
        // 1) If the original position still points to the same item, use it.
        if (suggestedPosition >= 0 && suggestedPosition < appointments.size()) {
            Appointment atPos = appointments.get(suggestedPosition);
            if (appointmentId.equals(atPos.getAppointmentId())) {
                appointments.remove(suggestedPosition);
                notifyItemRemoved(suggestedPosition);
                if (suggestedPosition < appointments.size()) {
                    notifyItemRangeChanged(suggestedPosition, appointments.size() - suggestedPosition);
                }
                return;
            }
        }
        // 2) Otherwise, look up the current index by id.
        int idx = indexOfAppointment(appointmentId);
        if (idx >= 0) {
            appointments.remove(idx);
            notifyItemRemoved(idx);
            if (idx < appointments.size()) {
                notifyItemRangeChanged(idx, appointments.size() - idx);
            }
            return;
        }
        // 3) Already gone (e.g., list refreshed) — just refresh UI safely.
        notifyDataSetChanged();
    }

    // -------------------------------------------------------------------

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

        // ✅ Request log
        Log.d(TAG_REQ, "url=" + url + " body=" + postData.toString());

        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, postData,
                response -> {
                    // ✅ Raw response log
                    Log.d(TAG_RESP, response.toString());

                    // ✅ Refund-specific logs
                    boolean success = response.optBoolean("success", false);
                    boolean refundCreated = response.optBoolean("refund_created", false);
                    boolean walletCredited = response.optBoolean("wallet_credited", false);
                    boolean phUpdated = response.optBoolean("payment_history_updated", false);
                    String message = response.optString("message", "");

                    Log.d(TAG_REF,
                            "success=" + success +
                                    ", refund_created=" + refundCreated +
                                    ", wallet_credited=" + walletCredited +
                                    ", payment_history_updated=" + phUpdated +
                                    ", message=\"" + message + "\""
                    );

                    // ✅ Debug object log (contains refund_reason / ph_seeded etc.)
                    JSONObject dbg = response.optJSONObject("debug");
                    if (dbg != null) {
                        Log.d(TAG_DBG, dbg.toString());
                    }

                    if (success) {
                        Toast.makeText(context,
                                "Appointment updated successfully.",
                                Toast.LENGTH_SHORT).show();

                        // ✅ Safe removal (prevents IndexOutOfBoundsException)
                        removeAppointmentSafely(appointmentId, position);

                    } else {
                        String msg = response.optString("message", "Failed to update appointment.");
                        if (msg.toLowerCase().contains("already")) {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                error -> {
                    // ✅ Network error log
                    Log.d(TAG_RESP, "error=" + error.toString());
                    Toast.makeText(context,
                            "Unable to update appointment. Please check your network and try again.",
                            Toast.LENGTH_SHORT).show();
                }
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
