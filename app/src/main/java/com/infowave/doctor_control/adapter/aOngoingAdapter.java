package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.track_patient_location;
// ❗ Placeholder vet activity (implement this in your project)
import com.infowave.doctor_control.AnimalVirtualReportActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class aOngoingAdapter extends RecyclerView.Adapter<aOngoingAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<String> appointmentIds;
    private final ArrayList<String> patientNames;
    private final ArrayList<String> problems;
    private final ArrayList<String> distanceStrings;
    private final ArrayList<Boolean> hasReport;
    private final ArrayList<String> mapLinks;
    private final ArrayList<String> amounts;
    private final ArrayList<String> paymentMethods;
    private final Consumer<Integer> onAppointmentCompleted;
    private final BiConsumer<String, Integer> onAddReportClicked;

    // is_vet_case flags (same order as items)
    private ArrayList<Boolean> vetCases = new ArrayList<>();

    // Vet data arrays (same order as items)
    private ArrayList<String> animalCategoryNames = new ArrayList<>();
    private ArrayList<String> animalBreeds        = new ArrayList<>();
    private ArrayList<String> vaccinationNames    = new ArrayList<>();
    // NEW: vaccination IDs (same order as items)
    private ArrayList<String> vaccinationIds      = new ArrayList<>();

    public aOngoingAdapter(Context context,
                           ArrayList<String> appointmentIds,
                           ArrayList<String> patientNames,
                           ArrayList<String> problems,
                           ArrayList<String> distanceStrings,
                           ArrayList<Boolean> hasReport,
                           ArrayList<String> mapLinks,
                           ArrayList<String> amounts,
                           ArrayList<String> paymentMethods,
                           Consumer<Integer> onAppointmentCompleted,
                           BiConsumer<String, Integer> onAddReportClicked) {
        this.context = context;
        this.appointmentIds = appointmentIds;
        this.patientNames = patientNames;
        this.problems = problems;
        this.distanceStrings = distanceStrings;
        this.hasReport = hasReport;
        this.mapLinks = mapLinks;
        this.amounts = amounts;
        this.paymentMethods = paymentMethods;
        this.onAppointmentCompleted = onAppointmentCompleted;
        this.onAddReportClicked = onAddReportClicked;
    }

    // Called by Fragment after parsing JSON
    public void setVetCases(ArrayList<Boolean> list) {
        this.vetCases = (list != null) ? list : new ArrayList<>();
    }

    // Provide vet text fields
    public void setVetData(ArrayList<String> animalCategoryNames,
                           ArrayList<String> animalBreeds,
                           ArrayList<String> vaccinationNames) {
        this.animalCategoryNames = (animalCategoryNames != null) ? animalCategoryNames : new ArrayList<>();
        this.animalBreeds        = (animalBreeds != null)        ? animalBreeds        : new ArrayList<>();
        this.vaccinationNames    = (vaccinationNames != null)    ? vaccinationNames    : new ArrayList<>();
    }

    // NEW: Provide vaccination IDs
    public void setVaccinationIds(ArrayList<String> vaccinationIds) {
        this.vaccinationIds = (vaccinationIds != null) ? vaccinationIds : new ArrayList<>();
    }

    @NonNull
    @Override
    public aOngoingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_ongoing, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull aOngoingAdapter.ViewHolder holder, int position) {
        if (appointmentIds.size() == 0) {
            holder.tvPatientName.setText("No data available.");
            holder.tvProblem.setVisibility(View.GONE);
            holder.tvDistance.setVisibility(View.GONE);
            holder.tvAmount.setVisibility(View.GONE);
            holder.tvPaymentMethod.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnTrack.setVisibility(View.GONE);
            holder.btnView.setVisibility(View.GONE);

            // Hide vet fields too
            if (holder.tvAnimalCategory != null)  holder.tvAnimalCategory.setVisibility(View.GONE);
            if (holder.tvAnimalBreed != null)     holder.tvAnimalBreed.setVisibility(View.GONE);
            if (holder.tvVaccinationName != null) holder.tvVaccinationName.setVisibility(View.GONE);
            return;
        }

        holder.tvPatientName.setVisibility(View.VISIBLE);
        holder.tvProblem.setVisibility(View.VISIBLE);
        holder.tvDistance.setVisibility(View.VISIBLE);
        holder.tvAmount.setVisibility(View.VISIBLE);
        holder.tvPaymentMethod.setVisibility(View.VISIBLE);
        holder.btnComplete.setVisibility(View.VISIBLE);
        holder.btnTrack.setVisibility(View.VISIBLE);
        holder.btnView.setVisibility(View.VISIBLE);

        holder.tvPatientName.setText(patientNames.get(position));
        holder.tvProblem.setText("Problem: " + problems.get(position));
        holder.tvDistance.setText("Distance: " + distanceStrings.get(position));

        String amount = amounts.get(position);
        String method = paymentMethods.get(position);

        if ("Online".equalsIgnoreCase(method)) {
            holder.tvAmount.setText("₹ " + amount + " paid");
        } else if ("Offline".equalsIgnoreCase(method)) {
            holder.tvAmount.setText("₹ " + amount + " (Cash collection)");
        } else {
            holder.tvAmount.setText("₹ " + amount);
        }

        holder.tvPaymentMethod.setText("Payment Method: " + method);

        boolean reportSubmitted = hasReport.get(position);

        holder.btnComplete.setEnabled(reportSubmitted);
        holder.btnComplete.setBackgroundColor(reportSubmitted
                ? ContextCompat.getColor(context, R.color.primaryColor)
                : Color.LTGRAY);
        holder.btnComplete.setTextColor(ContextCompat.getColor(context,
                reportSubmitted ? android.R.color.white : R.color.gray));

        // ===== Vet fields binding (hide when missing) =====
        if (holder.tvAnimalCategory != null) holder.tvAnimalCategory.setVisibility(View.GONE);
        if (holder.tvAnimalBreed != null)    holder.tvAnimalBreed.setVisibility(View.GONE);
        if (holder.tvVaccinationName != null)holder.tvVaccinationName.setVisibility(View.GONE);

        // Show only when values are present (adapter lists may be shorter if fragment didn't set them)
        if (holder.tvAnimalCategory != null && position < animalCategoryNames.size()) {
            String ac = clean(animalCategoryNames.get(position));
            if (!ac.isEmpty()) {
                holder.tvAnimalCategory.setText("Animal Category: " + ac);
                holder.tvAnimalCategory.setVisibility(View.VISIBLE);
            }
        }
        if (holder.tvAnimalBreed != null && position < animalBreeds.size()) {
            String br = clean(animalBreeds.get(position));
            if (!br.isEmpty()) {
                holder.tvAnimalBreed.setText("Breed: " + br);
                holder.tvAnimalBreed.setVisibility(View.VISIBLE);
            }
        }
        if (holder.tvVaccinationName != null && position < vaccinationNames.size()) {
            String vx = clean(vaccinationNames.get(position));
            if (!vx.isEmpty()) {
                holder.tvVaccinationName.setText("Vaccination: " + vx);
                holder.tvVaccinationName.setVisibility(View.VISIBLE);
            } else {
                holder.tvVaccinationName.setVisibility(View.GONE);
            }
        }
        // ==================================================

        // Add Report / View logic
        if (reportSubmitted) {
            holder.btnView.setEnabled(false);
            holder.btnView.setBackgroundColor(Color.LTGRAY);
            holder.btnView.setText("Report Added");
        } else {
            holder.btnView.setEnabled(true);
            holder.btnView.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryColor));
            holder.btnView.setTextColor(Color.WHITE);
            holder.btnView.setText("Add report");

            holder.btnView.setOnClickListener(v -> {
                boolean isVet = (position < vetCases.size()) && Boolean.TRUE.equals(vetCases.get(position));
                if (isVet) {
                    // ===== VET FLOW =====
                    Intent intent = new Intent(context, AnimalVirtualReportActivity.class);
                    intent.putExtra("appointment_id", appointmentIds.get(position));

                    // NEW: pass vaccination name & id if available (safe bounds + clean)
                    String vxName = (position < vaccinationNames.size()) ? clean(vaccinationNames.get(position)) : "";
                    String vxId   = (position < vaccinationIds.size())   ? clean(vaccinationIds.get(position))   : "";
                    if (!vxName.isEmpty()) intent.putExtra("vaccination_name", vxName);
                    if (!vxId.isEmpty())   intent.putExtra("vaccination_id",   vxId);

                    context.startActivity(intent);
                } else {
                    // ===== HUMAN FLOW =====
                    if (onAddReportClicked != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        onAddReportClicked.accept(appointmentIds.get(position), position);
                    }
                }
            });
        }

        holder.btnTrack.setOnClickListener(v -> {
            String mapLink = mapLinks.get(position);
            if (mapLink != null && !mapLink.isEmpty()) {
                Intent intent = new Intent(context, track_patient_location.class);
                intent.putExtra("map_link", mapLink);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Location not available for this patient.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnComplete.setOnClickListener(v -> {
            if (holder.btnComplete.isEnabled()) {
                holder.btnComplete.setText("Completing...");
                holder.btnComplete.setEnabled(false);
                updateAppointmentStatus(appointmentIds.get(position), position, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentIds.size() > 0 ? appointmentIds.size() : 1;
    }

    private void updateAppointmentStatus(String appointmentId, int position, ViewHolder holder) {
        String url = ApiConfig.endpoint("Doctors/update_appointment_status.php");

        JSONObject payload = new JSONObject();
        try {
            payload.put("appointment_id", appointmentId);
            payload.put("action", "complete");
        } catch (JSONException e) {
            Toast.makeText(context, "Sorry, we couldn't process your request.", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("SetTextI18n")
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, payload,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context, "Appointment marked as completed.", Toast.LENGTH_SHORT).show();
                        if (onAppointmentCompleted != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onAppointmentCompleted.accept(position);
                            removeAppointment(position);
                        }
                    } else {
                        Toast.makeText(context, "Unable to complete appointment. Please try again.", Toast.LENGTH_SHORT).show();
                        holder.btnComplete.setEnabled(true);
                        holder.btnComplete.setText("Complete");
                    }
                },
                error -> {
                    Toast.makeText(context, "A network error occurred. Please check your connection.", Toast.LENGTH_SHORT).show();
                    holder.btnComplete.setEnabled(true);
                    holder.btnComplete.setText("Complete");
                }
        );

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    private void removeAppointment(int position) {
        appointmentIds.remove(position);
        patientNames.remove(position);
        problems.remove(position);
        distanceStrings.remove(position);
        hasReport.remove(position);
        mapLinks.remove(position);
        amounts.remove(position);
        paymentMethods.remove(position);
        if (position < vetCases.size()) vetCases.remove(position);

        // Keep vet data arrays aligned
        if (position < animalCategoryNames.size()) animalCategoryNames.remove(position);
        if (position < animalBreeds.size())        animalBreeds.remove(position);
        if (position < vaccinationNames.size())    vaccinationNames.remove(position);
        if (position < vaccinationIds.size())      vaccinationIds.remove(position);

        notifyItemRemoved(position);
        notifyItemRangeChanged(position, appointmentIds.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance, tvAmount, tvPaymentMethod;
        TextView tvAnimalCategory, tvAnimalBreed, tvVaccinationName; // NEW: vet views
        Button btnComplete, btnTrack, btnView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvDistance = itemView.findViewById(R.id.tv_distans);
            tvAmount = itemView.findViewById(R.id.tv_price2);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod3);
            btnComplete = itemView.findViewById(R.id.btn_complete);
            btnTrack = itemView.findViewById(R.id.btn_cancel);
            btnView = itemView.findViewById(R.id.btn_view);

            // IDs from item_ongoing.xml (third card layout)
            tvAnimalCategory   = itemView.findViewById(R.id.tvAnimalName3);
            tvAnimalBreed      = itemView.findViewById(R.id.tvAnimalBreed3);
            tvVaccinationName  = itemView.findViewById(R.id.tvVaccinationName3);
        }
    }

    // Treat null/empty/"null"/"n/a"/"undefined" etc. as missing
    private static String clean(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return "";
        String v = t.toLowerCase();
        if (v.equals("null") || v.equals("none") || v.equals("n/a") || v.equals("na") || v.equals("undefined"))
            return "";
        return t;
    }
}
