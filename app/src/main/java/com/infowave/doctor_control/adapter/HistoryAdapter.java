package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.ExitGuard;
import com.infowave.doctor_control.HistoryItem;
import com.infowave.doctor_control.PatientProfileActivity;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.patient_bill;
import com.infowave.doctor_control.view_patient_report;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<HistoryItem> historyItems;
    private final List<String> appointmentIds;

    public HistoryAdapter(List<HistoryItem> historyItems, List<String> appointmentIds) {
        this.historyItems = historyItems;
        this.appointmentIds = appointmentIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);

        holder.tvPatientName.setText(item.getPatientName());
        holder.tvAppointmentDate.setText("Appointment Date: " + item.getAppointmentDate());
        holder.tvProblem.setText("Problem: " + item.getProblem());

        String status = item.getStatus() == null ? "" : item.getStatus();
        if (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("cancelled_by_doctor")) {
            holder.tvPaymentStatus.setText(status);
            holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_payment_pending);
            holder.btnViewBill.setEnabled(false);
            holder.btnViewReport.setEnabled(false);
        } else {
            if (item.isPaymentReceived()) {
                holder.tvPaymentStatus.setText("Payment Received");
                holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_payment_status);
            } else {
                holder.tvPaymentStatus.setText("Payment Pending");
                holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_payment_pending);
            }
            holder.btnViewBill.setEnabled(true);
            holder.btnViewReport.setEnabled(true);
        }

        // Expand / collapse actions
        holder.itemView.setOnClickListener(v -> {
            if (holder.buttonContainer.getVisibility() == View.GONE) {
                holder.buttonContainer.setVisibility(View.VISIBLE);
                holder.buttonContainer.animate().alpha(1f).setDuration(200);
            } else {
                holder.buttonContainer.animate().alpha(0f).setDuration(200)
                        .withEndAction(() -> holder.buttonContainer.setVisibility(View.GONE));
            }
        });

        // View Profile
        holder.btnViewProfile.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            ExitGuard.suppressNextPrompt();
            Intent intent = new Intent(v.getContext(), PatientProfileActivity.class);
            intent.putExtra("patient_id", historyItems.get(pos).getPatientId());
            v.getContext().startActivity(intent);
        });

        // View Bill
        holder.btnViewBill.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            String apptIdStr = (appointmentIds != null && pos < appointmentIds.size())
                    ? appointmentIds.get(pos) : null;
            if (apptIdStr == null || apptIdStr.trim().isEmpty()) return;

            int apptId;
            try {
                apptId = Integer.parseInt(apptIdStr);
            } catch (NumberFormatException e) {
                return;
            }

            ExitGuard.suppressNextPrompt();
            Intent intent = new Intent(v.getContext(), patient_bill.class);
            intent.putExtra("appointment_id", apptId);
            v.getContext().startActivity(intent);
        });

        // View Report
        holder.btnViewReport.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            String apptIdStr = (appointmentIds != null && pos < appointmentIds.size())
                    ? appointmentIds.get(pos) : null;
            if (apptIdStr == null || apptIdStr.trim().isEmpty()) return;

            ExitGuard.suppressNextPrompt();
            Intent intent = new Intent(v.getContext(), view_patient_report.class);
            intent.putExtra("appointment_id", apptIdStr);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return historyItems != null ? historyItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvPatientName, tvAppointmentDate, tvProblem, tvPaymentStatus;
        public final Button btnViewBill, btnViewReport, btnViewProfile;
        public final View buttonContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tv_patient_name);
            tvAppointmentDate = itemView.findViewById(R.id.tv_appointment_date);
            tvProblem = itemView.findViewById(R.id.tv_problem);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            buttonContainer = itemView.findViewById(R.id.button_container);
            btnViewBill = itemView.findViewById(R.id.btn_view_bill);
            btnViewReport = itemView.findViewById(R.id.btn_view_report);
            btnViewProfile = itemView.findViewById(R.id.btn_view_profile);
        }
    }
}
