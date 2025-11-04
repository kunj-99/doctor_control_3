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

    public void setData(List<PaymentSummary> data) {
        summaryList.clear();
        if (data != null) summaryList.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_payment, parent, false);
        return new ViewHolder(v);
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        PaymentSummary s = summaryList.get(position);

        // Header + meta
        h.tvCreatedAt.setText(s.createdAt);
        h.tvDoctorId.setText("Doctor ID: " + s.doctorId);
        h.tvTotalEarning.setText("Base (ex-GST): ₹" + String.format("%.2f", s.totalBaseExGst));
        h.tvNotes.setText(s.notes);

        // Transparency totals
        h.tvTotalGst.setText("GST: ₹" + String.format("%.2f", s.totalGst));
        h.tvAdminCollected.setText("Admin Collected: ₹" + String.format("%.2f", s.adminCollectedTotal));
        h.tvDoctorCollected.setText("Doctor Collected: ₹" + String.format("%.2f", s.doctorCollectedTotal));

        // Cuts & Adjustment
        h.tvAdminCut.setText("Admin Cut: ₹" + String.format("%.2f", s.adminCut));
        h.tvDoctorCut.setText("Doctor Cut: ₹" + String.format("%.2f", s.doctorCut));
        h.tvAdjustmentAmount.setText("Adjustment: ₹" + String.format("%.2f", s.adjustmentAmount));

        // Explicit credits/debits
        h.tvGivenToDoctor.setText("Given To Doctor: ₹" + String.format("%.2f", s.givenToDoctor));
        h.tvReceivedFromDoctor.setText("Received From Doctor: ₹" + String.format("%.2f", s.receivedFromDoctor));

        // ---- Business rules for button/status ----
        // amtAdminToDoctor > 0  => Admin → Doctor (doctor को पैसा लेना है) => Pay button DISABLED (Receive label)
        // amtDoctorToAdmin > 0  => Doctor → Admin (doctor को पैसा देना है)  => Pay button ENABLED
        // दोनों 0                => no dues => button GONE
        double amtAdminToDoctor = s.givenToDoctor;      // Admin → Doctor
        double amtDoctorToAdmin = s.receivedFromDoctor; // Doctor → Admin

        // Reset button baseline state
        h.btnPay.setEnabled(true);
        h.btnPay.setAlpha(1f);
        h.btnPay.setVisibility(View.VISIBLE);
        h.btnPay.setOnClickListener(null);

        if (amtAdminToDoctor > 0) {
            // Doctor will RECEIVE from Admin
            h.tvSettlementStatus.setText("Pending • (Admin → Doctor)");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#2E7D32")); // Green

            // Disabled button with "Receive" label (as per requirement)
            h.btnPay.setText("Receive from Admin ₹" + String.format("%.2f", amtAdminToDoctor));
            h.btnPay.setEnabled(false);
            h.btnPay.setAlpha(0.6f);          // subtle disabled look
            h.btnPay.setClickable(false);
            // No click action – doctor cannot pay in this direction

        } else if (amtDoctorToAdmin > 0) {
            // Doctor must PAY Admin
            h.tvSettlementStatus.setText("Pending • (Doctor → Admin)");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#EF6C00")); // Orange

            h.btnPay.setText("Pay Admin ₹" + String.format("%.2f", amtDoctorToAdmin));
            h.btnPay.setEnabled(true);
            h.btnPay.setAlpha(1f);
            h.btnPay.setClickable(true);
            h.btnPay.setOnClickListener(v -> {
                if (onPayClickListener != null) onPayClickListener.onPayClick(s);
            });

        } else {
            // No dues
            h.tvSettlementStatus.setText("Pending");
            h.tvSettlementStatus.setTextColor(Color.parseColor("#616161"));
            h.btnPay.setVisibility(View.GONE);
        }

        // Card click → open appointments included in this settlement
        h.itemView.setOnClickListener(v -> {
            if (onCardClickListener != null) onCardClickListener.onCardClick(s);
        });
    }

    @Override public int getItemCount() { return summaryList.size(); }

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

            // Totals
            tvTotalGst           = itemView.findViewById(R.id.tvTotalGst);
            tvAdminCollected     = itemView.findViewById(R.id.tvAdminCollected);
            tvDoctorCollected    = itemView.findViewById(R.id.tvDoctorCollected);
        }
    }
}
