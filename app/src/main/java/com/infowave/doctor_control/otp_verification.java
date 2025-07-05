package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class otp_verification extends AppCompatActivity {

    private static final String VERIFY_OTP_URL = "http://sxm.a58.mytemp.website/Doctors/verify_otp.php"; // API URL for doctors

    private Button continu, resend;
    private EditText etOtp;
    private String mobileNumber;
    private SharedPreferences sharedPreferences; // For storing doctor data

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize SharedPreferences (using a dedicated name for doctor data)
        sharedPreferences = getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);

        // Initialize views
        continu = findViewById(R.id.continu);
        etOtp = findViewById(R.id.etLoginInput);
        resend = findViewById(R.id.resend);

        // Get mobile number from intent
        mobileNumber = getIntent().getStringExtra("mobile");

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Toast.makeText(this, "Mobile number not found. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set up the continue button click listener
        continu.setOnClickListener(v -> verifyOtp());

        // Setup Resend OTP button functionality
        resend.setEnabled(false);  // Initially disable the resend button
        startResendOtpTimer();  // Start the countdown timer
        resend.setOnClickListener(v -> resendOtp());
    }

    /**
     * Starts a 100-second timer that updates the resend button text.
     * When the timer finishes, the resend button is enabled.
     */
    private void startResendOtpTimer() {
        new CountDownTimer(100 * 1000, 1000) { // 100 seconds with 1-second intervals
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                resend.setText("Resend OTP in " + millisUntilFinished / 1000 + "s");
            }

            @Override
            public void onFinish() {
                resend.setText("Resend OTP");
                resend.setEnabled(true);  // Enable the button once the timer finishes
            }
        }.start();
    }

    /**
     * Resends the OTP via an API call.
     */
    private void resendOtp() {
        // Disable the button immediately and restart timer
        resend.setEnabled(false);
        startResendOtpTimer();  // Restart timer

        String URL = "http://sxm.a58.mytemp.website/Doctors/login.php";  // Your API URL to resend OTP

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            Toast.makeText(otp_verification.this, "OTP sent! Check your messages.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(otp_verification.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(otp_verification.this, "Unexpected response from server.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(otp_verification.this, "Network error! Try again.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);  // Pass mobile number for OTP
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(otp_verification.this);
        requestQueue.add(stringRequest);
    }

    private void verifyOtp() {
        final String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Toast.makeText(this, "Please enter the OTP.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a Volley POST request
        StringRequest request = new StringRequest(Request.Method.POST, VERIFY_OTP_URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");

                        if (success) {
                            Toast.makeText(otp_verification.this, "Login successful!", Toast.LENGTH_SHORT).show();

                            // Extract doctor details from the response
                            int doctorId = jsonObject.getInt("doctor_id");
                            String fullName = jsonObject.getString("full_name");
                            String specialization = jsonObject.getString("specialization");

                            // Save doctor data in SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putInt("doctor_id", doctorId);
                            editor.putString("full_name", fullName);
                            editor.putString("specialization", specialization);
                            editor.putString("mobile", mobileNumber);
                            editor.apply();

                            // Redirect to MainActivity (or your doctor's dashboard)
                            Intent intent = new Intent(otp_verification.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = jsonObject.getString("message");
                            Toast.makeText(otp_verification.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(otp_verification.this, "Invalid response from server. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Toast.makeText(otp_verification.this, "Network error! Please try again.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Send mobile number and OTP as POST parameters
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                params.put("otp", enteredOtp);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }
}
