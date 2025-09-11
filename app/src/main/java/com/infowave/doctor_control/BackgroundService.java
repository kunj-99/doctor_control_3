package com.infowave.doctor_control;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BackgroundService extends Service {

    public static final String TAG = BackgroundService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Maintain the doctor's status here
        keepDoctorActive();
        return START_STICKY;
    }

    private void keepDoctorActive() {
        Log.d(TAG, "Keeping doctor active");
        // Implementation to keep the doctor active
        // This could involve periodically updating the server or handling reconnections
        // Simulate updating the doctor's status on the server every hour
        // Note: Actual implementation would involve scheduling work with WorkManager or similar
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        Thread.sleep(3600000); // Sleep for one hour
                        updateServerWithActiveStatus();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        }).start();
    }

    private void updateServerWithActiveStatus() {
        // Here you would have the logic to contact the server and update the status
        Log.d(TAG, "Updating server with active status");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setDoctorInactive();
        Log.d(TAG, "Service destroyed and doctor set to inactive.");
    }

    private void setDoctorInactive() {
        // Code to set the doctor's status to inactive
        Log.d(TAG, "Setting doctor status to inactive");
        // Add actual server call or local state update logic here
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
