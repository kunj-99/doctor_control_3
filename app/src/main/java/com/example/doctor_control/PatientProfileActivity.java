package com.example.doctor_control;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class PatientProfileActivity extends AppCompatActivity {

    // Views
    private ImageView profileImage;
    private TextView tvFullName, tvDob, tvGender, tvBloodGroup, tvAddress,
            tvMobile, tvEmail, tvEmergencyContact, tvMedicalHistory,
            tvAllergies, tvMedications;

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
        // Set patient details (Replace with actual API/database data)
        tvFullName.setText("Sarah Johnson");
        tvDob.setText("March 15, 1985");
        tvGender.setText("Female");
        tvBloodGroup.setText("AB+");
        tvAddress.setText("456 Health Avenue\nWellness City, HC 12345");
        tvMobile.setText("+1 (555) 123-4567");
        tvEmail.setText("sarah.j@medicalmail.com");
        tvEmergencyContact.setText("John Johnson - +1 (555) 765-4321");
        tvMedicalHistory.setText("• Hypertension (2018)\n• Appendectomy (2015)");
        tvAllergies.setText("• Penicillin\n• Shellfish");
        tvMedications.setText("• Lisinopril 10mg daily\n• Metformin 500mg twice daily");

        // Load profile image using Glide
        Glide.with(this)
                .load(R.drawable.pr_ic_profile_placeholder) // Change to actual URL if needed
                .apply(RequestOptions.circleCropTransform())
                .placeholder(R.drawable.pr_ic_profile_placeholder)
                .error(R.drawable.pr_ic_profile_placeholder)
                .into(profileImage);
    }



    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
