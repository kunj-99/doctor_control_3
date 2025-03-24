package com.example.doctor_control;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    private static final String TAG = "DoctorLoginActivity";
    EditText etMobile;
    Button sendotp;
    SharedPreferences sharedPreferences;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences for doctor data
        sharedPreferences = getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);

        // Check if doctor_id is already stored
        if (sharedPreferences.contains("doctor_id") && sharedPreferences.getInt("doctor_id", -1) != -1) {
            // Doctor already logged in; redirect directly to MainActivity
            Intent intent = new Intent(login.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etMobile = findViewById(R.id.etMobileNumber);
        sendotp = findViewById(R.id.btnSendOtp);

        sendotp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String mobile = etMobile.getText().toString().trim();
                if (TextUtils.isEmpty(mobile) || mobile.length() != 10) {
                    etMobile.setError("Enter a valid 10-digit mobile number");
                    return;
                }
                checkDoctorMobile(mobile);
            }
        });
    }

    private void checkDoctorMobile(String mobile) {
        // URL to the doctor's PHP login file that sends the OTP.
        String URL = "http://sxm.a58.mytemp.website/Doctors/login.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            Toast.makeText(login.this, "OTP sent successfully. Proceeding to OTP verification.", Toast.LENGTH_SHORT).show();

                            // Pass the mobile number to the OTP verification activity
                            Intent intent = new Intent(login.this, otp_verification.class);
                            intent.putExtra("mobile", mobile);
                            startActivity(intent);
                        } else {
                            Toast.makeText(login.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing Error: " + e.getMessage(), e);
                    }
                },
                error -> {
                    Log.e(TAG, "Volley Error: " + error.getMessage(), error);
                    Toast.makeText(login.this, "Server error! Try again.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                // Send the mobile number as a POST parameter
                Map<String, String> params = new HashMap<>();
                params.put("mobile", mobile);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }
}
