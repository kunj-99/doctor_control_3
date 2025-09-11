package com.infowave.doctor_control.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.infowave.doctor_control.fragments.aOngoingFragment;
import com.infowave.doctor_control.fragments.aPendingFragment;
import com.infowave.doctor_control.fragments.aRequestFragment;

public class AppointmentPagerAdapter extends FragmentStateAdapter {
    public AppointmentPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new aOngoingFragment();
            case 1: return new aPendingFragment();
            case 2: return new aRequestFragment();
            default: return new aOngoingFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
