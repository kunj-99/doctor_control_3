package com.infowave.doctor_control.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragmentDBG";

    // Endpoints
    private static final String FETCH_URL = ApiConfig.endpoint("Doctors/get_doctor.php");

    // UI - Now using TextView instead of TextInputEditText
    private TextView etFullName, etSpecialization, etQualification, etExperienceYears,
            etLicenseNumber, etHospitalAffiliation, etAvailabilitySchedule;
    private ImageView profileImage;

    private RequestQueue queue;
    private int doctorId = -1;

    // Request tags (so we can cancel on destroy)
    private static final String TAG_REQ_FETCH = "req_fetch_doctor";

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

        // Bind views - Now TextViews
        etFullName = view.findViewById(R.id.et_full_name);
        etSpecialization = view.findViewById(R.id.et_specialization);
        etQualification = view.findViewById(R.id.et_qualification);
        etExperienceYears = view.findViewById(R.id.et_experience_years);
        etLicenseNumber = view.findViewById(R.id.et_license_number);
        etHospitalAffiliation = view.findViewById(R.id.et_hospital_affiliation);
        etAvailabilitySchedule = view.findViewById(R.id.et_availability_schedule);
        profileImage = view.findViewById(R.id.profile_image);

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            Log.d(TAG, "Logout clicked");
            logout();
        });

        if (doctorId == -1) {
            Log.e(TAG, "Invalid doctor id in SharedPreferences. Prompting user.");
            Toast.makeText(getContext(), "Invalid doctor id. Please login.", Toast.LENGTH_SHORT).show();
        }

        Log.d(TAG, "Calling fetchDoctorData()");
        fetchDoctorData();

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

                        // Set data to text views
                        setFieldText(etFullName, response.optString("full_name", "Not available"));
                        setFieldText(etSpecialization, response.optString("specialization", "Not specified"));
                        setFieldText(etQualification, response.optString("qualification", "Not specified"));
                        setFieldText(etExperienceYears, formatExperience(response.optString("experience_years", "")));
                        setFieldText(etLicenseNumber, response.optString("license_number", "Not available"));
                        setFieldText(etHospitalAffiliation, response.optString("hospital_affiliation", "Not specified"));
                        setFieldText(etAvailabilitySchedule, response.optString("availability_schedule", "Not specified"));

                        String imageUrl = response.optString("profile_picture", "");
                        Log.d(TAG, "Image URL: " + imageUrl);
                        if (!imageUrl.isEmpty() && !imageUrl.equals("null")) {
                            try {
                                Glide.with(requireContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.pr_ic_profile_placeholder)
                                        .error(R.drawable.pr_ic_profile_placeholder)
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
                    Toast.makeText(getContext(), "Network Error: " + msg, Toast.LENGTH_SHORT).show();
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

    private void setFieldText(TextView field, String text) {
        if (text == null || text.isEmpty() || text.equals("null")) {
            field.setText("Not available");
        } else {
            field.setText(text);
        }
    }

    private String formatExperience(String experience) {
        if (experience == null || experience.isEmpty() || experience.equals("null")) {
            return "Not specified";
        }
        try {
            int years = Integer.parseInt(experience);
            if (years == 0) {
                return "Fresh graduate";
            } else if (years == 1) {
                return "1 year";
            } else {
                return years + " years";
            }
        } catch (NumberFormatException e) {
            return experience + " years";
        }
    }
}