package com.infowave.doctor_control.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.infowave.doctor_control.PaymentSummary;
import com.infowave.doctor_control.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends BaseAdapter {

    private static final String TAG = "PaymentHistoryAdapter";

    // Chip filter modes
    public static final int FILTER_ALL = 0;
    public static final int FILTER_ADMIN_TO_DOCTOR = 1;
    public static final int FILTER_DOCTOR_TO_ADMIN = 2;

    public interface RowActionListener {
        /** Open bottom sheet for the given summary (card tap). */
        void onOpenSettlementDetails(PaymentSummary summary);

        /** Show Proof CTA: redirect to SettlementDetailsActivity. */
        void onShowProof(PaymentSummary summary);
    }

    private final Context context;
    private final RowActionListener listener;

    private final List<PaymentSummary> master = new ArrayList<>();
    private final List<PaymentSummary> display = new ArrayList<>();
    private int currentFilter = FILTER_ALL;

    public PaymentHistoryAdapter(Context context,
                                 List<PaymentSummary> initial,
                                 RowActionListener listener) {
        this.context = context;
        this.listener = listener;
        setData(initial);
    }

    /** Set fetched data — reset master & display using current filter */
    public void setData(List<PaymentSummary> data) {
        int prevMaster = master.size();
        int prevDisplay = display.size();

        master.clear();
        if (data != null) master.addAll(data);

        applyFilterInternal();

        Log.d(TAG, "setData: prevMaster=" + prevMaster + ", prevDisplay=" + prevDisplay
                + " -> newMaster=" + master.size() + ", newDisplay=" + display.size());
        if (!display.isEmpty()) {
            PaymentSummary first = display.get(0);
            Log.d(TAG, "setData: first summaryId=" + first.summaryId
                    + ", status=" + first.settlementStatus
                    + ", givenToDoctor=" + first.givenToDoctor
                    + ", receivedFromDoctor=" + first.receivedFromDoctor);
        }
    }

    /** Called by Activity when a chip is selected */
    public void setDirectionFilter(int filterMode) {
        if (filterMode != FILTER_ALL
                && filterMode != FILTER_ADMIN_TO_DOCTOR
                && filterMode != FILTER_DOCTOR_TO_ADMIN) {
            filterMode = FILTER_ALL;
        }
        if (currentFilter == filterMode) return;
        currentFilter = filterMode;
        Log.d(TAG, "setDirectionFilter: " + currentFilter);
        applyFilterInternal();
    }

    private void applyFilterInternal() {
        display.clear();
        for (PaymentSummary s : master) {
            double a2d = s.givenToDoctor;
            double d2a = s.receivedFromDoctor;

            boolean match;
            if (currentFilter == FILTER_ADMIN_TO_DOCTOR) {
                match = a2d > 0.0;
            } else if (currentFilter == FILTER_DOCTOR_TO_ADMIN) {
                match = d2a > 0.0;
            } else {
                match = true; // all
            }

            if (match) display.add(s);
        }
        notifyDataSetChanged();
        Log.d(TAG, "applyFilterInternal: display=" + display.size());
    }

    @Override public int getCount() { return display.size(); }
    @Override public Object getItem(int position) { return display.get(position); }
    @Override public long getItemId(int position) { return position; }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VH h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_payment_history, parent, false);
            h = new VH(convertView);
            convertView.setTag(h);
            Log.d(TAG, "getView: inflate position=" + position);
        } else {
            h = (VH) convertView.getTag();
        }

        PaymentSummary s = display.get(position);

        double adminToDoctor = s.givenToDoctor;       // Admin → Doctor
        double doctorToAdmin = s.receivedFromDoctor;  // Doctor → Admin

        // Status chip
        if (adminToDoctor > 0) {
            h.tvStatusChip.setText("Completed (Admin → Doctor)");
            h.tvStatusChip.setTextColor(Color.parseColor("#2E7D32"));
        } else if (doctorToAdmin > 0) {
            h.tvStatusChip.setText("Completed (Doctor → Admin)");
            h.tvStatusChip.setTextColor(Color.parseColor("#1565C0"));
        } else {
            h.tvStatusChip.setText("Completed");
            h.tvStatusChip.setTextColor(Color.parseColor("#424242"));
        }

        // Direction badge line
        if (adminToDoctor > 0) {
            h.tvDirectionBadge.setText(String.format(Locale.ROOT, "Admin \u2192 Doctor \u2022 ₹%.2f", adminToDoctor));
            h.tvDirectionBadge.setTextColor(Color.parseColor("#0D47A1"));
            h.tvDirectionBadge.setBackgroundColor(Color.parseColor("#E3F2FD"));
        } else if (doctorToAdmin > 0) {
            h.tvDirectionBadge.setText(String.format(Locale.ROOT, "Doctor \u2192 Admin \u2022 ₹%.2f", doctorToAdmin));
            h.tvDirectionBadge.setTextColor(Color.parseColor("#EF6C00"));
            h.tvDirectionBadge.setBackgroundColor(Color.parseColor("#FFF3E0"));
        } else {
            h.tvDirectionBadge.setText("Settlement");
            h.tvDirectionBadge.setTextColor(Color.parseColor("#455A64"));
            h.tvDirectionBadge.setBackgroundColor(Color.parseColor("#ECEFF1"));
        }

        h.tvCreatedAt.setText(s.createdAt);
        h.tvNotes.setText((s.notes == null || s.notes.trim().isEmpty()) ? "Settlement period…" : s.notes);

        h.tvBase.setText(String.format(Locale.ROOT, "Base (ex-GST): ₹%.2f", s.totalBaseExGst));
        h.tvGst.setText(String.format(Locale.ROOT, "GST: ₹%.2f", s.totalGst));
        h.tvAdminCollected.setText(String.format(Locale.ROOT, "Admin Collected: ₹%.2f", s.adminCollectedTotal));
        h.tvDoctorCollected.setText(String.format(Locale.ROOT, "Doctor Collected: ₹%.2f", s.doctorCollectedTotal));

        h.tvAdminCut.setText(String.format(Locale.ROOT, "Admin Cut: ₹%.2f", s.adminCut));
        h.tvDoctorCut.setText(String.format(Locale.ROOT, "Doctor Cut: ₹%.2f", s.doctorCut));
        h.tvAdjust.setText(String.format(Locale.ROOT, "Adjustment: ₹%.2f", s.adjustmentAmount));

        h.tvGivenToDoctor.setText(String.format(Locale.ROOT, "Given To Doctor: ₹%.2f", s.givenToDoctor));
        h.tvReceivedFromDoctor.setText(String.format(Locale.ROOT, "Received From Doctor: ₹%.2f", s.receivedFromDoctor));

        h.tvFooter.setText(String.format(Locale.ROOT,
                "Appointments: %d  |  Online: %d  |  Offline: %d",
                s.appointmentCount, s.onlineAppointments, s.offlineAppointments));

        // WHOLE CARD: bottom sheet (unchanged)
        convertView.setOnClickListener(v -> {
            if (listener != null) listener.onOpenSettlementDetails(s);
        });

        // SHOW PROOF BUTTON: redirect to SettlementDetailsActivity
        h.btnShowProof.setVisibility((adminToDoctor > 0) || (doctorToAdmin > 0) ? View.VISIBLE : View.GONE);
        h.btnShowProof.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowProof(s); // <-- changed to call onShowProof
            }
        });

        // Header bar only for first item
        h.headerBar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        if (position == 0 || position == display.size() - 1) {
            Log.d(TAG, "bind position=" + position
                    + " summaryId=" + s.summaryId
                    + " status=" + s.settlementStatus
                    + " A2D=" + adminToDoctor
                    + " D2A=" + doctorToAdmin);
        }

        return convertView;
    }

    static class VH {
        View headerBar;
        TextView tvStatusChip, tvCreatedAt, tvNotes;
        TextView tvDirectionBadge;
        TextView tvBase, tvGst, tvAdminCollected, tvDoctorCollected;
        TextView tvAdminCut, tvDoctorCut, tvAdjust, tvGivenToDoctor, tvReceivedFromDoctor, tvFooter;
        MaterialButton btnShowProof;

        VH(View v) {
            headerBar            = v.findViewById(R.id.header_section);
            tvStatusChip         = v.findViewById(R.id.tv_status_chip);
            tvCreatedAt          = v.findViewById(R.id.tv_created_at);
            tvNotes              = v.findViewById(R.id.tv_notes);
            tvDirectionBadge     = v.findViewById(R.id.tv_direction_badge);
            tvBase               = v.findViewById(R.id.tv_base_ex_gst);
            tvGst                = v.findViewById(R.id.tv_gst);
            tvAdminCollected     = v.findViewById(R.id.tv_admin_collected);
            tvDoctorCollected    = v.findViewById(R.id.tv_doctor_collected);
            tvAdminCut           = v.findViewById(R.id.tv_admin_cut);
            tvDoctorCut          = v.findViewById(R.id.tv_doctor_cut);
            tvAdjust             = v.findViewById(R.id.tv_adjustment);
            tvGivenToDoctor      = v.findViewById(R.id.tv_given_to_doctor);
            tvReceivedFromDoctor = v.findViewById(R.id.tv_received_from_doctor);
            tvFooter             = v.findViewById(R.id.tv_footer_meta);
            btnShowProof         = v.findViewById(R.id.btn_show_proof);
        }
    }
}
