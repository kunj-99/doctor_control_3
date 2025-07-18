package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class medical_report extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;

    private android.widget.LinearLayout virtualReportForm, directUploadSection, medicationsContainer;
    private android.widget.RadioGroup radioGroupUploadType;
    private android.widget.ImageView ivReportImage;
    private MaterialButton btnUploadImage, btnSaveReport, btnAddMedicine;
    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private int medicineCount = 1;

    private TextInputEditText etPatientName, etAge, etSex, etWeight, etAddress, etDate,
            etTemperature, etPulse, etSpo2, etBloodPressure, etSignature, etReportType;
    private TextInputEditText etSymptoms, etRespiratorySystem;

    private RequestQueue requestQueue;
    private String appointmentId;

    private Uri cameraImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_report);

        initializeViews();
        setupRadioGroup();

        appointmentId = getIntent().getStringExtra("appointment_id");
        requestQueue  = Volley.newRequestQueue(this);

        if (appointmentId != null && !appointmentId.isEmpty()) {
            fetchAppointmentDetails(appointmentId);
        }
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        virtualReportForm   = findViewById(R.id.virtual_report_form);
        directUploadSection = findViewById(R.id.direct_upload_section);
        medicationsContainer= findViewById(R.id.medications_container);
        radioGroupUploadType= findViewById(R.id.radioGroupUploadType);
        ivReportImage       = findViewById(R.id.ivReportImage);
        btnUploadImage      = findViewById(R.id.btnUploadImage);
        btnSaveReport       = findViewById(R.id.btnSaveReport);
        btnAddMedicine      = findViewById(R.id.btnAddMedicine);

        etSymptoms          = findViewById(R.id.etSymptoms);
        etRespiratorySystem = findViewById(R.id.etRespiratorySystem);

        etPatientName   = findViewById(R.id.etPatientName);
        etAge           = findViewById(R.id.etAge);
        etSex           = findViewById(R.id.etSex);
        etWeight        = findViewById(R.id.etWeight);
        etAddress       = findViewById(R.id.etAddress);
        etDate          = findViewById(R.id.etDate);
        etTemperature   = findViewById(R.id.etTemperature);
        etPulse         = findViewById(R.id.etPulse);
        etSpo2          = findViewById(R.id.etSpo2);
        etBloodPressure = findViewById(R.id.etBloodPressure);
        etSignature     = findViewById(R.id.etSignature);
        etReportType    = findViewById(R.id.etReportType);

        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveReport .setOnClickListener(v -> saveReport());
        btnAddMedicine.setOnClickListener(v -> addMedicineField());
    }

    private void setupRadioGroup() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVirtualReport) {
                virtualReportForm.setVisibility(View.VISIBLE);
                directUploadSection.setVisibility(View.GONE);
            } else {
                virtualReportForm.setVisibility(View.GONE);
                directUploadSection.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchAppointmentDetails(String id) {
        String url = "https://thedoctorathome.in/Doctors/get_appointment_details.php?appointment_id=" + id;

        loaderutil.showLoader(this);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    loaderutil.hideLoader();
                    try {
                        if (response.optBoolean("success")) {
                            etPatientName.setText(response.optString("full_name", ""));
                            etAge.setText     (response.optString("age", ""));
                            etSex.setText     (response.optString("sex", ""));
                            etAddress.setText (response.optString("address", ""));
                            etDate.setText    (response.optString("date", ""));
                            etReportType.setText("");
                            etSignature.setText(response.optString("doctor_name", ""));
                        } else {
                            showError("Appointment not found.");
                        }

                        etWeight.setText("");
                        etTemperature.setText("");
                        etPulse.setText("");
                        etSpo2.setText("");
                        etBloodPressure.setText("");

                    } catch (Exception e) {
                        showError("Error parsing appointment data.");
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    showError("Failed to fetch appointment details.");
                });

        requestQueue.add(request);
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Upload Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) openCamera();
                    else openGallery();
                })
                .show();
    }

    private void openCamera() {
        File imageFile = new File(getExternalCacheDir(),
                "report_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                imageFile);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loaderutil.showLoader(this);
            if (requestCode == REQUEST_CAMERA) {
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), cameraImageUri);
                    ivReportImage.setImageBitmap(selectedBitmap);
                    ivReportImage.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    showError("Failed to load captured image.");
                }
            } else if (requestCode == REQUEST_GALLERY &&
                    data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                try {
                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                            this.getContentResolver(), selectedImageUri);
                    ivReportImage.setImageBitmap(selectedBitmap);
                    ivReportImage.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    showError("Failed to load image.");
                }
            }
            loaderutil.hideLoader();
        }
    }

    private void addMedicineField() {
        medicineCount++;
        View medicineView = LayoutInflater.from(this)
                .inflate(R.layout.item_medicine_field, medicationsContainer, false);
        TextInputLayout medicineNameLayout = medicineView.findViewById(R.id.medicineNameLayout);
        TextInputLayout dosageLayout       = medicineView.findViewById(R.id.dosageLayout);
        medicineNameLayout.setHint("Medicine Name " + medicineCount);
        dosageLayout.setHint("Dosage " + medicineCount);
        medicationsContainer.addView(medicineView);
    }

    private void saveReport() {
        int selectedRadioId = radioGroupUploadType.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Please select an upload type.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://thedoctorathome.in/Doctors/insert_medical_report.php";
        JSONObject postData = new JSONObject();

        try {
            postData.put("appointment_id", appointmentId);
            postData.put("patient_name", etPatientName.getText().toString());
            postData.put("age", etAge.getText().toString());
            postData.put("sex", etSex.getText().toString());
            postData.put("weight", etWeight.getText().toString());
            postData.put("patient_address", etAddress.getText().toString());
            postData.put("visit_date", etDate.getText().toString());
            postData.put("temperature", etTemperature.getText().toString());
            postData.put("pulse", etPulse.getText().toString());
            postData.put("spo2", etSpo2.getText().toString());
            postData.put("blood_pressure", etBloodPressure.getText().toString());
            postData.put("report_type", etReportType.getText().toString());
            postData.put("symptoms", etSymptoms.getText().toString());
            postData.put("respiratory_system", etRespiratorySystem.getText().toString());
            postData.put("doctor_name", etSignature.getText().toString());
            postData.put("doctor_signature", etSignature.getText().toString());

            if (selectedRadioId == R.id.radioDirectUpload) {
                if (selectedBitmap == null) {
                    Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                postData.put("report_photo", getStringImage(selectedBitmap));
            } else {
                postData.put("report_photo", "");
            }

            JSONArray medicineArray = new JSONArray();
            int childCount = medicationsContainer.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = medicationsContainer.getChildAt(i);
                TextInputLayout medicineNameLayout = child.findViewById(R.id.medicineNameLayout);
                TextInputLayout dosageLayout       = child.findViewById(R.id.dosageLayout);
                String medicineName = "", dosage = "";
                if (medicineNameLayout.getEditText() != null)
                    medicineName = medicineNameLayout.getEditText().getText().toString().trim();
                if (dosageLayout.getEditText() != null)
                    dosage = dosageLayout.getEditText().getText().toString().trim();
                if (!medicineName.isEmpty() && !dosage.isEmpty()) {
                    JSONObject medObject = new JSONObject();
                    medObject.put("medicine_name", medicineName);
                    medObject.put("dosage", dosage);
                    medicineArray.put(medObject);
                }
            }
            postData.put("medicines", medicineArray);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to collect form data.", Toast.LENGTH_SHORT).show();
            return;
        }

        loaderutil.showLoader(this);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, url, postData,
                response -> {
                    loaderutil.hideLoader();
                    if (response.optBoolean("success", false)) {
                        Toast.makeText(this, "Report saved successfully.", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("report_submitted", true);
                        resultIntent.putExtra("appointment_id", appointmentId);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save report.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Error sending report to server.", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
