package com.example.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.DistanceCalculator;
import com.example.doctor_control.LiveLocationManager;
import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aOngoingAdapter;
import com.example.doctor_control.medical_report;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class aOngoingFragment extends Fragment {

    private static final String TAG = "aOngoingFragment";
    private static final String LIVE_LOCATION_URL = "http://sxm.a58.mytemp.website/update_live_location.php";
    private static final long APPT_REFRESH_MS = 5000;

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private RequestQueue queue;

    private final ArrayList<String> appointmentIds = new ArrayList<>();
    private final ArrayList<String> patientNames = new ArrayList<>();
    private final ArrayList<String> problems = new ArrayList<>();
    private final ArrayList<String> distances = new ArrayList<>();
    private final ArrayList<String> mapLinks = new ArrayList<>();
    private final ArrayList<Boolean> hasReport = new ArrayList<>();
    private final ArrayList<String> amounts = new ArrayList<>();
    private final ArrayList<String> paymentMethods = new ArrayList<>();


    private String doctorId;
    private double doctorLat = 0, doctorLon = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refresher;

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private boolean locationStarted = false;

    private ActivityResultLauncher<Intent> reportLauncher;
    private int lastReportPosition = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        queue = Volley.newRequestQueue(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());

        reportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && lastReportPosition != -1) {
                        hasReport.set(lastReportPosition, true);
                        adapter.notifyItemChanged(lastReportPosition);
                        lastReportPosition = -1;
                    }
                });

        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new aOngoingAdapter(
                getContext(),
                appointmentIds,
                patientNames,
                problems,
                distances,
                hasReport,
                mapLinks,
                position -> {
                    appointmentIds.remove(position);
                    patientNames.remove(position);
                    problems.remove(position);
                    distances.remove(position);
                    mapLinks.remove(position);
                    hasReport.remove(position);
                    adapter.notifyItemRemoved(position);
                },
                (appointmentId, position) -> {
                    lastReportPosition = position;
                    Intent intent = new Intent(getContext(), medical_report.class);
                    intent.putExtra("appointment_id", appointmentId);
                    reportLauncher.launch(intent);
                }
        );

        recyclerView.setAdapter(adapter);

        doctorId = String.valueOf(
                requireActivity()
                        .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                        .getInt("doctor_id", -1)
        );

        if ("-1".equals(doctorId)) {
            Toast.makeText(getContext(), "Doctor ID not found!", Toast.LENGTH_SHORT).show();
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (!appointmentIds.isEmpty()) {
                    doctorLat = result.getLastLocation().getLatitude();
                    doctorLon = result.getLastLocation().getLongitude();
                    sendLiveLocation(appointmentIds.get(0), doctorLat, doctorLon);
                }
            }
        };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndPromptGPS();
        startAppointmentRefresh();
        ensureLocationUpdates();

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && !appointmentIds.isEmpty()) {
                sendLiveLocation(appointmentIds.get(0), location.getLatitude(), location.getLongitude());
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAppointmentRefresh();
    }

    private void startAppointmentRefresh() {
        stopAppointmentRefresh();
        fetchAppointments();
        refresher = () -> {
            if (isAdded()) {
                fetchAppointments();
                handler.postDelayed(refresher, APPT_REFRESH_MS);
            }
        };
        handler.postDelayed(refresher, APPT_REFRESH_MS);
    }

    private void stopAppointmentRefresh() {
        if (refresher != null) handler.removeCallbacks(refresher);
    }

    private void ensureLocationUpdates() {
        if (locationStarted) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            return;
        }

        LocationRequest req = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2500)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
        locationStarted = true;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchAppointments() {
        String url = "http://sxm.a58.mytemp.website/Doctors/getOngoingAppointment.php";

        queue.add(new StringRequest(
                Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject root = new JSONObject(response);
                        if (!root.getBoolean("success")) return;

                        JSONArray arr = root.getJSONArray("appointments");

                        recyclerView.post(() -> {
                            appointmentIds.clear();
                            patientNames.clear();
                            problems.clear();
                            distances.clear();
                            mapLinks.clear();
                            hasReport.clear();
                            amounts.clear(); // ✅ Clear existing values
                            paymentMethods.clear(); // ✅ Clear existing values

                            for (int i = 0; i < arr.length(); i++) {
                                try {
                                    JSONObject o = arr.getJSONObject(i);
                                    appointmentIds.add(o.getString("appointment_id"));
                                    patientNames.add(o.getString("patient_name"));
                                    problems.add(o.getString("reason_for_visit"));
                                    mapLinks.add(o.getString("patient_map_link"));
                                    hasReport.add(o.optInt("has_report", 0) == 1);
                                    distances.add("Calculating...");

                                    // ✅ Add amount and method
                                    amounts.add(o.optString("amount", "0.00"));
                                    paymentMethods.add(o.optString("payment_method", "Unknown"));

                                    Log.d(TAG, "Appointment: ID=" + o.getString("appointment_id") +
                                            ", Amount=" + o.optString("amount") +
                                            ", Method=" + o.optString("payment_method"));

                                    if (i == 0) {
                                        requireContext()
                                                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                                .edit()
                                                .putString("ongoing_appointment_id", o.getString("appointment_id"))
                                                .apply();

                                        LiveLocationManager.getInstance()
                                                .startLocationUpdates(requireContext().getApplicationContext());
                                    }

                                } catch (JSONException e) {
                                    Log.e(TAG, "JSON parsing error at index " + i, e);
                                }
                            }

                            adapter.notifyDataSetChanged();

                            for (int i = 0; i < mapLinks.size(); i++) {
                                final int idx = i;
                                DistanceCalculator.calculateDistance(
                                        requireActivity(),
                                        queue,
                                        mapLinks.get(idx),
                                        dist -> {
                                            if (idx < distances.size()) {
                                                distances.set(idx, dist);
                                                adapter.notifyItemChanged(idx);
                                            }
                                        }
                                );
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "Parse error", e);
                    }
                },
                error -> Log.e(TAG, "Fetch error", error)
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                return p;
            }
        });
    }


    private void sendLiveLocation(String apptId, double lat, double lon) {
        StringRequest req = new StringRequest(
                Request.Method.POST, LIVE_LOCATION_URL,
                resp -> Log.d(TAG, "LiveLoc resp: " + resp),
                err -> Log.e(TAG, "LiveLoc err: " + err.getMessage())
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                p.put("appointment_id", apptId);
                p.put("latitude", String.valueOf(lat));
                p.put("longitude", String.valueOf(lon));
                return p;
            }
        };
        queue.add(req);
    }

    private void checkAndPromptGPS() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
                .setAlwaysShow(true)
                .build();

        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        client.checkLocationSettings(settingsRequest)
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(requireActivity(), 1011);
                        } catch (Exception ex) {
                            Log.e(TAG, "GPS dialog error", ex);
                        }
                    }
                });
    }
}
