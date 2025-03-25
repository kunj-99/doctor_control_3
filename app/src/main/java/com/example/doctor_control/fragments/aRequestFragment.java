package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctor_control.R;
import com.example.doctor_control.adapter.aRequestAdapeter;

import java.util.ArrayList;

public class aRequestFragment extends Fragment {

    private RecyclerView recyclerView;
    private aRequestAdapeter adapter;

    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<String> problems = new ArrayList<>();
    private ArrayList<String> distances = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDummyData(); // Fill arrays

        adapter = new aRequestAdapeter(names, problems, distances);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void loadDummyData() {
        names.add("Meena");
        problems.add("Skin Rash");
        distances.add("4 km");

        names.add("Rahul");
        problems.add("Leg Injury");
        distances.add("9 km");

        names.add("Fatima");
        problems.add("High Fever");
        distances.add("2 km");
    }
}
