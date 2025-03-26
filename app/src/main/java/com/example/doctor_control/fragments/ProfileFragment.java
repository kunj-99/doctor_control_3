package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.doctor_control.R;

import org.json.JSONException;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private EditText etFullName, etSpecialization, etQualification, etExperienceYears,
            etLicenseNumber, etHospitalAffiliation, etAvailabilitySchedule;

    private ImageView profileImage;
    private Button btnSaveProfile;

    private final String FETCH_URL = "http://sxm.a58.mytemp.website/Doctors/get_doctor.php";
    private final String UPDATE_URL = "http://sxm.a58.mytemp.website/Doctors/update_doctor.php";
    private final int doctorId = 10; // replace with dynamic id

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

        fetchDoctorData();

        btnSaveProfile.setOnClickListener(v -> updateDoctorData());

        return view;
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
                        if(response.has("error")){
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

                        String imageUrl = response.getString("profile_picture");
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
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, UPDATE_URL, jsonParams,
                response -> {
                    Toast.makeText(getContext(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                },
                error -> Toast.makeText(getContext(), "Update Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );

        queue.add(jsonRequest);
    }
}
