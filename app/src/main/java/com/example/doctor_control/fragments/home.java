package com.example.doctor_control.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doctor_control.R;
import com.example.doctor_control.adapter.homeadapter;

public class home extends Fragment {

    private RecyclerView recentActivityRecyclerView;
    private homeadapter recentActivityAdapter;
    private Switch statusToggle;
    private TextView statusText;
    private ImageView status_icon;

    public home() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize RecyclerView
        recentActivityRecyclerView = view.findViewById(R.id.recent_activity_recycler_view);
        recentActivityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Data arrays for recent activities
        String[] activityTitles = {
                "Completed appointment with John Doe",
                "New patient registration",
                "Prescription updated for Jane Smith"
        };

        String[] activityTimes = {
                "2h ago",
                "4h ago",
                "1d ago"
        };

        // Set adapter with the arrays
        recentActivityAdapter = new homeadapter(activityTitles, activityTimes);
        recentActivityRecyclerView.setAdapter(recentActivityAdapter);

        // Initialize status views
        statusToggle = view.findViewById(R.id.status_toggle);
        status_icon = view.findViewById(R.id.status_icon);
        statusText = view.findViewById(R.id.status_text);

        // Set toggle listener
        statusToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                status_icon.setImageResource(R.drawable.ic_active_status);
                statusText.setText("Active");
                statusText.setTextColor(getResources().getColor(R.color.primaryColor));
            } else {
                status_icon.setImageResource(R.drawable.ic_inactive_status);
                statusText.setText("Inactive");
                statusText.setTextColor(getResources().getColor(R.color.textSecondary));
            }
        });

        return view;
    }
}
