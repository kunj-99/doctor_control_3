package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.BackgroundService;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.PaymentHistoryActivity;
import com.infowave.doctor_control.PendingPaymentActivity;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class home extends Fragment {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private SharedPreferences doctorPrefs;
    private String doctorId;
    CardView payment_history, cardPendingPayment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doctorPrefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        payment_history = view.findViewById(R.id.card_payment_history);
        cardPendingPayment = view.findViewById(R.id.card_pending_payment);

        payment_history.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), PaymentHistoryActivity.class));
        });

        cardPendingPayment.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), PendingPaymentActivity.class));
        });

        View layoutPatients = view.findViewById(R.id.layout_patients);
        View layoutYourPatients = view.findViewById(R.id.layout_your_patients);
        View layoutOngoing = view.findViewById(R.id.layout_ongoing);
        View layoutDoctorStatus = view.findViewById(R.id.layout_doctor_status);

        layoutPatients.setOnClickListener(v -> {
            resetGridCardStyles();
            highlightCard(view, R.id.layout_patients, R.id.img_patient, R.id.txt, R.id.patients_count);
        });

        layoutYourPatients.setOnClickListener(v -> {
            resetGridCardStyles();
            highlightCard(view, R.id.layout_your_patients, R.id.img_patient2, R.id.txt2, R.id.upcoming_appointments_count);
        });

        layoutOngoing.setOnClickListener(v -> {
            resetGridCardStyles();
            highlightCard(view, R.id.layout_ongoing, R.id.img_patient3, R.id.txt3, R.id.ongoing_appointments_count);
        });

        layoutDoctorStatus.setOnClickListener(v -> {
            resetGridCardStyles();
            highlightCard(view, R.id.layout_doctor_status, R.id.status_icon, R.id.status_text, 0);
        });

        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

        int tempId = doctorPrefs.getInt("doctor_id", -1);
        doctorId = tempId != -1 ? String.valueOf(tempId) : "";
        if (!doctorId.isEmpty()) {
            fetchYourPatientsCount();
            fetchOngoingAppointmentsCount();
        }
        fetchCompletedCount();

        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final Switch statusToggle = view.findViewById(R.id.status_toggle);
        final TextView statusText = view.findViewById(R.id.status_text);
        final ImageView statusIcon = view.findViewById(R.id.status_icon);

        if (!doctorId.isEmpty() && statusToggle != null && statusText != null && statusIcon != null) {
            fetchDoctorStatus(statusText, statusToggle, statusIcon);
        }

        Intent serviceIntent = new Intent(requireContext(), BackgroundService.class);
        requireContext().startService(serviceIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(requireContext(), BackgroundService.class);
        requireContext().stopService(serviceIntent);
    }

    private void highlightCard(View root, int cardId, int imageId, int textId, int countId) {
        root.findViewById(cardId).setBackgroundColor(getResources().getColor(R.color.navy_blue));
        if (imageId != 0) ((ImageView) root.findViewById(imageId)).setColorFilter(getResources().getColor(R.color.white));
        if (textId != 0) ((TextView) root.findViewById(textId)).setTextColor(getResources().getColor(R.color.white));
        if (countId != 0) ((TextView) root.findViewById(countId)).setTextColor(getResources().getColor(R.color.white));
    }

    private void resetGridCardStyles() {
        View root = getView();
        if (root == null) return;
        // Reset all cards
        int[] cardIds = {R.id.layout_patients, R.id.layout_your_patients, R.id.layout_ongoing, R.id.layout_doctor_status};
        int[] imageIds = {R.id.img_patient, R.id.img_patient2, R.id.img_patient3, R.id.status_icon};
        int[] textIds = {R.id.txt, R.id.txt2, R.id.txt3, R.id.status_text};
        int[] countIds = {R.id.patients_count, R.id.upcoming_appointments_count, R.id.ongoing_appointments_count, 0};

        for (int i = 0; i < cardIds.length; i++) {
            root.findViewById(cardIds[i]).setBackgroundColor(getResources().getColor(R.color.white));
            ((ImageView) root.findViewById(imageIds[i])).setColorFilter(getResources().getColor(R.color.navy_blue));
            ((TextView) root.findViewById(textIds[i])).setTextColor(getResources().getColor(R.color.textSecondary));
            if (countIds[i] != 0) ((TextView) root.findViewById(countIds[i])).setTextColor(getResources().getColor(R.color.navy_blue));
        }
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

    private void updateDoctorStatus(final boolean isActive, final TextView statusText,
                                    final Switch statusToggle, final ImageView statusIcon) {
        String urlDoctorStatus = "http://sxm.a58.mytemp.website/Doctors/update_doctor_status.php";
        final String statusValue = isActive ? "active" : "inactive";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlDoctorStatus,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            statusText.setText(isActive ? "Active" : "Inactive");
                            statusIcon.setImageResource(isActive ?
                                    R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                        } else {
                            statusText.setText(isActive ? "Inactive" : "Active");
                            statusToggle.setChecked(!isActive);
                            statusIcon.setImageResource(!isActive ?
                                    R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                        }
                    } catch (JSONException e) {
                        statusText.setText(isActive ? "Inactive" : "Active");
                        statusToggle.setChecked(!isActive);
                        statusIcon.setImageResource(!isActive ?
                                R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                    } finally {
                        statusToggle.setEnabled(true);
                    }
                },
                error -> {
                    statusText.setText(!isActive ? "Inactive" : "Active");
                    statusToggle.setChecked(!isActive);
                    statusIcon.setImageResource(!isActive ?
                            R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                    statusToggle.setEnabled(true);
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                params.put("status", statusValue);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void fetchDoctorStatus(final TextView statusText, final Switch statusToggle, final ImageView statusIcon) {
        String urlFetchStatus = "http://sxm.a58.mytemp.website/Doctors/get_doctor_status.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlFetchStatus,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            String status = jsonObject.getString("status");
                            if ("active".equalsIgnoreCase(status)) {
                                statusText.setText("Active");
                                statusToggle.setChecked(true);
                                statusIcon.setImageResource(R.drawable.ic_active_status);
                            } else {
                                statusText.setText("Inactive");
                                statusToggle.setChecked(false);
                                statusIcon.setImageResource(R.drawable.ic_inactive_status);
                            }
                        }
                    } catch (JSONException e) {
                        // no action; will use default UI state
                    } finally {
                        attachStatusToggleListener(statusToggle, statusText, statusIcon);
                    }
                },
                error -> attachStatusToggleListener(statusToggle, statusText, statusIcon)
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void fetchCompletedCount() {
        String urlCompletedCount = "http://sxm.a58.mytemp.website/completed_appointment.php";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlCompletedCount,
                response -> {
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
                    } catch (JSONException ignored) {}
                },
                error -> {}
        );
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void fetchYourPatientsCount() {
        String urlYourPatientsCount = "http://sxm.a58.mytemp.website/Doctors/complete_appointment_count.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlYourPatientsCount,
                response -> {
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
                    } catch (JSONException ignored) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }

    private void fetchOngoingAppointmentsCount() {
        String urlOngoingCount = "http://sxm.a58.mytemp.website/Doctors/ongoing_appointment_count.php";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlOngoingCount,
                response -> {
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
                    } catch (JSONException ignored) {}
                },
                error -> {}
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        RequestQueue requestQueue = Volley.newRequestQueue(requireContext());
        requestQueue.add(stringRequest);
    }
}
