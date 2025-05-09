package com.example.doctor_control; // Use your actual package name

import android.os.Bundle;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import com.example.doctor_control.adapter.PendingPaymentAdapter;
public class PendingPaymentActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<PaymentModel> paymentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_payment);

        listView = findViewById(R.id.list_pending_payment);
        paymentList = new ArrayList<>();

        // Example data - replace with real data from database or API
        paymentList.add(new PaymentModel("John Patel", R.drawable.plaseholder_error, "₹1200", "Transaction Pending"));
        paymentList.add(new PaymentModel("Riya Shah", R.drawable.plaseholder_error, "₹1500", "Transaction Pending"));
        paymentList.add(new PaymentModel("Amit Joshi", R.drawable.plaseholder_error, "₹950", "Transaction Pending"));

        // Initialize and set the adapter
        PendingPaymentAdapter adapter = new PendingPaymentAdapter(this, paymentList);
        listView.setAdapter(adapter);
    }
}
