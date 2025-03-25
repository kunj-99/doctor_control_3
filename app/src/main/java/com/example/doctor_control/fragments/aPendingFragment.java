package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctor_control.R;
import com.example.doctor_control.adapter.apendingAdapter;

import java.util.ArrayList;

public class aPendingFragment extends Fragment {

    private RecyclerView recyclerView;
    private apendingAdapter adapter;

    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> problems = new ArrayList<>();
    ArrayList<String> distances = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        recyclerView = view.findViewById(R.id.rv_pending_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadDummyData();

        adapter = new apendingAdapter(names, problems, distances);
        recyclerView.setAdapter(adapter);

        return view;
    }

    private void loadDummyData() {
        names.add("Ramesh");
        problems.add("Fever");
        distances.add("5 km");

        names.add("Suresh");
        problems.add("Cough & Cold");
        distances.add("8 km");

        names.add("Ganesh");
        problems.add("Back Pain");
        distances.add("12 km");
    }
}
