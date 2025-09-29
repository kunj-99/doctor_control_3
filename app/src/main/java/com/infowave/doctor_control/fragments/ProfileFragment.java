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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.login;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragmentDBG";
    private static final int REQUEST_GALLERY = 2;
    private static final int REQ_READ_STORAGE = 1001;

    // Endpoints
    private static final String FETCH_URL  = ApiConfig.endpoint("Doctors/get_doctor.php");
    private static final String UPDATE_URL = ApiConfig.endpoint("Doctors/update_doctor.php");

    // UI
    private EditText etFullName, etSpecialization, etQualification, etExperienceYears,
            etLicenseNumber, etHospitalAffiliation, etAvailabilitySchedule;
    private ImageView profileImage;
    private Button btnSaveProfile;

    private RequestQueue queue;
    private Bitmap selectedBitmap;
    private int doctorId = -1;

    // Request tags (so we can cancel on destroy)
    private static final String TAG_REQ_FETCH  = "req_fetch_doctor";
    private static final String TAG_REQ_UPDATE = "req_update_doctor";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        queue = Volley.newRequestQueue(requireContext().getApplicationContext());

        SharedPreferences sp = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        doctorId = sp.getInt("doctor_id", -1);
        Log.d(TAG, "Loaded doctor_id from SharedPreferences: " + doctorId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() – inflating layout");
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Bind views
        etFullName             = view.findViewById(R.id.et_full_name);
        etSpecialization       = view.findViewById(R.id.et_specialization);
        etQualification        = view.findViewById(R.id.et_qualification);
        etExperienceYears      = view.findViewById(R.id.et_experience_years);
        etLicenseNumber        = view.findViewById(R.id.et_license_number);
        etHospitalAffiliation  = view.findViewById(R.id.et_hospital_affiliation);
        etAvailabilitySchedule = view.findViewById(R.id.et_availability_schedule);
        profileImage           = view.findViewById(R.id.profile_image);
        btnSaveProfile         = view.findViewById(R.id.btn_save_profile);

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout clicked");
            logout();
        });

        if (doctorId == -1) {
            Log.e(TAG, "Invalid doctor id in SharedPreferences. Prompting user.");
            Toast.makeText(getContext(), "Invalid doctor id. Please login.", Toast.LENGTH_SHORT).show();
        }

        profileImage.setOnClickListener(v -> {
            Log.d(TAG, "Profile image clicked: open gallery");
            openGallery();
        });

        Log.d(TAG, "Calling fetchDoctorData()");
        fetchDoctorData();

        btnSaveProfile.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked");
            if (validateFields()) {
                Log.d(TAG, "Validation success, disabling button and updating profile");
                btnSaveProfile.setEnabled(false);
                updateDoctorData();
            } else {
                Log.w(TAG, "Validation failed");
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView() – canceling Volley requests with tags");
        if (queue != null) {
            queue.cancelAll(TAG_REQ_FETCH);
            queue.cancelAll(TAG_REQ_UPDATE);
        }
    }

    private void logout() {
        Log.d(TAG, "Executing logout()");
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Toast.makeText(getContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private boolean validateFields() {
        String fullName = etFullName.getText().toString().trim();
        Log.d(TAG, "Validating fields: full_name=" + fullName);
        if (fullName.isEmpty()) {
            etFullName.setError("Full name required");
            return false;
        }
        // Add extra field validations here if needed, and log them
        return true;
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Log.d(TAG, "openGallery() – checking READ_EXTERNAL_STORAGE permission");
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_EXTERNAL_STORAGE not granted – requesting");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_READ_STORAGE);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        Log.d(TAG, "Starting gallery activity for result");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult(): requestCode=" + requestCode);
        if (requestCode == REQ_READ_STORAGE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "READ_EXTERNAL_STORAGE granted=" + granted);
            if (granted) openGallery();
            else Toast.makeText(getContext(), "Storage permission denied.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d(TAG, "onActivityResult(): requestCode=" + requestCode + ", resultCode=" + resultCode + ", data=" + (data != null));
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            Log.d(TAG, "Image URI selected: " + selectedImageUri);
            try {
                Bitmap raw = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedImageUri);
                Log.d(TAG, "Original bitmap: w=" + raw.getWidth() + ", h=" + raw.getHeight());
                selectedBitmap = compressBitmap(raw, 600);
                Log.d(TAG, "Compressed bitmap: w=" + selectedBitmap.getWidth() + ", h=" + selectedBitmap.getHeight());
                profileImage.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image", e);
                Toast.makeText(getContext(), "Failed to load image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "No image chosen or canceled");
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap, int maxSizePx) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = Math.min((float) maxSizePx / width, (float) maxSizePx / height);
        Log.d(TAG, "compressBitmap(): in=" + width + "x" + height + ", scale=" + scale);
        if (scale < 1.0f) {
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);
            Log.d(TAG, "Scaling to: " + newWidth + "x" + newHeight);
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }
        Log.d(TAG, "No scaling required");
        return bitmap;
    }

    private void fetchDoctorData() {
        Log.d(TAG, "fetchDoctorData(): url=" + FETCH_URL + " doctor_id=" + doctorId);
        if (doctorId <= 0) {
            Log.e(TAG, "fetchDoctorData(): invalid doctor_id, aborting request");
            Toast.makeText(getContext(), "Login required. Invalid doctor id.", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject jsonParams = new JSONObject();
        try {
            jsonParams.put("doctor_id", doctorId);
        } catch (JSONException e) {
            Log.e(TAG, "JSON build error (fetch params)", e);
        }
        Log.d(TAG, "fetchDoctorData() payload: " + jsonParams.toString());

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST, FETCH_URL, jsonParams,
                response -> {
                    Log.d(TAG, "fetchDoctorData() response: " + response);
                    try {
                        if (response.has("error")) {
                            String err = response.optString("error");
                            Log.w(TAG, "Server returned error: " + err);
                            Toast.makeText(getContext(), err, Toast.LENGTH_SHORT).show();
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
                        Log.d(TAG, "Image URL: " + imageUrl);
                        if (!imageUrl.isEmpty()) {
                            try {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.pr_ic_profile_placeholder)
                                        .into(profileImage);
                                Log.d(TAG, "Glide load success");
                            } catch (Exception e) {
                                Log.e(TAG, "Glide load failed", e);
                                profileImage.setImageResource(R.drawable.pr_ic_profile_placeholder);
                            }
                        } else {
                            profileImage.setImageResource(R.drawable.pr_ic_profile_placeholder);
                            Log.d(TAG, "No image URL; set placeholder");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "JSON parse error (fetch)", e);
                        Toast.makeText(getContext(), "JSON Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "fetchDoctorData() Volley error: " + error, error);
                    String msg = (error.getMessage() != null) ? error.getMessage() : error.toString();
                    Toast.makeText(getContext(), "Volley Error: " + msg, Toast.LENGTH_SHORT).show();
                }
        );

        jsonRequest.setTag(TAG_REQ_FETCH);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10s timeout
                1,     // 1 retry
                1.0f
        ));
        queue.add(jsonRequest);
        Log.d(TAG, "fetchDoctorData() request enqueued");
    }

    private void updateDoctorData() {
        Log.d(TAG, "updateDoctorData(): url=" + UPDATE_URL + " doctor_id=" + doctorId);
        if (doctorId <= 0) {
            Log.e(TAG, "updateDoctorData(): invalid doctor_id, aborting");
            Toast.makeText(getContext(), "Login required. Invalid doctor id.", Toast.LENGTH_SHORT).show();
            btnSaveProfile.setEnabled(true);
            return;
        }

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
                String base64 = getStringImage(selectedBitmap);
                Log.d(TAG, "Adding base64 image: length=" + (base64 != null ? base64.length() : 0));
                jsonParams.put("profile_picture", base64);
            } else {
                Log.d(TAG, "No image selected to upload");
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON build error (update params)", e);
        }

        Log.d(TAG, "updateDoctorData() payload: " + jsonParams.toString());

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST, UPDATE_URL, jsonParams,
                response -> {
                    Log.d(TAG, "updateDoctorData() response: " + response);
                    Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                    selectedBitmap = null; // reset
                },
                error -> {
                    Log.e(TAG, "updateDoctorData() Volley error: " + error, error);
                    String msg = (error.getMessage() != null) ? error.getMessage() : error.toString();
                    Toast.makeText(getContext(), "Update Error: " + msg, Toast.LENGTH_SHORT).show();
                    btnSaveProfile.setEnabled(true);
                }
        );

        jsonRequest.setTag(TAG_REQ_UPDATE);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000, // a bit higher for uploads
                1,
                1.0f
        ));
        queue.add(jsonRequest);
        Log.d(TAG, "updateDoctorData() request enqueued");
    }

    private String getStringImage(Bitmap bmp) {
        if (bmp == null) {
            Log.w(TAG, "getStringImage(): bitmap is null");
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean ok = bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        Log.d(TAG, "Bitmap compress ok=" + ok + ", byteSize=" + baos.size());
        byte[] imageBytes = baos.toByteArray();
        String b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        Log.d(TAG, "Base64 length=" + (b64 != null ? b64.length() : 0));
        return b64;
    }
}
