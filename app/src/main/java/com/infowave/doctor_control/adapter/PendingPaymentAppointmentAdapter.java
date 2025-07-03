package com.infowave.doctor_control.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.PendingPaymentAppointment;
import com.infowave.doctor_control.R;

import java.util.List;

public class PendingPaymentAppointmentAdapter extends RecyclerView.Adapter<PendingPaymentAppointmentAdapter.ViewHolder> {
    private List<PendingPaymentAppointment> appointmentList;

    public PendingPaymentAppointmentAdapter(List<PendingPaymentAppointment> appointmentList) {
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_payment_appointment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingPaymentAppointment appt = appointmentList.get(position);
        holder.tvPatientName.setText("Patient: " + appt.patientName);
        holder.tvAmount.setText("Amount: ₹" + String.format("%.2f", appt.amount));
        holder.tvPaymentStatus.setText("Status: " + appt.paymentStatus);
        holder.tvCreatedAt.setText("Created: " + appt.createdAt);
    }

    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvAmount, tvPaymentStatus, tvCreatedAt;

        public ViewHolder(View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
