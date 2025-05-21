package com.infowave.doctor_control.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.infowave.doctor_control.MainActivity;
import com.infowave.doctor_control.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AppointmentFragment extends Fragment {

    private static final String TAG = "AppointmentFragment";
    private ViewPager2 innerViewPager;

    public AppointmentFragment() {
        // Required empty public constructor
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_appointment, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        innerViewPager = view.findViewById(R.id.child_view_pager);

        // Setup child adapter
        innerViewPager.setAdapter(new AppointmentPagerAdapter(this));
        innerViewPager.setCurrentItem(0, false);

        // Attach tabs
        new TabLayoutMediator(tabLayout, innerViewPager, (tab, pos) -> {
            switch (pos) {
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
        }).attach();

        // Detect current tab and update outer swipe control
        innerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "Child tab selected: " + position);

                if (getActivity() instanceof MainActivity) {
                    MainActivity main = (MainActivity) getActivity();
                    if (position == 0 || position == 2) {
                        // Enable outer swipe only on first or last tab
                        main.setMainViewPagerSwipeEnabled(true);
                    } else {
                        main.setMainViewPagerSwipeEnabled(false);
                    }
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setMainViewPagerSwipeEnabled(false);
        }
        innerViewPager.setUserInputEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setMainViewPagerSwipeEnabled(true);
        }
    }

    private static class AppointmentPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        AppointmentPagerAdapter(@NonNull Fragment fragment) {
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