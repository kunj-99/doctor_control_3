package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.infowave.doctor_control.adapter.ViewPagerAdapter;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private TextView toolbarTitle;
    ImageView iconSupport, iconReports;
    private Toolbar toolbar;

    // ✅ NEW: guard that asks Active/Inactive on app close
    private final ActiveStatusManager activeGuard = new ActiveStatusManager();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== Edge-to-edge with scrims =====
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final android.view.View statusScrim = findViewById(R.id.status_bar_scrim);
        final android.view.View navScrim = findViewById(R.id.navigation_bar_scrim);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            if (statusScrim != null) {
                statusScrim.getLayoutParams().height = sys.top;
                statusScrim.setLayoutParams(statusScrim.getLayoutParams());
                statusScrim.setVisibility(sys.top > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            if (navScrim != null) {
                navScrim.getLayoutParams().height = sys.bottom;
                navScrim.setLayoutParams(navScrim.getLayoutParams());
                navScrim.setVisibility(sys.bottom > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
            }
            v.setPadding(sys.left, 0, sys.right, 0);
            return insets;
        });

        // ===== Your existing setup =====
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        iconSupport = findViewById(R.id.icon_support);
        iconReports = findViewById(R.id.icon_reports);

        DoctorFcmTokenHelper.ensureTokenSynced(this);

        iconSupport.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, suppor.class)));

        iconReports.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, tarmsandcondition.class)));

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
            @Override public void onPageSelected(int pos) {
                if (pos == 0) bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                else if (pos == 1) bottomNavigationView.setSelectedItemId(R.id.navigation_appointment);
                else if (pos == 2) bottomNavigationView.setSelectedItemId(R.id.navigation_history);
                else if (pos == 3) bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
            }
        });

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    /** Enables or disables swipe gestures on the main ViewPager2. */
    public void setMainViewPagerSwipeEnabled(boolean enabled) {
        viewPager.setUserInputEnabled(enabled);
    }

    /** Navigates to the Home screen (position 0) smoothly. */
    public void navigateToHome() {
        viewPager.setCurrentItem(0, true);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    /** Navigates to the History screen (position 2) smoothly. */
    public void navigateToHistory() {
        viewPager.setCurrentItem(2, true);
        bottomNavigationView.setSelectedItemId(R.id.navigation_history);
    }

    // ✅ NEW: Ask before exiting via Back on the root task
    @Override
    public void onBackPressed() {
        activeGuard.promptAndExit(this,
                ActiveStatusManager.Trigger.BACK_EXIT,
                null /* nothing after exit */);
    }

    // ✅ NEW: Ask when the user presses Home / app switch
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        activeGuard.promptAndExit(this,
                ActiveStatusManager.Trigger.HOME_OR_APP_SWITCH,
                null);
    }
}
