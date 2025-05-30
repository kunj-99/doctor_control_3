package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.BackgroundService;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.PaymentHistoryActivity;  // Use the correct package name
import com.infowave.doctor_control.PendingPaymentActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class home extends Fragment {

    // Define a constant for the location permission request code.
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private SharedPreferences doctorPrefs;
    // Doctor ID is stored in "DoctorPrefs"
    private String doctorId;

//    ImageView terms, support;
    CardView payment_history,cardPendingPayment;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);



    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doctorPrefs = getActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
//        terms = view.findViewById(R.id.terms);  // Make sure the ID is correct
//        support = view.findViewById(R.id.support);  // Make sure the ID is correct
        payment_history = view.findViewById(R.id.card_payment_history);
        cardPendingPayment = view.findViewById(R.id.card_pending_payment);

//        terms.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), tarmsandcondition.class);
//            startActivity(intent);
//        });
//
//        support.setOnClickListener(v -> {
//            Intent intent = new Intent(getContext(), suppor.class);
//            startActivity(intent);
//        });

        payment_history.setOnClickListener(v -> {
            // Inside your Fragment (home.java)
            Intent intent = new Intent(getActivity(), PaymentHistoryActivity.class);
            startActivity(intent);
        });

        cardPendingPayment.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PendingPaymentActivity.class);
            startActivity(intent);
        });

        View layoutPatients = view.findViewById(R.id.layout_patients);
        View layoutYourPatients = view.findViewById(R.id.layout_your_patients);
        View layoutOngoing = view.findViewById(R.id.layout_ongoing);
        View layoutDoctorStatus = view.findViewById(R.id.layout_doctor_status);

        layoutPatients.setOnClickListener(v -> {
            resetGridCardStyles();
            layoutPatients.setBackgroundColor(getResources().getColor(R.color.navy_blue));
            ((ImageView) view.findViewById(R.id.img_patient)).setColorFilter(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.txt)).setTextColor(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.patients_count)).setTextColor(getResources().getColor(R.color.white));
        });

        layoutYourPatients.setOnClickListener(v -> {
            resetGridCardStyles();
            layoutYourPatients.setBackgroundColor(getResources().getColor(R.color.navy_blue));
            ((ImageView) view.findViewById(R.id.img_patient2)).setColorFilter(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.txt2)).setTextColor(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.upcoming_appointments_count)).setTextColor(getResources().getColor(R.color.white));
        });

        layoutOngoing.setOnClickListener(v -> {
            resetGridCardStyles();
            layoutOngoing.setBackgroundColor(getResources().getColor(R.color.navy_blue));
            ((ImageView) view.findViewById(R.id.img_patient3)).setColorFilter(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.txt3)).setTextColor(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.ongoing_appointments_count)).setTextColor(getResources().getColor(R.color.white));
        });

        layoutDoctorStatus.setOnClickListener(v -> {
            resetGridCardStyles();
            layoutDoctorStatus.setBackgroundColor(getResources().getColor(R.color.navy_blue));
            ((ImageView) view.findViewById(R.id.status_icon)).setColorFilter(getResources().getColor(R.color.white));
            ((TextView) view.findViewById(R.id.status_text)).setTextColor(getResources().getColor(R.color.white));
        });



        // Check for location permission. Make sure to use requireContext() or getContext() instead of 'this'.
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        // Log all SharedPreferences key/value pairs for debugging
        for (Map.Entry<String, ?> entry : doctorPrefs.getAll().entrySet()) {
            Log.d("DoctorPrefs", entry.getKey() + " : " + entry.getValue());
        }

        // Retrieve doctor_id from DoctorPrefs (stored as an integer, so we convert it to String)
        int tempId = doctorPrefs.getInt("doctor_id", -1);
        if (tempId != -1) {
            doctorId = String.valueOf(tempId);
        } else {
            doctorId = "";
        }

        if (!doctorId.isEmpty()) {
            Log.d("home", "Doctor ID retrieved: " + doctorId);
            // Call the doctor-specific methods
            fetchYourPatientsCount();
            fetchOngoingAppointmentsCount();
        } else {
            Log.e("home", "Doctor ID not found in SharedPreferences, skipping doctor-specific methods");
        }

        // Call the common method (no parameters required)
        fetchCompletedCount();

        // Retrieve UI elements
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final Switch statusToggle = view.findViewById(R.id.status_toggle);
        final TextView statusText = view.findViewById(R.id.status_text);
        final ImageView statusIcon = view.findViewById(R.id.status_icon);

        // First, fetch the current doctor status from the database.
        // Once fetched, attach the toggle listener.
        if (!doctorId.isEmpty() && statusToggle != null && statusText != null && statusIcon != null) {
            fetchDoctorStatus(statusText, statusToggle, statusIcon);
        }

        // Start the service to keep the doctor active
        Intent serviceIntent = new Intent(getContext(), BackgroundService.class);
        getContext().startService(serviceIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the service when the fragment is destroyed
        Intent serviceIntent = new Intent(getContext(), BackgroundService.class);
        getContext().stopService(serviceIntent);
    }
    private void resetGridCardStyles() {
        if (getView() == null) return;

        // Reset 1st card
        View layout1 = getView().findViewById(R.id.layout_patients);
        layout1.setBackgroundColor(getResources().getColor(R.color.white));
        ((ImageView) getView().findViewById(R.id.img_patient)).setColorFilter(getResources().getColor(R.color.navy_blue));
        ((TextView) getView().findViewById(R.id.txt)).setTextColor(getResources().getColor(R.color.textSecondary));
        ((TextView) getView().findViewById(R.id.patients_count)).setTextColor(getResources().getColor(R.color.navy_blue));

        // Reset 2nd card
        View layout2 = getView().findViewById(R.id.layout_your_patients);
        layout2.setBackgroundColor(getResources().getColor(R.color.white));
        ((ImageView) getView().findViewById(R.id.img_patient2)).setColorFilter(getResources().getColor(R.color.navy_blue));
        ((TextView) getView().findViewById(R.id.txt2)).setTextColor(getResources().getColor(R.color.textSecondary));
        ((TextView) getView().findViewById(R.id.upcoming_appointments_count)).setTextColor(getResources().getColor(R.color.navy_blue));

        // Reset 3rd card
        View layout3 = getView().findViewById(R.id.layout_ongoing);
        layout3.setBackgroundColor(getResources().getColor(R.color.white));
        ((ImageView) getView().findViewById(R.id.img_patient3)).setColorFilter(getResources().getColor(R.color.navy_blue));
        ((TextView) getView().findViewById(R.id.txt3)).setTextColor(getResources().getColor(R.color.textSecondary));
        ((TextView) getView().findViewById(R.id.ongoing_appointments_count)).setTextColor(getResources().getColor(R.color.navy_blue));

        // Reset 4th card
        View layout4 = getView().findViewById(R.id.layout_doctor_status);
        layout4.setBackgroundColor(getResources().getColor(R.color.white));
        ((ImageView) getView().findViewById(R.id.status_icon)).setColorFilter(getResources().getColor(R.color.navy_blue));
        ((TextView) getView().findViewById(R.id.status_text)).setTextColor(getResources().getColor(R.color.textSecondary));
    }

    private void attachStatusToggleListener(@NonNull final Switch statusToggle, final TextView statusText, final ImageView statusIcon) {
        statusToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                statusText.setText("Updating status...");
                statusToggle.setEnabled(false);
                new Handler().postDelayed(() -> updateDoctorStatus(isChecked, statusText, statusToggle, statusIcon), 500);
            }
        });
    }

    /**
     * This method sends a request to update the doctor's status.
     */
    private void updateDoctorStatus(final boolean isActive, final TextView statusText,
                                    @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch statusToggle, final ImageView statusIcon) {
        String urlDoctorStatus = "http://sxm.a58.mytemp.website/Doctors/update_doctor_status.php";
        final String statusValue = isActive ? "active" : "inactive";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlDoctorStatus,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("home", "Doctor status update response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if (success) {
                                // Successfully updated; set text and icon accordingly.
                                statusText.setText(isActive ? "Active" : "Inactive");
                                statusIcon.setImageResource(isActive ?
                                        R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                            } else {
                                // Update failed on server side, revert switch.
                                statusText.setText(isActive ? "Inactive" : "Active");
                                statusToggle.setChecked(!isActive);
                                statusIcon.setImageResource(!isActive ?
                                        R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                                Log.e("home", "Doctor status update failed: " + jsonObject.optString("message"));
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error in updateDoctorStatus: " + e.getMessage());
                            // On exception, revert UI changes.
                            statusText.setText(isActive ? "Inactive" : "Active");
                            statusToggle.setChecked(!isActive);
                            statusIcon.setImageResource(!isActive ?
                                    R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                        } finally {
                            // Re-enable the switch in all cases.
                            statusToggle.setEnabled(true);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Error updating doctor status: " + error.getMessage());
                        // On error, revert UI changes.
                        statusText.setText(!isActive ? "Inactive" : "Active");
                        statusToggle.setChecked(!isActive);
                        statusIcon.setImageResource(!isActive ?
                                R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                        statusToggle.setEnabled(true);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                params.put("status", statusValue);
                Log.d("home", "Sending update: doctor_id = " + doctorId + ", status = " + statusValue);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    private void fetchDoctorStatus(final TextView statusText, @SuppressLint("UseSwitchCompatOrMaterialCode") final Switch statusToggle, final ImageView statusIcon) {
        String urlFetchStatus = "http://sxm.a58.mytemp.website/Doctors/get_doctor_status.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlFetchStatus,
                new Response.Listener<String>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onResponse(String response) {
                        Log.d("home", "Doctor status fetch response: " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            boolean success = jsonObject.getBoolean("success");
                            if (success) {
                                // Get the status from the response. Expected values: "active" or "inactive"
                                String status = jsonObject.getString("status");
                                if (status.equalsIgnoreCase("active")) {
                                    statusText.setText("Active");
                                    statusToggle.setChecked(true);
                                    statusIcon.setImageResource(R.drawable.ic_active_status);
                                } else {
                                    statusText.setText("Inactive");
                                    statusToggle.setChecked(false);
                                    statusIcon.setImageResource(R.drawable.ic_inactive_status);
                                }
                            } else {
                                Log.e("home", "Failed to fetch doctor status: " + jsonObject.optString("message"));
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error in fetchDoctorStatus: " + e.getMessage());
                        } finally {
                            // Now attach the listener so that programmatic changes during initialization do not trigger it.
                            attachStatusToggleListener(statusToggle, statusText, statusIcon);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Error fetching doctor status: " + error.getMessage());
                        // Even on error, attach the listener (default state from layout applies).
                        attachStatusToggleListener(statusToggle, statusText, statusIcon);
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    private void fetchCompletedCount() {
        String urlCompletedCount = "http://sxm.a58.mytemp.website/completed_appointment.php";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlCompletedCount,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("home", "Server response (completed): " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int completedCount = jsonObject.getInt("completed_count");
                            View rootView = getView();
                            if (rootView != null) {
                                TextView completedCountTextView = rootView.findViewById(R.id.patients_count);
                                if (completedCountTextView != null) {
                                    completedCountTextView.setText(String.valueOf(completedCount));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error in fetchCompletedCount: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Volley error in fetchCompletedCount: " + error.getMessage());
                    }
                });

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void fetchYourPatientsCount() {
        String urlYourPatientsCount = "http://sxm.a58.mytemp.website/Doctors/complete_appointment_count.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlYourPatientsCount,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("home", "Server response (doctor completed): " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int completedCount = jsonObject.getInt("completed_count");
                            View rootView = getView();
                            if (rootView != null) {
                                TextView yourPatientsCountTextView = rootView.findViewById(R.id.upcoming_appointments_count);
                                if (yourPatientsCountTextView != null) {
                                    yourPatientsCountTextView.setText(String.valueOf(completedCount));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error in fetchYourPatientsCount: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Volley error in fetchYourPatientsCount: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                Log.d("home", "Sending doctor_id in fetchYourPatientsCount: " + doctorId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }

    private void fetchOngoingAppointmentsCount() {
        String urlOngoingCount = "http://sxm.a58.mytemp.website/Doctors/ongoing_appointment_count.php";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlOngoingCount,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("home", "Server response (ongoing): " + response);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int ongoingCount = jsonObject.getInt("ongoing_count");
                            View rootView = getView();
                            if (rootView != null) {
                                TextView ongoingCountTextView = rootView.findViewById(R.id.ongoing_appointments_count);
                                if (ongoingCountTextView != null) {
                                    ongoingCountTextView.setText(String.valueOf(ongoingCount));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error in fetchOngoingAppointmentsCount: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Volley error in fetchOngoingAppointmentsCount: " + error.getMessage());
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                Log.d("home", "Sending doctor_id in fetchOngoingAppointmentsCount: " + doctorId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(stringRequest);
    }
}
