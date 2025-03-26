package com.example.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class view_patient_report extends AppCompatActivity {

    // API endpoint URL (update with your actual server URL)
    // This endpoint expects an "appointment_id" parameter.
    private static final String GET_REPORT_URL = "http://sxm.a58.mytemp.website/get_medical_report.php?appointment_id=";

    // Appointment ID retrieved from the Intent extra
    private String appointmentId;
    private TextView tvHospitalName, tvHospitalAddress;
    private TextView tvPatientName, tvPatientAddress, tvVisitDate;
    private TextView tvPatientAge, tvPatientWeight, tvPatientSex;
    private TextView tvTemperature, tvPulse, tvSpo2, tvBloodPressure, tvRespiratory;
    private TextView tvSymptoms, tvInvestigations;
    private TextView tvDoctorName;
    private ImageButton btnBack;
    private ImageView ivReportPhoto;

    private RequestQueue requestQueue;
    private static final String TAG = "MedicalReport";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check appointment ID BEFORE inflating the layout
        appointmentId = getIntent().getStringExtra("appointment_id");
        Log.d(TAG, "Received appointment ID: " + appointmentId);
        if (appointmentId == null || appointmentId.isEmpty()) {
            Toast.makeText(this, "Appointment ID not provided", Toast.LENGTH_SHORT).show();
            redirectToHistoryFragment();
            return;
        }

        // Inflate the layout only if appointmentId is present
        setContentView(R.layout.activity_view_patient_report);

        // Initialize UI elements
        tvHospitalName = findViewById(R.id.tv_hospital_name);
        tvHospitalAddress = findViewById(R.id.tv_hospital_address);
        tvPatientName = findViewById(R.id.tv_patient_name);
        tvPatientAddress = findViewById(R.id.tv_patient_address);
        tvVisitDate = findViewById(R.id.tv_visit_date);
        tvPatientAge = findViewById(R.id.tv_patient_age);
        tvPatientSex = findViewById(R.id.tv_patient_sex);
        tvPatientWeight = findViewById(R.id.tv_patient_weight);
        tvTemperature = findViewById(R.id.tv_temperature);
        tvPulse = findViewById(R.id.tv_pulse);
        tvSpo2 = findViewById(R.id.tv_spo2);
        tvBloodPressure = findViewById(R.id.tv_blood_pressure);
        tvRespiratory = findViewById(R.id.tv_respiratory);
        tvSymptoms = findViewById(R.id.tv_symptoms);
        tvInvestigations = findViewById(R.id.tv_investigations_content);
        tvDoctorName = findViewById(R.id.tv_doctor_name);
        ivReportPhoto = findViewById(R.id.iv_report_photo);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());

        // Optionally, set static header texts
        tvHospitalName.setText("VRAJ HOSPITAL");
        tvHospitalAddress.setText("150 Feet Ring Road, Rajkot - 360 004");

        // Initialize Volley RequestQueue
        requestQueue = Volley.newRequestQueue(this);

        // Fetch the medical report data using the appointment ID
        fetchMedicalReport();
    }

    private void fetchMedicalReport() {
        String url = GET_REPORT_URL + appointmentId;
        Log.d(TAG, "Fetching report from URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "API Response received: " + response.toString());

                        try {
                            String status = response.optString("status", "");
                            Log.d(TAG, "Response status: " + status);

                            // If the response is not success, redirect immediately
                            if (!status.equalsIgnoreCase("success")) {
                                Toast.makeText(view_patient_report.this, "Report not found", Toast.LENGTH_SHORT).show();
                                redirectToHistoryFragment();
                                return;
                            }

                            JSONObject data = response.optJSONObject("data");
                            if (data == null) {
                                Toast.makeText(view_patient_report.this, "Report not found", Toast.LENGTH_SHORT).show();
                                redirectToHistoryFragment();
                                return;
                            }

                            String photoUrl = data.optString("report_photo", "");
                            if (!photoUrl.isEmpty()) {
                                ivReportPhoto.setVisibility(View.VISIBLE);

                                // Hide everything else
                                findViewById(R.id.content_container).setVisibility(View.GONE);

                                Glide.with(view_patient_report.this)
                                        .load(photoUrl)
                                        .error(R.drawable.error)
                                        .into(ivReportPhoto);
                                return;
                            }


                            tvPatientName.setText("Name: " + data.optString("patient_name", "N/A"));
                            tvPatientAddress.setText("Address: " + data.optString("patient_address", "N/A"));
                            tvVisitDate.setText("Date: " + data.optString("visit_date", "N/A"));
                            tvTemperature.setText("Temperature: " + data.optString("temperature", "N/A"));
                            tvPatientAge.setText("Age: " + data.optString("age", "N/A") + " Years");
                            tvPatientWeight.setText("Weight: " + data.optString("weight", "N/A") + " kg");
                            tvPatientSex.setText("Sex: " + data.optString("sex", "N/A"));
                            tvPulse.setText("Pulse: " + data.optString("pulse", "N/A"));
                            tvSpo2.setText("SP02: " + data.optString("spo2", "N/A"));
                            tvBloodPressure.setText("Blood Pressure: " + data.optString("blood_pressure", "N/A"));
                            tvRespiratory.setText("Respiratory: " + data.optString("respiratory_system", "N/A"));
                            tvSymptoms.setText("Symptoms: " + data.optString("symptoms", "N/A"));
                            tvInvestigations.setText("Investigations: " + data.optString("investigations", "N/A"));
                            tvDoctorName.setText("Doctor: " + data.optString("doctor_name", "N/A"));

                            String medicationsStr = data.optString("medications", "");
                            String dosageStr = data.optString("dosage", "");

                            String[] medicationsArray = medicationsStr.split("\\n");
                            String[] dosageArray = dosageStr.split("\\n");

                            List<String> medList = new ArrayList<>();
                            for (String med : medicationsArray) {
                                med = med.trim();
                                if (!med.isEmpty()) {
                                    if (med.endsWith(",")) med = med.substring(0, med.length() - 1);
                                    medList.add(med);
                                }
                            }

                            List<String> dosageList = new ArrayList<>();
                            for (String dos : dosageArray) {
                                dos = dos.trim();
                                if (!dos.isEmpty()) {
                                    if (dos.endsWith(",")) dos = dos.substring(0, dos.length() - 1);
                                    dosageList.add(dos);
                                }
                            }

                            int rowCount = Math.max(medList.size(), dosageList.size());
                            TableLayout tableMedications = findViewById(R.id.table_medications);
                            if (tableMedications.getChildCount() > 1) {
                                tableMedications.removeViews(1, tableMedications.getChildCount() - 1);
                            }

                            for (int i = 0; i < rowCount; i++) {
                                TableRow row = new TableRow(view_patient_report.this);
                                TextView tvNo = new TextView(view_patient_report.this);
                                tvNo.setText(String.valueOf(i + 1));
                                tvNo.setTextSize(16);
                                tvNo.setPadding(4, 4, 4, 4);

                                TextView tvMedName = new TextView(view_patient_report.this);
                                tvMedName.setText(i < medList.size() ? medList.get(i) : "");
                                tvMedName.setTextSize(16);
                                tvMedName.setPadding(4, 4, 4, 4);

                                TextView tvDosage = new TextView(view_patient_report.this);
                                tvDosage.setText(i < dosageList.size() ? dosageList.get(i) : "");
                                tvDosage.setTextSize(16);
                                tvDosage.setPadding(4, 4, 4, 4);

                                row.addView(tvNo);
                                row.addView(tvMedName);
                                row.addView(tvDosage);
                                tableMedications.addView(row);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception during parsing: " + e.getMessage(), e);
                            Toast.makeText(view_patient_report.this, "Error parsing report data", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Volley error: " + error.getMessage(), error);
                        Toast.makeText(view_patient_report.this, "Error fetching report", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        requestQueue.add(request);
    }

    private void redirectToHistoryFragment() {
        Intent intent = new Intent(view_patient_report.this, MainActivity.class);
        intent.putExtra("open_fragment", 3);
        startActivity(intent);
        overridePendingTransition(1, 1);
        finish();
    }
}
