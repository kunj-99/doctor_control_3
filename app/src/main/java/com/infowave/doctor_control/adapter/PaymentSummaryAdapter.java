package com.infowave.doctor_control.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.PaymentSummary;
import com.infowave.doctor_control.R;

import java.util.List;

public class PaymentSummaryAdapter extends RecyclerView.Adapter<PaymentSummaryAdapter.ViewHolder> {

    private List<PaymentSummary> summaryList;
    private OnPayClickListener onPayClickListener;
    private OnCardClickListener onCardClickListener;

    // Interface for Pay button click
    public interface OnPayClickListener {
        void onPayClick(PaymentSummary summary);
    }

    // Interface for Card click
    public interface OnCardClickListener {
        void onCardClick(int doctorId);
    }

    // Adapter constructor
    public PaymentSummaryAdapter(List<PaymentSummary> summaryList,
                                 OnPayClickListener onPayClickListener,
                                 OnCardClickListener onCardClickListener) {
        this.summaryList = summaryList;
        this.onPayClickListener = onPayClickListener;
        this.onCardClickListener = onCardClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_payment, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentSummary summary = summaryList.get(position);

        holder.tvSettlementStatus.setText(summary.settlementStatus);
        holder.tvDoctorId.setText("Doctor ID: " + summary.doctorId);
        holder.tvTotalEarning.setText("Total Earning: ₹" + summary.totalEarning);
        holder.tvAdminCut.setText("Admin Cut: ₹" + summary.adminCut);
        holder.tvDoctorCut.setText("Doctor Cut: ₹" + summary.doctorCut);
        holder.tvAdjustmentAmount.setText("Adjustment: ₹" + summary.adjustmentAmount);
        holder.tvGivenToDoctor.setText("Given To Doctor: ₹" + summary.givenToDoctor);
        holder.tvReceivedFromDoctor.setText("Received From Doctor: ₹" + summary.receivedFromDoctor);
        holder.tvNotes.setText(summary.notes);
        holder.tvCreatedAt.setText(summary.createdAt);

        // Show Pay button with amount
        double amountToPay = (summary.receivedFromDoctor > 0) ? summary.receivedFromDoctor : summary.givenToDoctor;
        holder.btnPay.setText("Pay ₹" + String.format("%.2f", amountToPay));

        // Pay button click listener
        holder.btnPay.setOnClickListener(v -> {
            if (onPayClickListener != null) {
                onPayClickListener.onPayClick(summary);
            }
        });

        // Card click listener
        holder.itemView.setOnClickListener(v -> {
            if (onCardClickListener != null) {
                onCardClickListener.onCardClick(summary.doctorId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSettlementStatus, tvDoctorId, tvTotalEarning, tvAdminCut, tvDoctorCut,
                tvAdjustmentAmount, tvGivenToDoctor, tvReceivedFromDoctor, tvNotes, tvCreatedAt;
        Button btnPay;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSettlementStatus = itemView.findViewById(R.id.tvSettlementStatus);
            tvDoctorId = itemView.findViewById(R.id.tvDoctorId);
            tvTotalEarning = itemView.findViewById(R.id.tvTotalEarning);
            tvAdminCut = itemView.findViewById(R.id.tvAdminCut);
            tvDoctorCut = itemView.findViewById(R.id.tvDoctorCut);
            tvAdjustmentAmount = itemView.findViewById(R.id.tvAdjustmentAmount);
            tvGivenToDoctor = itemView.findViewById(R.id.tvGivenToDoctor);
            tvReceivedFromDoctor = itemView.findViewById(R.id.tvReceivedFromDoctor);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnPay = itemView.findViewById(R.id.btnPay);
        }
    }
}
