package com.infowave.doctor_control;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class suppor extends AppCompatActivity {

    // Replace with your actual API URL where contact_us.php is hosted
    private static final String API_URL = "http://sxm.a58.mytemp.website/contact_us.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suppor);
        new FetchContactDataTask().execute();
    }

    // Helper method to format phone number: first 3 digits in brackets.
    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && phoneNumber.length() >= 3) {
            String firstThree = phoneNumber.substring(0, 3);
            String rest = phoneNumber.substring(3);
            return "(" + firstThree + ")" + rest;
        }
        return phoneNumber;
    }

    private class FetchContactDataTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString();
                }
            } catch (Exception e) {
                // Error handled below in onPostExecute
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                try {
                    JSONArray jsonArray = new JSONArray(result);
                    if (jsonArray.length() > 0) {
                        JSONObject contact = jsonArray.getJSONObject(0);
                        final String whatsappNumber = contact.getString("Whatsapp_number");
                        final String phoneNumber = contact.getString("phone_number");
                        final String email = contact.getString("email");
                        final String formattedPhoneNumber = formatPhoneNumber(phoneNumber);

                        TextView whatsappText = findViewById(R.id.whatsapp_text);
                        TextView phoneText = findViewById(R.id.mobile_text);
                        TextView emailText = findViewById(R.id.email_text);

                        whatsappText.setText(whatsappNumber);
                        phoneText.setText(formattedPhoneNumber);
                        emailText.setText(email);

                        emailText.setOnClickListener(v -> {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                            emailIntent.setData(Uri.parse("mailto:" + email));
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
                            emailIntent.putExtra(Intent.EXTRA_TEXT, "Your email body here");
                            startActivity(Intent.createChooser(emailIntent, "Send Email"));
                        });

                        phoneText.setOnClickListener(v -> {
                            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                            dialIntent.setData(Uri.parse("tel:" + formattedPhoneNumber));
                            startActivity(dialIntent);
                        });

                        whatsappText.setOnClickListener(v -> {
                            String url = "https://api.whatsapp.com/send?phone=" + whatsappNumber;
                            Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                            whatsappIntent.setData(Uri.parse(url));
                            startActivity(whatsappIntent);
                        });
                    } else {
                        Toast.makeText(suppor.this, "Contact information is currently unavailable.", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(suppor.this, "Unable to load contact details. Please try again later.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(suppor.this, "Unable to fetch contact info. Please check your internet connection.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
