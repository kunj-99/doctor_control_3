package com.example.doctor_control.fragments;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aOngoingAdapter;

import java.util.ArrayList;

public class aOngoingFragment extends Fragment {

    private RecyclerView recyclerView;
    private aOngoingAdapter adapter;
    private ArrayList<String> patientNames;
    private ArrayList<String> problems;
    private ArrayList<String> distances;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ongoing, container, false);
        recyclerView = view.findViewById(R.id.rv_ongoing_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize separate arrays for each field
        patientNames = new ArrayList<>();
        problems = new ArrayList<>();
        distances = new ArrayList<>();

        // Populate the arrays with sample data
        patientNames.add("John Doe");
        problems.add("Fever and Headache");
        distances.add("23 km");

        patientNames.add("Jane Smith");
        problems.add("Cold and Cough");
        distances.add("15 km");

        patientNames.add("Bob Johnson");
        problems.add("Back Pain");
        distances.add("10 km");

        // Create and set the adapter with the separate arrays
        adapter = new aOngoingAdapter(getContext(), patientNames, problems, distances);
        recyclerView.setAdapter(adapter);

        return view;
    }
}
