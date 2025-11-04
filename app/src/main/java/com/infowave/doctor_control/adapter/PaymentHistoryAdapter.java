package com.infowave.doctor_control.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.infowave.doctor_control.PaymentSummary;
import com.infowave.doctor_control.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PaymentHistoryAdapter extends BaseAdapter {

    private final Context context;

    // पूरा डेटा (मास्टर) और दिखने वाला डेटा (फ़िल्टर रिज़ल्ट)
    private final List<PaymentSummary> master = new ArrayList<>();
    private final List<PaymentSummary> display = new ArrayList<>();

    public PaymentHistoryAdapter(Context context, List<PaymentSummary> initial) {
        this.context = context;
        setData(initial);
    }

    /** सर्वर/फेच्ड डेटा सेट करें — master और display दोनों री-सेट */
    public void setData(List<PaymentSummary> data) {
        master.clear();
        if (data != null) master.addAll(data);

        display.clear();
        display.addAll(master);
        notifyDataSetChanged();
    }

    /** Live search filter: query खाली हो तो पूरा master दिखाएँ */
    public void filter(String query) {
        String q = (query == null ? "" : query.trim()).toLowerCase(Locale.ROOT);
        display.clear();

        if (q.isEmpty()) {
            display.addAll(master);
        } else {
            for (PaymentSummary s : master) {
                // आप जो चाहें वो फ़ील्ड्स जोड़ें
                String hay = (safe(s.notes) + " " + safe(s.createdAt) + " " +
                        s.summaryId + " " + s.doctorId).toLowerCase(Locale.ROOT);
                if (hay.contains(q)) {
                    display.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    private String safe(String x) { return x == null ? "" : x; }

    @Override public int getCount() { return display.size(); }
    @Override public Object getItem(int position) { return display.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VH h;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_payment_history, parent, false);
            h = new VH(convertView);
            convertView.setTag(h);
        } else {
            h = (VH) convertView.getTag();
        }

        PaymentSummary s = display.get(position);

        // Header chip: completion direction
        double adminToDoctor = s.givenToDoctor;
        double doctorToAdmin = s.receivedFromDoctor;

        if (adminToDoctor > 0) {
            h.tvStatusChip.setText("Completed (Admin → Doctor)");
            h.tvStatusChip.setTextColor(Color.parseColor("#2E7D32")); // green
        } else if (doctorToAdmin > 0) {
            h.tvStatusChip.setText("Completed (Doctor → Admin)");
            h.tvStatusChip.setTextColor(Color.parseColor("#1565C0")); // blue-ish
        } else {
            h.tvStatusChip.setText("Completed");
            h.tvStatusChip.setTextColor(Color.parseColor("#424242"));
        }

        // Main info
        h.tvDoctor.setText("Doctor ID: " + s.doctorId);
        h.tvCreatedAt.setText(s.createdAt);


        h.tvBase.setText(String.format(Locale.ROOT, "Base (ex-GST): ₹%.2f", s.totalBaseExGst));
        h.tvGst.setText(String.format(Locale.ROOT, "GST: ₹%.2f", s.totalGst));
        h.tvAdminCollected.setText(String.format(Locale.ROOT, "Admin Collected: ₹%.2f", s.adminCollectedTotal));
        h.tvDoctorCollected.setText(String.format(Locale.ROOT, "Doctor Collected: ₹%.2f", s.doctorCollectedTotal));

        h.tvAdminCut.setText(String.format(Locale.ROOT, "Admin Cut: ₹%.2f", s.adminCut));
        h.tvDoctorCut.setText(String.format(Locale.ROOT, "Doctor Cut: ₹%.2f", s.doctorCut));
        h.tvAdjust.setText(String.format(Locale.ROOT, "Adjustment: ₹%.2f", s.adjustmentAmount));

        h.tvGivenToDoctor.setText(String.format(Locale.ROOT, "Given To Doctor: ₹%.2f", s.givenToDoctor));
        h.tvReceivedFromDoctor.setText(String.format(Locale.ROOT, "Received From Doctor: ₹%.2f", s.receivedFromDoctor));

        // Footer meta (appointments)
        h.tvFooter.setText(
                String.format(Locale.ROOT, "Appointments: %d  |  Online: %d  |  Offline: %d",
                        s.appointmentCount, s.onlineAppointments, s.offlineAppointments)
        );

        // Header bar केवल पहले item पर
        h.headerBar.setVisibility(position == 0 ? View.VISIBLE : View.GONE);

        return convertView;
    }

    static class VH {
        View headerBar;
        TextView tvStatusChip, tvCreatedAt, tvDoctor, tvNotes;
        TextView tvBase, tvGst, tvAdminCollected, tvDoctorCollected;
        TextView tvAdminCut, tvDoctorCut, tvAdjust, tvGivenToDoctor, tvReceivedFromDoctor, tvFooter;

        VH(View v) {
            headerBar            = v.findViewById(R.id.header_section);
            tvStatusChip         = v.findViewById(R.id.tv_status_chip);
            tvCreatedAt          = v.findViewById(R.id.tv_created_at);
            tvDoctor             = v.findViewById(R.id.tv_doctor_id);
            tvNotes              = v.findViewById(R.id.tv_notes);

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
        }
    }
}
