package com.example.doctor_control;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;

import java.net.URISyntaxException;

public class track_patient_location extends AppCompatActivity {

    private WebView mapWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_track_patient_location);

        // Handle edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize WebView
        mapWebView = findViewById(R.id.mapWebView);
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Set custom WebViewClient to handle intent:// URLs
        mapWebView.setWebViewClient(new WebViewClient() {

            // For API < 24
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(view, url);
            }

            // For API >= 24
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(view, request.getUrl().toString());
            }

            private boolean handleUrl(WebView view, String url) {
                if (url.startsWith("intent://")) {
                    try {
                        // Parse the intent URL
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            try {
                                // Launch the intent externally
                                startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                // Notify user if no application can handle the intent
                                Toast.makeText(track_patient_location.this,
                                        "No application available to handle this request.",
                                        Toast.LENGTH_LONG).show();
                            }
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    // Return true to prevent default behavior in case of error
                    return true;
                }
                // For non-intent URLs, load normally
                view.loadUrl(url);
                return true;
            }
        });

        // Get map link from intent extras
        String mapLink = getIntent().getStringExtra("map_link");
        if (mapLink != null && !mapLink.isEmpty()) {
            mapWebView.loadUrl(mapLink);
        } else {
            Toast.makeText(this, "No direction link provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
