package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.R;
import com.infowave.doctor_control.SettlementAppointment;

import java.util.List;

public class SettlementAppointmentAdapter extends RecyclerView.Adapter<SettlementAppointmentAdapter.VH> {
    private final List<SettlementAppointment> list;

    public SettlementAppointmentAdapter(List<SettlementAppointment> list) {
        this.list = list;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_settlement_appointment, parent, false);
        return new VH(v);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        SettlementAppointment a = list.get(position);

        h.tvPatientName.setText(
                (a.patientName != null && !a.patientName.isEmpty())
                        ? "Patient: " + a.patientName
                        : "Patient ID: " + a.patientId
        );

        h.tvAmount.setText("Total: ₹" + String.format("%.2f", a.amountTotal));
        h.tvGst.setText("GST: ₹" + String.format("%.2f", a.gst));
        h.tvBase.setText("Base: ₹" + String.format("%.2f", a.baseExGst));
        h.tvMethod.setText("Method: " + a.paymentMethod);

        if (a.deposit > 0) {
            h.tvDeposit.setVisibility(View.VISIBLE);
            String dep = "Deposit: ₹" + String.format("%.2f", a.deposit);
            if (a.depositStatus != null && !a.depositStatus.isEmpty()) {
                dep += " (" + a.depositStatus + ")";
            }
            h.tvDeposit.setText(dep);
        } else {
            h.tvDeposit.setVisibility(View.GONE);
        }

        h.tvCreatedAt.setText("Created: " + a.createdAt);
    }

    @Override public int getItemCount() { return list == null ? 0 : list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvAmount, tvGst, tvBase, tvMethod, tvDeposit, tvCreatedAt;
        public VH(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvAmount      = itemView.findViewById(R.id.tvAmount);
            tvGst         = itemView.findViewById(R.id.tvGst);
            tvBase        = itemView.findViewById(R.id.tvBase);
            tvMethod      = itemView.findViewById(R.id.tvMethod);
            tvDeposit     = itemView.findViewById(R.id.tvDeposit);
            tvCreatedAt   = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
