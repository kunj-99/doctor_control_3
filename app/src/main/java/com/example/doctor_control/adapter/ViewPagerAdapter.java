package com.example.doctor_control.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.doctor_control.fragments.AppointmentFragment;
import com.example.doctor_control.fragments.HistoryFragment;
import com.example.doctor_control.fragments.ProfileFragment;
import com.example.doctor_control.fragments.home;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new home();         // Your Home fragment
            case 1:
                return new AppointmentFragment();  // Your Appointment fragment
            case 2:
                return new HistoryFragment();      // Your History fragment
            case 3:
                return new ProfileFragment();      // Your Profile fragment
            default:
                return new home();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // Total number of pages
    }
}
