package com.infowave.doctor_control;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.infowave.doctor_control.adapter.PaymentAdapter;
import java.util.ArrayList;

public class PaymentHistoryActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<PaymentModel> paymentList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);
        View decoreview = getWindow().getDecorView();
        decoreview.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View v, @NonNull WindowInsets insets) {
                int left = insets.getSystemWindowInsetLeft();
                int top = insets.getSystemWindowInsetTop();
                int right = insets.getSystemWindowInsetRight();
                int bottom = insets.getSystemWindowInsetBottom();
                v.setPadding(left,top,right,bottom);
                return insets.consumeSystemWindowInsets();
            }
        });
        listView = findViewById(R.id.list_payment);
        listView.setDivider(null); // Optional: remove ListView divider lines

        paymentList = new ArrayList<>();

        // Dummy Data for display
        paymentList.add(new PaymentModel("John Patel", R.drawable.plaseholder_error, "₹1200","complete"));
        paymentList.add(new PaymentModel("Riya Shah", R.drawable.plaseholder_error, "₹1500","complete"));
        paymentList.add(new PaymentModel("Amit Joshi", R.drawable.plaseholder_error, "₹950","complete"));

        if (paymentList.isEmpty()) {
            Toast.makeText(this, "No payment data available", Toast.LENGTH_SHORT).show();
        }

        Log.d("PaymentHistoryActivity", "Payment List Size: " + paymentList.size());

        PaymentAdapter adapter = new PaymentAdapter(this, paymentList);
        listView.setAdapter(adapter);
    }
}
