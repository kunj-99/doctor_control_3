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
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.track_patient_location;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_ongoing, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (appointmentIds.size() == 0) {
            holder.tvPatientName.setText("No Data Available");
            holder.tvProblem.setVisibility(View.GONE);
            holder.tvDistance.setVisibility(View.GONE);
            holder.tvAmount.setVisibility(View.GONE);
            holder.tvPaymentMethod.setVisibility(View.GONE);
            holder.btnComplete.setVisibility(View.GONE);
            holder.btnTrack.setVisibility(View.GONE);
            holder.btnView.setVisibility(View.GONE);
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
            holder.tvAmount.setText("₹ " + amount + " Paid");
        } else if ("Offline".equalsIgnoreCase(method)) {
            holder.tvAmount.setText("₹ " + amount + " (Collect in cash)");
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

        if (reportSubmitted) {
            holder.btnView.setEnabled(false);
            holder.btnView.setBackgroundColor(Color.LTGRAY);
            holder.btnView.setText("Report Added");
        } else {
            holder.btnView.setEnabled(true);
            holder.btnView.setBackgroundColor(ContextCompat.getColor(context, R.color.primaryColor));
            holder.btnView.setTextColor(Color.WHITE);
            holder.btnView.setText("Add Report");

            holder.btnView.setOnClickListener(v -> {
                if (onAddReportClicked != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onAddReportClicked.accept(appointmentIds.get(position), position);
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
                Toast.makeText(context, "Map link not available", Toast.LENGTH_SHORT).show();
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
        String url = "http://sxm.a58.mytemp.website/Doctors/update_appointment_status.php";
        JSONObject payload = new JSONObject();
        try {
            payload.put("appointment_id", appointmentId);
            payload.put("action", "complete");
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error creating request.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, payload,
                response -> {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        Toast.makeText(context, "Marked as Completed", Toast.LENGTH_SHORT).show();
                        if (onAppointmentCompleted != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            onAppointmentCompleted.accept(position);
                            // Remove item after completion
                            removeAppointment(position);
                        }
                    } else {
                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                        holder.btnComplete.setEnabled(true);
                        holder.btnComplete.setText("Complete");
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
                    holder.btnComplete.setEnabled(true);
                    holder.btnComplete.setText("Complete");
                }
        );

        RequestQueue queue = Volley.newRequestQueue(context);
        queue.add(request);
    }

    // Method to remove an appointment from the list
    private void removeAppointment(int position) {
        appointmentIds.remove(position);
        patientNames.remove(position);
        problems.remove(position);
        distanceStrings.remove(position);
        hasReport.remove(position);
        mapLinks.remove(position);
        amounts.remove(position);
        paymentMethods.remove(position);

        // Notify the adapter about the removal
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, appointmentIds.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvProblem, tvDistance, tvAmount, tvPaymentMethod;
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
        }
    }
}
