package com.example.doctor_control.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doctor_control.HistoryItem;
import com.example.doctor_control.PatientProfileActivity;
import com.example.doctor_control.R;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyItems;

    public HistoryAdapter(List<HistoryItem> historyItems) {
        this.historyItems = historyItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyItems.get(position);
        holder.tvPatientName.setText(item.getPatientName());
        holder.tvAppointmentDate.setText("Appointment Date: " + item.getAppointmentDate());
        holder.tvProblem.setText("Problem: " + item.getProblem());

        if (item.isPaymentReceived()) {
            holder.tvPaymentStatus.setText("Payment Received");
            holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_payment_status);
        } else {
            holder.tvPaymentStatus.setText("Payment Pending");
            holder.tvPaymentStatus.setBackgroundResource(R.drawable.bg_payment_pending);
        }

        // Toggle the button container visibility with smooth animation
        holder.itemView.setOnClickListener(v -> {
            if (holder.buttonContainer.getVisibility() == View.GONE) {
                holder.buttonContainer.setVisibility(View.VISIBLE);
                holder.buttonContainer.animate().alpha(1.0f).setDuration(200);
            } else {
                holder.buttonContainer.animate().alpha(0.0f).setDuration(200).withEndAction(() ->
                        holder.buttonContainer.setVisibility(View.GONE));
            }
        });

        // Pass the patient_id to the PatientProfileActivity via intent extras
        holder.btnViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), PatientProfileActivity.class);
            intent.putExtra("patient_id", item.getPatientId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return historyItems != null ? historyItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvPatientName, tvAppointmentDate, tvProblem, tvPaymentStatus;
        public Button btnViewBill, btnViewReport, btnViewProfile;
        public View buttonContainer;

        public ViewHolder(View itemView) {
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
