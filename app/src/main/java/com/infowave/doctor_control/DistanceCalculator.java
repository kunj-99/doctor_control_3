package com.infowave.doctor_control;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DistanceCalculator {
    private static final String DIST_MATRIX_BASE =
            "https://maps.googleapis.com/maps/api/distancematrix/json";

    /** For single‐link lookups */
    public interface DistanceCallback {
        void onDistanceResult(String distanceText);
    }

    /**
     * Look up driving distance from the device’s last location → one destination.
     */
    public static void calculateDistance(
            final Activity host,
            final RequestQueue queue,
            final String patientMapLink,
            final DistanceCallback callback
    ) {
        if (ActivityCompat.checkSelfPermission(host,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onDistanceResult("N/A");
            return;
        }

        LocationServices
                .getFusedLocationProviderClient(host)
                .getLastLocation()
                .addOnSuccessListener((Location location) -> {
                    if (location == null) {
                        callback.onDistanceResult("N/A");
                        return;
                    }
                    double originLat = location.getLatitude();
                    double originLon = location.getLongitude();

                    String coordStr = "";
                    if (patientMapLink != null && patientMapLink.contains("query=")) {
                        String[] split = patientMapLink.split("query=");
                        if (split.length > 1) coordStr = split[1].split("&")[0];
                    }

                    double destLat, destLon;
                    try {
                        String[] parts = coordStr.split(",");
                        destLat = Double.parseDouble(parts[0].trim());
                        destLon = Double.parseDouble(parts[1].trim());
                    } catch (Exception ex) {
                        callback.onDistanceResult("N/A");
                        return;
                    }

                    String key = host.getString(R.string.google_maps_key);
                    String url = DIST_MATRIX_BASE
                            + "?origins="      + originLat  + "," + originLon
                            + "&destinations=" + destLat    + "," + destLon
                            + "&mode=driving"
                            + "&key="          + key;

                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.GET, url, null,
                            resp -> {
                                try {
                                    JSONArray rows = resp.getJSONArray("rows");
                                    JSONObject elem = rows
                                            .getJSONObject(0)
                                            .getJSONArray("elements")
                                            .getJSONObject(0);

                                    if ("OK".equals(elem.getString("status"))) {
                                        String text = elem
                                                .getJSONObject("distance")
                                                .getString("text");
                                        callback.onDistanceResult(text);
                                    } else {
                                        callback.onDistanceResult("N/A");
                                    }
                                } catch (JSONException je) {
                                    callback.onDistanceResult("N/A");
                                }
                            },
                            err -> {
                                callback.onDistanceResult("N/A");
                            }
                    );
                    queue.add(req);
                })
                .addOnFailureListener(e -> {
                    callback.onDistanceResult("N/A");
                });
    }


    /** Batch‐lookup callback */
    public interface DistanceBatchCallback {
        void onDistanceResults(List<String> distanceTexts);
    }

    /**
     * Look up driving distances from one origin → many destinations in a single call.
     */
    public static void calculateDistanceBatch(
            Activity host,
            RequestQueue queue,
            double originLat,
            double originLon,
            List<String> patientLinks,
            DistanceBatchCallback callback
    ) {
        if (ActivityCompat.checkSelfPermission(host,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            List<String> naList = new ArrayList<>();
            for (int i = 0; i < patientLinks.size(); i++) naList.add("N/A");
            callback.onDistanceResults(naList);
            return;
        }

        List<String> destCoords = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            destCoords = patientLinks.stream().map(link -> {
                if (link != null && link.contains("query=")) {
                    String[] split = link.split("query=");
                    if (split.length > 1) return split[1].split("&")[0];
                }
                return "";
            }).collect(Collectors.toList());
        }

        String destinations = String.join("|", destCoords);
        String key = host.getString(R.string.google_maps_key);
        String url = DIST_MATRIX_BASE
                + "?origins="      + originLat  + "," + originLon
                + "&destinations=" + destinations
                + "&mode=driving"
                + "&key="          + key;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONArray elems = resp
                                .getJSONArray("rows")
                                .getJSONObject(0)
                                .getJSONArray("elements");

                        List<String> results = new ArrayList<>();
                        for (int i = 0; i < elems.length(); i++) {
                            JSONObject el = elems.getJSONObject(i);
                            if ("OK".equals(el.getString("status"))) {
                                results.add(el.getJSONObject("distance").getString("text"));
                            } else {
                                results.add("N/A");
                            }
                        }
                        callback.onDistanceResults(results);
                    } catch (JSONException je) {
                        List<String> naList = new ArrayList<>();
                        for (int i = 0; i < patientLinks.size(); i++) naList.add("N/A");
                        callback.onDistanceResults(naList);
                    }
                },
                err -> {
                    List<String> naList = new ArrayList<>();
                    for (int i = 0; i < patientLinks.size(); i++) naList.add("N/A");
                    callback.onDistanceResults(naList);
                }
        );
        queue.add(req);
    }
}
