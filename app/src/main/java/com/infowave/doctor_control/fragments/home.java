package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.ApiConfig;
import com.infowave.doctor_control.BackgroundService;
import com.infowave.doctor_control.PaymentHistoryActivity;
import com.infowave.doctor_control.PendingPaymentActivity;
import com.infowave.doctor_control.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class home extends Fragment {

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private SharedPreferences doctorPrefs;
    private String doctorId;
    CardView payment_history, cardPendingPayment;

    /** true => doctor is Active / Ongoing; false => Inactive */
    private volatile boolean isDoctorActive = false;

    /** guard to avoid showing popup when fragment is not in foreground/root */
    private final OnBackPressedCallback backCallback = new OnBackPressedCallback(true) {
        @Override public void handleOnBackPressed() {
            if (!isResumed() || !isVisible()) {
                // Not in foreground—let Activity handle it
                setEnabled(false);
                requireActivity().onBackPressed();
                setEnabled(true);
                return;
            }

            // If there is fragment back stack, pop it instead of showing exit popup
            if (requireActivity().getSupportFragmentManager().getBackStackEntryCount() > 0) {
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }

            // We are on the Home/root. Now decide exit behavior.
            if (isDoctorActive) {
                showExitConfirmDialog();
            } else {
                closeApp();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doctorPrefs = requireActivity().getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        payment_history = view.findViewById(R.id.card_payment_history);
        cardPendingPayment = view.findViewById(R.id.card_pending_payment);

        payment_history.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), PaymentHistoryActivity.class)));

        cardPendingPayment.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), PendingPaymentActivity.class)));

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

        // Start BG service
        requireContext().startService(new Intent(requireContext(), BackgroundService.class));

        // Register back handler (only once; lives with view lifecycle)
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireContext().stopService(new Intent(requireContext(), BackgroundService.class));
    }

    /* --------------------------- UI helpers --------------------------- */

    private void highlightCard(View root, int cardId, int imageId, int textId, int countId) {
        root.findViewById(cardId).setBackgroundColor(getResources().getColor(R.color.navy_blue));
        if (imageId != 0) ((ImageView) root.findViewById(imageId)).setColorFilter(getResources().getColor(R.color.white));
        if (textId != 0) ((TextView) root.findViewById(textId)).setTextColor(getResources().getColor(R.color.white));
        if (countId != 0) ((TextView) root.findViewById(countId)).setTextColor(getResources().getColor(R.color.white));
    }

    private void resetGridCardStyles() {
        View root = getView();
        if (root == null) return;
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
            @Override public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
                statusText.setText("Updating status...");
                statusToggle.setEnabled(false);
                new Handler().postDelayed(() -> updateDoctorStatus(isChecked, statusText, statusToggle, statusIcon), 500);
            }
        });
    }

    private void updateDoctorStatus(final boolean isActive, final TextView statusText,
                                    final Switch statusToggle, final ImageView statusIcon) {
        String urlDoctorStatus = ApiConfig.endpoint("Doctors/update_doctor_status.php");
        final String statusValue = isActive ? "active" : "inactive";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlDoctorStatus,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            statusText.setText(isActive ? "Active" : "Inactive");
                            statusIcon.setImageResource(isActive ? R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                            isDoctorActive = isActive;
                        } else {
                            statusText.setText(isActive ? "Inactive" : "Active");
                            statusToggle.setChecked(!isActive);
                            statusIcon.setImageResource(!isActive ? R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                            isDoctorActive = !isActive;
                        }
                    } catch (JSONException e) {
                        statusText.setText(isActive ? "Inactive" : "Active");
                        statusToggle.setChecked(!isActive);
                        statusIcon.setImageResource(!isActive ? R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                        isDoctorActive = !isActive;
                    } finally {
                        statusToggle.setEnabled(true);
                    }
                },
                error -> {
                    statusText.setText(!isActive ? "Inactive" : "Active");
                    statusToggle.setChecked(!isActive);
                    statusIcon.setImageResource(!isActive ? R.drawable.ic_active_status : R.drawable.ic_inactive_status);
                    statusToggle.setEnabled(true);
                    isDoctorActive = !isActive;
                }) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
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
        String urlFetchStatus = ApiConfig.endpoint("Doctors/get_doctor_status.php");

        StringRequest stringRequest = new StringRequest(Request.Method.POST, urlFetchStatus,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        boolean success = jsonObject.getBoolean("success");
                        if (success) {
                            String status = jsonObject.getString("status");
                            if ("Active".equalsIgnoreCase(status)) {
                                statusText.setText("Active");
                                statusToggle.setChecked(true);
                                statusToggle.setEnabled(true);
                                statusIcon.setImageResource(R.drawable.ic_active_status);
                                isDoctorActive = true;
                            } else if ("Inactive".equalsIgnoreCase(status)) {
                                statusText.setText("Inactive");
                                statusToggle.setChecked(false);
                                statusToggle.setEnabled(true);
                                statusIcon.setImageResource(R.drawable.ic_inactive_status);
                                isDoctorActive = false;
                            } else if ("Ongoing Appointment".equalsIgnoreCase(status)) {
                                statusText.setText("Ongoing Appointment");
                                statusToggle.setChecked(true);
                                statusToggle.setEnabled(false);
                                statusIcon.setImageResource(R.drawable.ic_inactive_status); // fallback icon
                                isDoctorActive = true;
                            } else {
                                statusText.setText(status);
                                statusToggle.setChecked(false);
                                statusToggle.setEnabled(true);
                                statusIcon.setImageResource(R.drawable.ic_inactive_status);
                                isDoctorActive = false;
                            }
                        }
                    } catch (JSONException e) {
                        statusText.setText("Inactive");
                        statusToggle.setChecked(false);
                        statusToggle.setEnabled(true);
                        statusIcon.setImageResource(R.drawable.ic_inactive_status);
                        isDoctorActive = false;
                    } finally {
                        attachStatusToggleListener(statusToggle, statusText, statusIcon);
                    }
                },
                error -> {
                    attachStatusToggleListener(statusToggle, statusText, statusIcon);
                    isDoctorActive = false;
                }
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }

    private void fetchCompletedCount() {
        String urlCompletedCount = ApiConfig.endpoint("completed_appointment.php");
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
                    } catch (JSONException ignored) { }
                },
                error -> { }
        );
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }

    private void fetchYourPatientsCount() {
        String urlYourPatientsCount = ApiConfig.endpoint("Doctors/complete_appointment_count.php");
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
                    } catch (JSONException ignored) { }
                },
                error -> { }
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }

    private void fetchOngoingAppointmentsCount() {
        String urlOngoingCount = ApiConfig.endpoint("Doctors/ongoing_appointment_count.php");
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
                    } catch (JSONException ignored) { }
                },
                error -> { }
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("doctor_id", doctorId);
                return params;
            }
        };
        Volley.newRequestQueue(requireContext()).add(stringRequest);
    }

    /* --------------------------- Exit Flow --------------------------- */

    private void showExitConfirmDialog() {
        if (!isAdded()) return;

        final int pad = dp(16), space8 = dp(8), space12 = dp(12);

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(pad, pad, pad, pad);

        LinearLayout titleRow = new LinearLayout(requireContext());
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setPadding(0, 0, 0, space12);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_active_status);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.navy_blue));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconLp.rightMargin = space8;
        titleRow.addView(icon, iconLp);

        TextView title = new TextView(requireContext());
        title.setText("Confirm Exit");
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(17);
        title.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_blue));
        titleRow.addView(title);

        TextView msg = new TextView(requireContext());
        msg.setText("You are currently Active. Do you want to go Inactive before exiting?");
        msg.setTextSize(15);
        msg.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));

        container.addView(titleRow);
        container.addView(msg);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(container)
                .setCancelable(true)
                .create();

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Exit", (d, w) -> {
            d.dismiss();
            closeApp();
        });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Make Inactive & Exit", (d, w) -> {
            d.dismiss();
            setDoctorInactiveThenExit();
        });

        dialog.show();

        Window win = dialog.getWindow();
        if (win != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            bg.setCornerRadius(dp(18));
            win.setBackgroundDrawable(bg);
        }

        try {
            TextView positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            TextView negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positive != null) {
                positive.setAllCaps(false);
                positive.setTypeface(Typeface.DEFAULT_BOLD);
                positive.setTextColor(ContextCompat.getColor(requireContext(), R.color.navy_blue));
                positive.setTextSize(15);
            }
            if (negative != null) {
                negative.setAllCaps(false);
                negative.setTextColor(ContextCompat.getColor(requireContext(), R.color.textSecondary));
                negative.setTextSize(15);
            }
        } catch (Exception ignored) {}
    }

    private void closeApp() {
        if (!isAdded()) return;
        requireActivity().finishAffinity();
    }

    private void setDoctorInactiveThenExit() {
        String urlDoctorStatus = ApiConfig.endpoint("Doctors/update_doctor_status.php");
        StringRequest req = new StringRequest(Request.Method.POST, urlDoctorStatus,
                response -> closeApp(),
                error -> closeApp()
        ) {
            @Override protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                p.put("status", "inactive");
                return p;
            }
        };
        Volley.newRequestQueue(requireContext()).add(req);
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return (int) (v * d + 0.5f);
    }
}
