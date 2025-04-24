package com.example.doctor_control.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class aOngoingFragment extends Fragment {
    private static final String TAG = "aOngoingFragment";
    private static final String LIVE_LOCATION_URL =
            "http://sxm.a58.mytemp.website/update_live_location.php";
    private static final long APPT_REFRESH_MS = 5000L;

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private RequestQueue queue;

    // Adapter data
    private final ArrayList<String> appointmentIds = new ArrayList<>();
    private final ArrayList<String> patientNames   = new ArrayList<>();
    private final ArrayList<String> problems       = new ArrayList<>();
    private final ArrayList<String> distances      = new ArrayList<>();
    private final ArrayList<String> mapLinks       = new ArrayList<>();
    private final ArrayList<Boolean> hasReport     = new ArrayList<>();

    private String doctorId;
    private double doctorLat = 0, doctorLon = 0;

    // Appointment refresher
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refresher;

    // Location updates
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private boolean locationStarted = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);

        queue = Volley.newRequestQueue(requireContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(requireContext());

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
                null
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
        startAppointmentRefresh();
        ensureLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAppointmentRefresh();
    }

    private void startAppointmentRefresh() {
        stopAppointmentRefresh();
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
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
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

                        appointmentIds.clear();
                        patientNames.clear();
                        problems.clear();
                        distances.clear();
                        mapLinks.clear();
                        hasReport.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);
                            String id   = o.getString("appointment_id");
                            String name = o.getString("patient_name");
                            String prob = o.getString("reason_for_visit");
                            String link = o.getString("patient_map_link");

                            int reportFlag = o.optInt("has_report", 0); // 💡 Read from JSON
                            boolean reportExists = reportFlag == 1;

                            appointmentIds.add(id);
                            patientNames.add(name);
                            problems.add(prob);
                            distances.add("Calculating...");
                            mapLinks.add(link);
                            hasReport.add(reportExists);

                            if (i == 0) {
                                requireContext()
                                        .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE)
                                        .edit()
                                        .putString("ongoing_appointment_id", id)
                                        .apply();

                                LiveLocationManager.getInstance()
                                        .startLocationUpdates(requireContext().getApplicationContext());
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
                                        distances.set(idx, dist);
                                        adapter.notifyItemChanged(idx);
                                    }
                            );
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Parse error", e);
                    }
                },
                error -> Log.e(TAG, "Fetch error", error)
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                return p;
            }
        });
    }

    private void sendLiveLocation(String apptId, double lat, double lon) {
        StringRequest req = new StringRequest(
                Request.Method.POST, LIVE_LOCATION_URL,
                resp -> Log.d(TAG, "LiveLoc resp: " + resp),
                err  -> Log.e(TAG, "LiveLoc err: " + err.getMessage())
        ) {
            @Override
            protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("doctor_id", doctorId);
                p.put("appointment_id", apptId);
                p.put("latitude", String.valueOf(lat));
                p.put("longitude", String.valueOf(lon));
                return p;
            }
        };
        queue.add(req);
    }
}
