package com.infowave.doctor_control;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "OTPVerification";
    private static final String VERIFY_OTP_URL = "http://sxm.a58.mytemp.website/Doctors/verify_otp.php"; // API URL for doctors

    private Button continu;
    private EditText etOtp;
    private String mobileNumber;
    private SharedPreferences sharedPreferences; // For storing doctor data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        // Initialize SharedPreferences (using a dedicated name for doctor data)
        sharedPreferences = getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        Log.d(TAG, "SharedPreferences 'DoctorPrefs' initialized.");

        // Initialize views
        continu = findViewById(R.id.continu);
        etOtp = findViewById(R.id.etLoginInput);

        // Get mobile number from intent
        mobileNumber = getIntent().getStringExtra("mobile");

        if (mobileNumber == null || mobileNumber.isEmpty()) {
            Log.e(TAG, "Error: Mobile number is missing in intent!");
            finish();
            return;
        } else {
            Log.d(TAG, "Mobile number received: " + mobileNumber);
        }

        // Set up the continue button click listener
        continu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyOtp();
            }
        });
    }

    private void verifyOtp() {
        final String enteredOtp = etOtp.getText().toString().trim();

        if (enteredOtp.isEmpty()) {
            Log.w(TAG, "OTP field is empty.");
            Toast.makeText(otp_verification.this, "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Entered OTP: " + enteredOtp + " | Sending to API for verification");

        // Create a Volley POST request
        StringRequest request = new StringRequest(Request.Method.POST, VERIFY_OTP_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Server Response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");

                            if (success) {
                                Log.d(TAG, "OTP Verified Successfully!");
                                Toast.makeText(otp_verification.this, "Login Successful!", Toast.LENGTH_SHORT).show();

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

                                Log.d(TAG, "Doctor data saved: doctor_id=" + doctorId +
                                        ", full_name=" + fullName + ", specialization=" + specialization);

                                // Redirect to MainActivity (or your doctor's dashboard)
                                Intent intent = new Intent(otp_verification.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonObject.getString("message");
                                Log.w(TAG, "OTP Verification Failed: " + message);
                                Toast.makeText(otp_verification.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                            Toast.makeText(otp_verification.this, "Response error! Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = "Volley Error: " + error.toString();
                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null) {
                            errorMsg += ", Status Code: " + networkResponse.statusCode;
                            Log.e(TAG, "Network Response Data: " + new String(networkResponse.data));
                        }
                        Log.e(TAG, errorMsg);
                        Toast.makeText(otp_verification.this, "Server error! Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Send mobile number and OTP as POST parameters
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobileNumber);
                params.put("otp", enteredOtp);
                Log.d(TAG, "Sending request params: " + params.toString());
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        Log.d(TAG, "Adding request to Volley queue");
        requestQueue.add(request);
    }
}
