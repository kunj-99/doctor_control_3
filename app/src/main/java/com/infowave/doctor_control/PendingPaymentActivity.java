package com.infowave.doctor_control;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.infowave.doctor_control.adapter.PaymentSummaryAdapter;
import com.infowave.doctor_control.adapter.PendingPaymentAppointmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class PendingPaymentActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PaymentSummaryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_payment);

        // Optional: Handle window insets for edge-to-edge screens (can be omitted on latest Android)
        View decorView = getWindow().getDecorView();
        decorView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                int left = insets.getSystemWindowInsetLeft();
                int top = insets.getSystemWindowInsetTop();
                int right = insets.getSystemWindowInsetRight();
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(left, top, right, bottom);
                return insets.consumeSystemWindowInsets();
            }
        });

        recyclerView = findViewById(R.id.recyclerViewPaymentSummary);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<PaymentSummary> summaryList = getPendingSummaries(); // Your data source

        adapter = new PaymentSummaryAdapter(
                summaryList,
                summary -> {
                    // On Pay button clicked
                    double amount = (summary.receivedFromDoctor > 0) ? summary.receivedFromDoctor : summary.givenToDoctor;
                    Toast.makeText(this, "Paying ₹" + amount + " for Doctor ID " + summary.doctorId, Toast.LENGTH_SHORT).show();
                    // Call your payment flow here (UPI, wallet, etc)
                },
                doctorId -> {
                    // On Card clicked
                    showPendingPaymentAppointmentBottomSheet(doctorId);
                }
        );

        recyclerView.setAdapter(adapter);
    }

    // Sample pending payment summaries
    private List<PaymentSummary> getPendingSummaries() {
        List<PaymentSummary> list = new ArrayList<>();
        PaymentSummary s1 = new PaymentSummary();
        s1.summaryId = 9; s1.doctorId = 11; s1.settlementStatus = "Pending";
        s1.totalEarning = 1626.20F; s1.adminCut = 325.24; s1.doctorCut = 1300.96;
        s1.adjustmentAmount = 493.24; s1.givenToDoctor = 0; s1.receivedFromDoctor = 493.24;
        s1.notes = "Settlement from 2025-05-06 to 2025-05-11";
        s1.createdAt = "2025-05-11 09:39:13";
        list.add(s1);

        PaymentSummary s2 = new PaymentSummary();
        s2.summaryId = 10; s2.doctorId = 10; s2.settlementStatus = "Pending";
        s2.totalEarning = 6577.08F; s2.adminCut = 1315.42; s2.doctorCut = 5261.66;
        s2.adjustmentAmount = 1665.42; s2.givenToDoctor = 0; s2.receivedFromDoctor = 1665.42;
        s2.notes = "Settlement from 2025-05-06 to 2025-05-11";
        s2.createdAt = "2025-05-11 09:39:13";
        list.add(s2);

        return list;
    }

    // Show appointments with payment_status 'Pending' for given doctor
    private void showPendingPaymentAppointmentBottomSheet(int doctorId) {
        // Fetch all appointments for this doctor with payment_status 'Pending'
        List<PendingPaymentAppointment> pendingList = new ArrayList<>();
        for (PendingPaymentAppointment a : getAllPendingPaymentAppointments()) {
            if (a.doctorId == doctorId && "Pending".equalsIgnoreCase(a.paymentStatus)) {
                pendingList.add(a);
            }
        }

        // Inflate bottom sheet
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottomsheet_pending_payment_appointment, null);
        RecyclerView rv = sheetView.findViewById(R.id.rvPendingPaymentAppointments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new PendingPaymentAppointmentAdapter(pendingList));
        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        tvTitle.setText("Pending Payment Appointments (Doctor ID: " + doctorId + ")");

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.show();
    }

    // Example: Should return all pending payment appointments (replace with your actual data source)
    private List<PendingPaymentAppointment> getAllPendingPaymentAppointments() {
        List<PendingPaymentAppointment> list = new ArrayList<>();
        list.add(new PendingPaymentAppointment(125, 11, 2, "kunj", 689.95, "Pending", "2025-05-06 01:44:56"));
        list.add(new PendingPaymentAppointment(128, 10, 10, "Vansh", 515.00, "Pending", "2025-05-06 10:45:32"));
        // ... add all records (fetch from DB or API in real app)
        return list;
    }
}
