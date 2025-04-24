package com.example.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.doctor_control.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private EditText etFullName, etSpecialization, etQualification, etExperienceYears,
            etLicenseNumber, etHospitalAffiliation, etAvailabilitySchedule;

    private ImageView profileImage;
    private Button btnSaveProfile;

    private final String FETCH_URL = "http://sxm.a58.mytemp.website/Doctors/get_doctor.php";
    private final String UPDATE_URL = "http://sxm.a58.mytemp.website/Doctors/update_doctor.php";
    // Remove the static doctorId and declare as member variable
    private int doctorId;

    private static final int REQUEST_GALLERY = 2;
    private Bitmap selectedBitmap; // holds the newly selected image bitmap

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        etFullName = view.findViewById(R.id.et_full_name);
        etSpecialization = view.findViewById(R.id.et_specialization);
        etQualification = view.findViewById(R.id.et_qualification);
        etExperienceYears = view.findViewById(R.id.et_experience_years);
        etLicenseNumber = view.findViewById(R.id.et_license_number);
        etHospitalAffiliation = view.findViewById(R.id.et_hospital_affiliation);
        etAvailabilitySchedule = view.findViewById(R.id.et_availability_schedule);
        profileImage = view.findViewById(R.id.profile_image);
        btnSaveProfile = view.findViewById(R.id.btn_save_profile);

        // Retrieve the doctor_id from SharedPreferences dynamically
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = sharedPreferences.getInt("doctor_id", -1); // 0 is the default value if not found

        // Optional: Check if the doctorId is valid
        if (doctorId == -1) {
            Toast.makeText(getContext(), "Invalid doctor id. Please login.", Toast.LENGTH_SHORT).show();
        }

        // Set click listener to open gallery when profile image is clicked
        profileImage.setOnClickListener(v -> openGallery());

        fetchDoctorData();

        btnSaveProfile.setOnClickListener(v -> updateDoctorData());

        return view;
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedImageUri);
                // Update the profile image with the selected image
                profileImage.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchDoctorData() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("doctor_id", doctorId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, FETCH_URL, jsonParams,
                response -> {
                    try {
                        if (response.has("error")) {
                            Toast.makeText(getContext(), response.getString("error"), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        etFullName.setText(response.getString("full_name"));
                        etSpecialization.setText(response.getString("specialization"));
                        etQualification.setText(response.getString("qualification"));
                        etExperienceYears.setText(response.getString("experience_years"));
                        etLicenseNumber.setText(response.getString("license_number"));
                        etHospitalAffiliation.setText(response.getString("hospital_affiliation"));
                        etAvailabilitySchedule.setText(response.getString("availability_schedule"));

                        // Retrieve the profile image URL from the API response
                        String imageUrl = response.getString("profile_picture");
                        // Use Glide to load the image into the profileImage ImageView
                        Glide.with(requireContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.pr_ic_profile_placeholder)
                                .into(profileImage);

                    } catch (JSONException e) {
                        Toast.makeText(getContext(), "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(getContext(), "Volley Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(jsonRequest);
    }

    private void updateDoctorData() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("doctor_id", doctorId);
            jsonParams.put("full_name", etFullName.getText().toString());
            jsonParams.put("specialization", etSpecialization.getText().toString());
            jsonParams.put("qualification", etQualification.getText().toString());
            jsonParams.put("experience_years", etExperienceYears.getText().toString());
            jsonParams.put("license_number", etLicenseNumber.getText().toString());
            jsonParams.put("hospital_affiliation", etHospitalAffiliation.getText().toString());
            jsonParams.put("availability_schedule", etAvailabilitySchedule.getText().toString());

            // If a new image was selected, include it in the update
            if (selectedBitmap != null) {
                jsonParams.put("profile_picture", getStringImage(selectedBitmap));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, UPDATE_URL, jsonParams,
                response -> Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(getContext(), "Update Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(jsonRequest);
    }

    // Helper method to convert a Bitmap to a Base64 encoded string
    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
