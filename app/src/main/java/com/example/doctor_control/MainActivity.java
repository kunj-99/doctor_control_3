package com.example.doctor_control;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.doctor_control.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(this));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (id == R.id.navigation_appointment) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (id == R.id.navigation_history) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (id == R.id.navigation_profile) {
                viewPager.setCurrentItem(3, true);
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                } else if (position == 1) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_appointment);
                } else if (position == 2) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_history);
                } else if (position == 3) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                }
            }
        });
    }

    /**
     * Enables or disables swipe gestures on the main ViewPager2.
     *
     * @param enabled Pass true to enable swiping.
     */
    public void setMainViewPagerSwipeEnabled(boolean enabled) {
        viewPager.setUserInputEnabled(enabled);
    }

    /**
     * Navigates to the Home screen (position 0) smoothly.
     */
    public void navigateToHome() {
        viewPager.setCurrentItem(0, true);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    /**
     * Navigates to the History screen (position 2) smoothly.
     */
    public void navigateToHistory() {
        viewPager.setCurrentItem(2, true);
        bottomNavigationView.setSelectedItemId(R.id.navigation_history);
    }
}
