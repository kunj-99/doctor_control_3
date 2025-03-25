package com.example.doctor_control.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.doctor_control.adapter.HistoryAdapter;
import com.example.doctor_control.HistoryItem;
import com.example.doctor_control.R;
import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment  {

    private RecyclerView rvHistory;
    private HistoryAdapter historyAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // Initialize RecyclerView
        rvHistory = view.findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create dummy data
        List<HistoryItem> historyItems = new ArrayList<>();
        historyItems.add(new HistoryItem("John Doe", "12 Oct 2023", "Fever & Headache", true));
        historyItems.add(new HistoryItem("Jane Smith", "10 Oct 2023", "Back Pain", false));
        historyItems.add(new HistoryItem("Alice Johnson", "8 Oct 2023", "Cough & Cold", true));

        // Set adapter
        historyAdapter = new HistoryAdapter(historyItems);
        rvHistory.setAdapter(historyAdapter);

        return view;
    }
}
