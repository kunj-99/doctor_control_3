package com.example.doctor_control.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.doctor_control.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AppointmentFragment extends Fragment {

    public AppointmentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointment, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabs);
        ViewPager2 viewPager = view.findViewById(R.id.view_pager);

        // Set the adapter for the ViewPager2
        viewPager.setAdapter(new AppointmentPagerAdapter(this));

        // Link the TabLayout and the ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        // Set the title of each tab based on its position
                        switch (position) {
                            case 0:
                                tab.setText("Ongoing");
                                break;
                            case 1:
                                tab.setText("Pending");
                                break;
                            case 2:
                                tab.setText("Request");
                                break;
                        }
                    }
                }).attach();

        return view;
    }

    // Adapter to supply fragments for each page
    private class AppointmentPagerAdapter extends FragmentStateAdapter {
        public AppointmentPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new aOngoingFragment();
                case 1:
                    return new aPendingFragment();
                case 2:
                    return new aRequestFragment();
                default:
                    return new aOngoingFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
