package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
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

    private static final String TAG = "DOC_MAIN";

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigationView;
    private TextView toolbarTitle;
    private ImageView iconSupport, iconReports;
    private Toolbar toolbar;

    // Ask Active/Inactive on app close
    private final ActiveStatusManager activeGuard = new ActiveStatusManager();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
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

        // ===== Toolbar & icons =====
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbarTitle = findViewById(R.id.toolbar_title);
        iconSupport = findViewById(R.id.icon_support);
        iconReports = findViewById(R.id.icon_reports);

        if (iconSupport != null) {
            iconSupport.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, suppor.class)));
        }
        if (iconReports != null) {
            iconReports.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, tarmsandcondition.class)));
        }

        // ===== ViewPager & BottomNav =====
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

        // ===== Android 13+ notifications permission (use the service helper for parity) =====
        MyFirebaseMessagingService.requestNotificationPermissionIfNeeded(this);

        // ===== Sync FCM token like patient side (only if doctor_id is present) =====
        SharedPreferences sp = getSharedPreferences("DoctorPrefs", MODE_PRIVATE);
        int doctorId = sp.getInt("doctor_id", -1);
        Log.d(TAG, "doctor_id in prefs=" + doctorId);
        if (doctorId > 0) {
            DoctorFcmTokenHelper.ensureTokenSynced(this);
        } else {
            Log.w(TAG, "doctor_id missing; deferring FCM sync until after login");
        }

        // ===== If app opened from FCM, jump to Appointments =====
        boolean openRequests = getIntent().getBooleanExtra("open_requests", false);
        if (openRequests) {
            Log.d(TAG, "open_requests=true from intent → switch to Appointments");
            bottomNavigationView.setSelectedItemId(R.id.navigation_appointment);
            viewPager.setCurrentItem(1, false);
            toolbarTitle.setText("Appointments");
        }

        // ===== Intercept back to show Active/Inactive guard BEFORE default behavior =====
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Log.d(TAG, "onBackPressed (intercepted)");
                activeGuard.promptAndExit(MainActivity.this,
                        ActiveStatusManager.Trigger.BACK_EXIT,
                        null);
            }
        });
    }

    /** Enables or disables swipe gestures on the main ViewPager2. */
    public void setMainViewPagerSwipeEnabled(boolean enabled) {
        if (viewPager != null) viewPager.setUserInputEnabled(enabled);
    }

    public void navigateToHome() {
        if (viewPager != null) {
            viewPager.setCurrentItem(0, true);
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
    }

    public void navigateToHistory() {
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
            bottomNavigationView.setSelectedItemId(R.id.navigation_history);
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        Log.d(TAG, "onUserLeaveHint()");
        activeGuard.promptAndExit(this,
                ActiveStatusManager.Trigger.HOME_OR_APP_SWITCH,
                null);
    }
}
