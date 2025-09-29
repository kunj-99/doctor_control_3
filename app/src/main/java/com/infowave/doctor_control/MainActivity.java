package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.widget.*;



import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.messaging.FirebaseMessaging;
import com.infowave.doctor_control.adapter.ViewPagerAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private TextView toolbarTitle;
    ImageView iconSupport,iconReports;
    private Toolbar toolbar;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge drawing by applying padding from system bars
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        iconSupport = findViewById(R.id.icon_support);
        iconReports = findViewById(R.id.icon_reports);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        DoctorFcmTokenHelper.ensureTokenSynced(this);

        // Your existing click listeners
        iconSupport.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, suppor.class)));

        iconReports.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, tarmsandcondition.class)));

        // Setup ViewPager and BottomNavigation
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(this));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                viewPager.setCurrentItem(0, true);
                toolbarTitle.setText("The Doctor Control");
                return true;
            } else if (id == R.id.navigation_appointment) {
                viewPager.setCurrentItem(1, true);
                toolbarTitle.setText("Appointments");
                return true;
            } else if (id == R.id.navigation_history) {
                viewPager.setCurrentItem(2, true);
                toolbarTitle.setText("History");
                return true;
            } else if (id == R.id.navigation_profile) {
                viewPager.setCurrentItem(3, true);
                toolbarTitle.setText("Profile");
                return true;
            }
            return false;
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                if (pos == 0) bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                else if (pos == 1) bottomNavigationView.setSelectedItemId(R.id.navigation_appointment);
                else if (pos == 2) bottomNavigationView.setSelectedItemId(R.id.navigation_history);
                else if (pos == 3) bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
            }
        });

        // Request Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001);
            }
        }
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