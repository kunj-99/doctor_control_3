package com.infowave.doctor_control;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class track_patient_location extends AppCompatActivity implements OnMapReadyCallback {

    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private boolean directMode = true;

    private MapView mapView;
    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;
    private LatLng destinationLocation;
    private TextView tvDistance, tvDuration;
    private LocationCallback locationCallback;

    // Scrim & overlay refs
    private View statusScrim, navScrim, rootContainer;
    private LinearLayout infoContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_patient_location);

        // System bars to black (actual bar colors)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);

        // Views
        rootContainer   = findViewById(R.id.root_container);
        statusScrim     = findViewById(R.id.status_bar_scrim);
        navScrim        = findViewById(R.id.navigation_bar_scrim);
        infoContainer   = findViewById(R.id.info_container);

        tvDistance      = findViewById(R.id.tvDistance);
        tvDuration      = findViewById(R.id.tvDuration);
        Button btnNavigate = findViewById(R.id.btnNavigation);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // MapView init
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Apply insets: set scrim heights and push info overlay below status bar
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Set top/bottom scrim heights
            if (statusScrim != null) {
                ViewGroup.LayoutParams lp = statusScrim.getLayoutParams();
                lp.height = sys.top;
                statusScrim.setLayoutParams(lp);
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                ViewGroup.LayoutParams lp = navScrim.getLayoutParams();
                lp.height = sys.bottom;
                navScrim.setLayoutParams(lp);
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }

            // Ensure info overlay starts below the status bar
            if (infoContainer != null) {
                int left   = infoContainer.getPaddingLeft();
                int right  = infoContainer.getPaddingRight();
                int bottom = infoContainer.getPaddingBottom();

                // Keep its original top padding (16dp) and add the status bar height
                int originalTop = dp(16);
                infoContainer.setPadding(left, originalTop + sys.top, right, bottom);
            }
            return insets;
        });

        // Parse destination from map_link (?query=lat,lng)
        String mapLink = getIntent().getStringExtra("map_link");
        if (mapLink != null && !mapLink.isEmpty()) {
            Uri uri = Uri.parse(mapLink);
            String query = uri.getQueryParameter("query");
            if (query != null && query.contains(",")) {
                String[] parts = query.split(",");
                try {
                    double lat = Double.parseDouble(parts[0].trim());
                    double lng = Double.parseDouble(parts[1].trim());
                    destinationLocation = new LatLng(lat, lng);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid coordinates in map link", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, "Map link does not contain coordinates", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "No map link provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // External Google Maps turn-by-turn navigation (driving mode)
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> {
                if (destinationLocation != null) {
                    String uri = "google.navigation:q=" + destinationLocation.latitude + "," + destinationLocation.longitude + "&mode=d";
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        Toast.makeText(track_patient_location.this, "Google Maps is not installed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(track_patient_location.this, "Destination not set", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                if (gMap != null) {
                    if (directMode) {
                        startDirectNavigation();
                    } else {
                        updateMap();
                    }
                }
                findAndTrackBestRoute();
            } else {
                Toast.makeText(track_patient_location.this, "Unable to fetch current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findAndTrackBestRoute() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                currentLocation = new LatLng(
                        locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude()
                );

                if (gMap != null) {
                    if (directMode) {
                        startDirectNavigation();
                    } else {
                        updateMap();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setZoomControlsEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
        }
        if (directMode && currentLocation != null) {
            startDirectNavigation();
        } else if (currentLocation != null) {
            updateMap();
        }
    }

    private void startDirectNavigation() {
        gMap.clear();
        if (destinationLocation != null) {
            gMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination"));
        }
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLocation, 12));
        String url = getDirectionsUrl(currentLocation, destinationLocation);
        new FetchRouteTask().execute(url);
    }

    private void updateMap() {
        gMap.clear();

        if (!directMode && currentLocation != null) {
            gMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Current Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        }
        if (destinationLocation != null) {
            gMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination"));
        }
        if (currentLocation != null) {
            if (directMode) {
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLocation, 12));
            } else {
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12));
            }
            String url = getDirectionsUrl(currentLocation, destinationLocation);
            new FetchRouteTask().execute(url);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String baseUrl = ApiConfig.endpoint("Doctors/directions.php") + "?";
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest   = "destination=" + dest.latitude + "," + dest.longitude;
        String mode       = "mode=driving";
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        return baseUrl + parameters;
    }

    private class FetchRouteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            try {
                return downloadUrl(url[0]);
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray routes = jsonObject.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject firstRoute = routes.getJSONObject(0);
                        JSONArray legs = firstRoute.getJSONArray("legs");
                        if (legs.length() > 0) {
                            JSONObject firstLeg = legs.getJSONObject(0);
                            String distanceText = firstLeg.getJSONObject("distance").getString("text");
                            String durationText = firstLeg.getJSONObject("duration").getString("text");
                            tvDistance.setText("Distance: " + distanceText);
                            tvDuration.setText("Duration: " + durationText);
                        }
                    }
                } catch (Exception ignored) {}
                new ParserTask().execute(result);
            } else {
                Toast.makeText(track_patient_location.this, "Failed to retrieve route", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String downloadUrl(String strUrl) throws Exception {
        String data;
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            data = sb.toString();
            br.close();
        } finally {
            if (iStream != null) iStream.close();
            if (urlConnection != null) urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends AsyncTask<String, Void, List<List<LatLng>>> {
        @Override
        protected List<List<LatLng>> doInBackground(String... jsonData) {
            try {
                JSONObject jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                return parser.parse(jObject);
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        protected void onPostExecute(List<List<LatLng>> result) {
            if (result == null || result.isEmpty()) {
                Toast.makeText(track_patient_location.this, "No route found", Toast.LENGTH_SHORT).show();
                return;
            }
            PolylineOptions lineOptions = new PolylineOptions()
                    .width(10f)
                    .color(Color.BLUE);

            for (List<LatLng> path : result) {
                lineOptions.addAll(path);
            }
            gMap.addPolyline(lineOptions);
        }
    }

    // ---- FIXED: removed the extra '>' in the generic ----
    public static class DirectionsJSONParser {

        public List<List<LatLng>> parse(JSONObject jObject) {
            List<List<LatLng>> routes = new ArrayList<>();
            try {
                JSONArray jRoutes = jObject.getJSONArray("routes");
                for (int i = 0; i < jRoutes.length(); i++) {
                    JSONArray jLegs = jRoutes.getJSONObject(i).getJSONArray("legs");
                    List<LatLng> path = new ArrayList<>();
                    for (int j = 0; j < jLegs.length(); j++) {
                        JSONArray jSteps = jLegs.getJSONObject(j).getJSONArray("steps");
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = jSteps.getJSONObject(k)
                                    .getJSONObject("polyline")
                                    .getString("points");
                            path.addAll(decodePoly(polyline));
                        }
                    }
                    routes.add(path);
                }
            } catch (Exception ignored) {}
            return routes;
        }

        private List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
                lat += dlat;

                shift = 0; result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0) ? ~(result >> 1) : (result >> 1);
                lng += dlng;

                poly.add(new LatLng(lat / 1E5, lng / 1E5));
            }
            return poly;
        }
    }

    // ---- MapView lifecycle ----
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onStart()  { super.onStart();  mapView.onStart(); }
    @Override protected void onStop()   { super.onStop();   mapView.onStop(); }
    @Override protected void onPause()  {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        mapView.onPause();
        super.onPause();
    }
    @Override protected void onDestroy(){ mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }

    // ---- Permission result ----
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
                if (gMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    gMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Location permission is required to show directions", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
