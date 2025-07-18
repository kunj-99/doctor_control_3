package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONObject;

public class view_patient_report extends AppCompatActivity {

    private static final String GET_REPORT_URL = "https://thedoctorathome.in/get_medical_report.php?appointment_id=";

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

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appointmentId = getIntent().getStringExtra("appointment_id");
        if (appointmentId == null || appointmentId.isEmpty()) {
            Toast.makeText(this, "Appointment ID not provided", Toast.LENGTH_SHORT).show();
            redirectToHistoryFragment();
            return;
        }

        setContentView(R.layout.activity_view_patient_report);

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

        tvHospitalName.setText("VRAJ HOSPITAL");
        tvHospitalAddress.setText("150 Feet Ring Road, Rajkot - 360 004");

        requestQueue = Volley.newRequestQueue(this);
        fetchMedicalReport();
    }

    private void fetchMedicalReport() {
        String url = GET_REPORT_URL + appointmentId;

        loaderutil.showLoader(this);

        @SuppressLint("SetTextI18n") JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        String status = response.optString("status", "");
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

                        // Medications table
                        String[] medList = data.optString("medications", "").split("\\n");
                        String[] dosageList = data.optString("dosage", "").split("\\n");

                        TableLayout tableMedications = findViewById(R.id.table_medications);
                        if (tableMedications.getChildCount() > 1) {
                            tableMedications.removeViews(1, tableMedications.getChildCount() - 1);
                        }

                        int rowCount = Math.max(medList.length, dosageList.length);
                        for (int i = 0; i < rowCount; i++) {
                            TableRow row = new TableRow(this);

                            TextView tvNo = new TextView(this);
                            tvNo.setText(String.valueOf(i + 1));
                            tvNo.setPadding(4, 4, 4, 4);

                            TextView tvMed = new TextView(this);
                            tvMed.setText(i < medList.length ? medList[i].trim().replaceAll(",$", "") : "");
                            tvMed.setPadding(4, 4, 4, 4);

                            TextView tvDose = new TextView(this);
                            tvDose.setText(i < dosageList.length ? dosageList[i].trim().replaceAll(",$", "") : "");
                            tvDose.setPadding(4, 4, 4, 4);

                            row.addView(tvNo);
                            row.addView(tvMed);
                            row.addView(tvDose);
                            tableMedications.addView(row);
                        }

                    } catch (Exception e) {
                        Toast.makeText(view_patient_report.this, "Error parsing report data", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(view_patient_report.this, "Error fetching report", Toast.LENGTH_SHORT).show();
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
