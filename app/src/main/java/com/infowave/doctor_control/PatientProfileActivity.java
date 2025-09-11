package com.infowave.doctor_control;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import org.json.JSONException;
import org.json.JSONObject;

public class PatientProfileActivity extends AppCompatActivity {

    // Views
    private ImageView profileImage;
    private TextView tvFullName, tvDob, tvGender, tvBloodGroup, tvAddress,
            tvMobile, tvEmail, tvEmergencyContact, tvMedicalHistory,
            tvAllergies, tvMedications;

    // Base URL for the profile API endpoint
    private static final String PROFILE_URL = ApiConfig.endpoint("get_profile.php?patient_id=");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_profile);

        initializeViews();
        loadPatientData();
    }

    private void initializeViews() {
        profileImage = findViewById(R.id.profile_image);
        tvFullName = findViewById(R.id.tv_full_name);
        tvDob = findViewById(R.id.tv_dob);
        tvGender = findViewById(R.id.tv_gender);
        tvBloodGroup = findViewById(R.id.tv_blood_group);
        tvAddress = findViewById(R.id.tv_address);
        tvMobile = findViewById(R.id.tv_mobile);
        tvEmail = findViewById(R.id.tv_email);
        tvEmergencyContact = findViewById(R.id.tv_emergency_contact);
        tvMedicalHistory = findViewById(R.id.tv_medical_history);
        tvAllergies = findViewById(R.id.tv_allergies);
        tvMedications = findViewById(R.id.tv_medications);
    }

    private void loadPatientData() {
        String patientId = getIntent().getStringExtra("patient_id");
        if (patientId == null || patientId.isEmpty()) {
            Toast.makeText(this, "Patient ID not provided", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = PROFILE_URL + patientId;
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        loaderutil.showLoader(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        String status = response.getString("status");
                        if (status.equals("success")) {
                            JSONObject data = response.getJSONObject("data");

                            tvFullName.setText(data.optString("full_name", "N/A"));
                            tvDob.setText(data.optString("date_of_birth", "N/A"));
                            tvGender.setText(data.optString("gender", "N/A"));
                            tvBloodGroup.setText(data.optString("blood_group", "N/A"));
                            tvAddress.setText(data.optString("address", "N/A"));
                            tvMobile.setText(data.optString("mobile", "N/A"));
                            tvEmail.setText(data.optString("email", "N/A"));

                            String emergencyName = data.optString("emergency_contact_name", "");
                            String emergencyNumber = data.optString("emergency_contact_number", "");
                            tvEmergencyContact.setText(
                                    (emergencyName.isEmpty() && emergencyNumber.isEmpty())
                                            ? "N/A" : (emergencyName + " - " + emergencyNumber));

                            tvMedicalHistory.setText(data.optString("medical_history", "N/A"));
                            tvAllergies.setText(data.optString("allergies", "N/A"));
                            tvMedications.setText(data.optString("current_medications", "N/A"));

                            String profilePicUrl = data.optString("profile_picture", "");
                            Glide.with(this)
                                    .load(profilePicUrl.isEmpty() ? R.drawable.pr_ic_profile_placeholder : profilePicUrl)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.pr_ic_profile_placeholder)
                                    .error(R.drawable.pr_ic_profile_placeholder)
                                    .into(profileImage);
                        } else {
                            Toast.makeText(this, response.optString("message", "Could not retrieve profile"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Unable to load profile details.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Could not load profile. Please check your connection.", Toast.LENGTH_SHORT).show();
                }
        );

        requestQueue.add(jsonObjectRequest);
    }
}
