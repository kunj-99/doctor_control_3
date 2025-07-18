package com.infowave.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.login;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private EditText etFullName, etSpecialization, etQualification, etExperienceYears,
            etLicenseNumber, etHospitalAffiliation, etAvailabilitySchedule;

    private ImageView profileImage;
    private Button btnSaveProfile;
    private int doctorId;

    private static final int REQUEST_GALLERY = 2;
    private Bitmap selectedBitmap;
    private static final String FETCH_URL = "https://thedoctorathome.in/Doctors/get_doctor.php";
    private static final String UPDATE_URL = "https://thedoctorathome.in/Doctors/update_doctor.php";

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

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> logout());

        // Get doctorId from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = sharedPreferences.getInt("doctor_id", -1);

        if (doctorId == -1) {
            Toast.makeText(getContext(), "Invalid doctor id. Please login.", Toast.LENGTH_SHORT).show();
        }

        profileImage.setOnClickListener(v -> openGallery());
        fetchDoctorData();

        btnSaveProfile.setOnClickListener(v -> {
            if (validateFields()) {
                btnSaveProfile.setEnabled(false); // Prevent double-click
                updateDoctorData();
            }
        });

        return view;
    }

    private void logout() {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private boolean validateFields() {
        if (etFullName.getText().toString().trim().isEmpty()) {
            etFullName.setError("Full name required");
            return false;
        }
        // Add other field validations if needed
        return true;
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedImageUri);
                selectedBitmap = compressBitmap(selectedBitmap, 600); // Downscale large images for upload
                profileImage.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxSizePx) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSizePx / width, (float) maxSizePx / height);
        if (scale < 1.0f) {
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        return bitmap;
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
                        etFullName.setText(response.optString("full_name", ""));
                        etSpecialization.setText(response.optString("specialization", ""));
                        etQualification.setText(response.optString("qualification", ""));
                        etExperienceYears.setText(response.optString("experience_years", ""));
                        etLicenseNumber.setText(response.optString("license_number", ""));
                        etHospitalAffiliation.setText(response.optString("hospital_affiliation", ""));
                        etAvailabilitySchedule.setText(response.optString("availability_schedule", ""));

                        String imageUrl = response.optString("profile_picture", "");
                        if (!imageUrl.isEmpty()) {
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.pr_ic_profile_placeholder)
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.pr_ic_profile_placeholder);
                        }
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
            jsonParams.put("full_name", etFullName.getText().toString().trim());
            jsonParams.put("specialization", etSpecialization.getText().toString().trim());
            jsonParams.put("qualification", etQualification.getText().toString().trim());
            jsonParams.put("experience_years", etExperienceYears.getText().toString().trim());
            jsonParams.put("license_number", etLicenseNumber.getText().toString().trim());
            jsonParams.put("hospital_affiliation", etHospitalAffiliation.getText().toString().trim());
            jsonParams.put("availability_schedule", etAvailabilitySchedule.getText().toString().trim());

            if (selectedBitmap != null) {
                jsonParams.put("profile_picture", getStringImage(selectedBitmap));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, UPDATE_URL, jsonParams,
                response -> {
                    Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                    selectedBitmap = null; // Reset so the same image isn't re-uploaded
                },
                error -> {
                    Toast.makeText(getContext(), "Update Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                }
        );

        queue.add(jsonRequest);
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos); // Lower quality for upload efficiency
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }
}
