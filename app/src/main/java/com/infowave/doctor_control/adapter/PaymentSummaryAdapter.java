package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.doctor_control.PaymentSummary;
import com.infowave.doctor_control.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentSummaryAdapter extends RecyclerView.Adapter<PaymentSummaryAdapter.ViewHolder> {

    private final List<PaymentSummary> summaryList = new ArrayList<>();

    public interface OnPayClickListener { void onPayClick(PaymentSummary summary); }
    public interface OnCardClickListener { void onCardClick(PaymentSummary summary); }

    private final OnPayClickListener onPayClickListener;
    private final OnCardClickListener onCardClickListener;

    public PaymentSummaryAdapter(List<PaymentSummary> initialData,
                                 OnPayClickListener onPayClickListener,
                                 OnCardClickListener onCardClickListener) {
        if (initialData != null) summaryList.addAll(initialData);
        this.onPayClickListener = onPayClickListener;
        this.onCardClickListener = onCardClickListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<PaymentSummary> data) {
        summaryList.clear();
        if (data != null) summaryList.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_payment, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint({"SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PaymentSummary s = summaryList.get(position);

        String createdAt = (s.createdAt == null) ? "" : s.createdAt;
        String notes = (s.notes == null) ? "" : s.notes;

        h.tvCreatedAt.setText(createdAt);
        h.tvDoctorId.setText("Doctor ID: " + s.doctorId);
        h.tvTotalEarning.setText(String.format(Locale.ROOT, "Base (ex-GST): ₹%.2f", s.totalBaseExGst));
        h.tvNotes.setText(notes);

        h.tvTotalGst.setText(String.format(Locale.ROOT, "GST: ₹%.2f", s.totalGst));
        h.tvAdminCollected.setText(String.format(Locale.ROOT, "Admin Collected: ₹%.2f", s.adminCollectedTotal));
        h.tvDoctorCollected.setText(String.format(Locale.ROOT, "Doctor Collected: ₹%.2f", s.doctorCollectedTotal));

        h.tvAdminCut.setText(String.format(Locale.ROOT, "Admin Cut: ₹%.2f", s.adminCut));
        h.tvDoctorCut.setText(String.format(Locale.ROOT, "Doctor Cut: ₹%.2f", s.doctorCut));
        h.tvAdjustmentAmount.setText(String.format(Locale.ROOT, "Adjustment: ₹%.2f", s.adjustmentAmount));

        h.tvGivenToDoctor.setText(String.format(Locale.ROOT, "Given To Doctor: ₹%.2f", s.givenToDoctor));
        h.tvReceivedFromDoctor.setText(String.format(Locale.ROOT, "Received From Doctor: ₹%.2f", s.receivedFromDoctor));

        double amtAdminToDoctor = s.givenToDoctor;       // Admin → Doctor
        double amtDoctorToAdmin = s.receivedFromDoctor;  // Doctor → Admin

        // Reset button (recycling safe)
        h.btnPay.setVisibility(View.VISIBLE);
        h.btnPay.setEnabled(true);
        h.btnPay.setClickable(true);
        h.btnPay.setAlpha(1f);
        h.btnPay.setOnClickListener(null);

        // Prefer Pay if both present (safety)
        if (amtDoctorToAdmin > 0) {
            h.tvSettlementStatus.setText("Pending • (Doctor → Admin)");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#EF6C00"));

            h.btnPay.setText(String.format(Locale.ROOT, "Pay Admin ₹%.2f", amtDoctorToAdmin));
            h.btnPay.setAlpha(1f);

            h.btnPay.setOnClickListener(v -> {
                int pos = h.getAdapterPosition(); // ✅ compatible
                if (pos == RecyclerView.NO_POSITION) return;
                if (onPayClickListener != null) onPayClickListener.onPayClick(summaryList.get(pos));
            });

        } else if (amtAdminToDoctor > 0) {
            h.tvSettlementStatus.setText("Pending • (Admin → Doctor)");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#2E7D32"));

            // ✅ Make it clickable to open proof screen
            h.btnPay.setText(String.format(Locale.ROOT, "Receive from Admin ₹%.2f", amtAdminToDoctor));
            h.btnPay.setAlpha(1f);

            h.btnPay.setOnClickListener(v -> {
                int pos = h.getAdapterPosition(); // ✅ compatible
                if (pos == RecyclerView.NO_POSITION) return;
                if (onPayClickListener != null) onPayClickListener.onPayClick(summaryList.get(pos));
            });

        } else {
            h.tvSettlementStatus.setText("Pending");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#616161"));
            h.btnPay.setVisibility(View.GONE);
        }

        // Card click: keep showing appointments bottom sheet
        h.itemView.setOnClickListener(v -> {
            int pos = h.getAdapterPosition(); // ✅ compatible
            if (pos == RecyclerView.NO_POSITION) return;
            if (onCardClickListener != null) onCardClickListener.onCardClick(summaryList.get(pos));
        });
    }

    @Override
    public int getItemCount() {
        return summaryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSettlementStatus, tvDoctorId, tvTotalEarning, tvAdminCut, tvDoctorCut,
                tvAdjustmentAmount, tvGivenToDoctor, tvReceivedFromDoctor, tvNotes, tvCreatedAt;
        TextView tvTotalGst, tvAdminCollected, tvDoctorCollected;
        Button btnPay;

        public ViewHolder(View itemView) {
            super(itemView);
            tvSettlementStatus   = itemView.findViewById(R.id.tvSettlementStatus);
            tvDoctorId           = itemView.findViewById(R.id.tvDoctorId);
            tvTotalEarning       = itemView.findViewById(R.id.tvTotalEarning);
            tvAdminCut           = itemView.findViewById(R.id.tvAdminCut);
            tvDoctorCut          = itemView.findViewById(R.id.tvDoctorCut);
            tvAdjustmentAmount   = itemView.findViewById(R.id.tvAdjustmentAmount);
            tvGivenToDoctor      = itemView.findViewById(R.id.tvGivenToDoctor);
            tvReceivedFromDoctor = itemView.findViewById(R.id.tvReceivedFromDoctor);
            tvNotes              = itemView.findViewById(R.id.tvNotes);
            tvCreatedAt          = itemView.findViewById(R.id.tvCreatedAt);
            btnPay               = itemView.findViewById(R.id.btnPay);

            tvTotalGst           = itemView.findViewById(R.id.tvTotalGst);
            tvAdminCollected     = itemView.findViewById(R.id.tvAdminCollected);
            tvDoctorCollected    = itemView.findViewById(R.id.tvDoctorCollected);
        }
    }
}
