package com.example.doctor_control;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Setup window insets for edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        String logs = LiveLocationManager.getInstance().getLocationLogs(getApplicationContext());
        Log.d("TrackingHistory", logs);


        // Initialize ViewPager2 and set its adapter
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ViewPagerAdapter(this));

        // Initialize Bottom Navigation View
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Listener for Bottom Navigation item selection to update ViewPager
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    viewPager.setCurrentItem(0);
                    return true;
                } else if (id == R.id.navigation_appointment) {
                    viewPager.setCurrentItem(1);
                    return true;
                } else if (id == R.id.navigation_history) {
                    viewPager.setCurrentItem(2);
                    return true;
                } else if (id == R.id.navigation_profile) {
                    viewPager.setCurrentItem(3);
                    return true;
                }
                return false;
            }
        });


        // Listener for page changes to update the Bottom Navigation selection
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_appointment);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_history);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.navigation_profile);
                        break;
                }
            }
        });
    }
}
