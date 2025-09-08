package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.infowave.doctor_control.HistoryItem;
import com.infowave.doctor_control.R;
import com.infowave.doctor_control.adapter.HistoryAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private HistoryAdapter historyAdapter;

    private final ArrayList<HistoryItem> historyItems = new ArrayList<>();
    private final ArrayList<String> appointmentIds = new ArrayList<>();

    private RequestQueue requestQueue;
    private static final String BASE_URL = "http://sxm.a58.mytemp.website/Doctors/gethistory.php?doctor_id=";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rvHistory    = view.findViewById(R.id.rv_history);
        swipeRefresh = view.findViewById(R.id.swipeRefreshHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        historyAdapter = new HistoryAdapter(historyItems, appointmentIds);
        rvHistory.setAdapter(historyAdapter);

        requestQueue = Volley.newRequestQueue(requireContext());

        // Initial load (no swipe spinner)
        fetchHistoryData(getDoctorId());

        // Pull-to-refresh
        swipeRefresh.setOnRefreshListener(() -> fetchHistoryData(getDoctorId()));
        // Optional: customize spinner colors
        // swipeRefresh.setColorSchemeResources(R.color.navy_blue, R.color.acqua_green, R.color.purple_500);
    }

    private String getDoctorId() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("DoctorPrefs", Context.MODE_PRIVATE);
        int id = prefs.getInt("doctor_id", 0);
        return String.valueOf(id);
    }

    private void stopSwipeSpinner() {
        if (swipeRefresh != null && swipeRefresh.isRefreshing()) {
            swipeRefresh.setRefreshing(false);
        }
    }

    private void fetchHistoryData(String doctorId) {
        String url = BASE_URL + doctorId;

        @SuppressLint("NotifyDataSetChanged")
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    ArrayList<HistoryItem> tmpItems = new ArrayList<>();
                    ArrayList<String> tmpIds = new ArrayList<>();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String apptId          = obj.getString("appointment_id");
                            String patientName     = obj.getString("patient_name");
                            String appointmentDate = obj.getString("appointment_date");
                            String symptoms        = obj.getString("reason_for_visit");
                            boolean flag           = obj.optBoolean("flag", false);
                            String patientId       = obj.getString("patient_id");
                            String status          = obj.optString("status", "");

                            tmpIds.add(apptId);
                            tmpItems.add(new HistoryItem(
                                    patientName,
                                    appointmentDate,
                                    symptoms,
                                    flag,
                                    patientId,
                                    apptId,
                                    status
                            ));
                        }

                        historyItems.clear();
                        historyItems.addAll(tmpItems);

                        appointmentIds.clear();
                        appointmentIds.addAll(tmpIds);

                        historyAdapter.notifyDataSetChanged();

                        if (historyItems.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "No appointment history found.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getContext(),
                                "Sorry, we couldn’t process the history right now.",
                                Toast.LENGTH_SHORT).show();
                    } finally {
                        stopSwipeSpinner();
                    }
                },
                error -> {
                    Toast.makeText(getContext(),
                            "Unable to fetch history. Please pull down to try again.",
                            Toast.LENGTH_SHORT).show();
                    stopSwipeSpinner();
                }
        );

        requestQueue.add(jsonArrayRequest);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (requestQueue != null) {
            requestQueue.cancelAll(request -> true);
        }
    }
}
