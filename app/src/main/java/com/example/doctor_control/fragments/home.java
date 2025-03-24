package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.doctor_control.R;

import org.json.JSONException;
import org.json.JSONObject;

public class home extends Fragment {

    // URL to your PHP endpoint (update with your actual URL)
    private static final String URL_COMPLETED_COUNT = "http://sxm.a58.mytemp.website/completed_appointment.php";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Fetch data from the server once the view is ready
        fetchCompletedCount();
    }

    private void fetchCompletedCount() {
        // Create a new JsonObjectRequest using Volley
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                URL_COMPLETED_COUNT,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Get the completed_count from the JSON response
                            int completedCount = response.getInt("completed_count");
                            // Update the view with the new count
                            // Ensure that the view is not null
                            View rootView = getView();
                            if (rootView != null) {
                                TextView patientsCountTextView = rootView.findViewById(R.id.patients_count);
                                if (patientsCountTextView != null) {
                                    patientsCountTextView.setText(String.valueOf(completedCount));
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("home", "JSON parsing error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("home", "Volley error: " + error.getMessage());
                    }
                }
        );

        // Add the request to the Volley request queue
        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);
    }
}
