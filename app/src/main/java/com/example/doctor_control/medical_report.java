package com.example.doctor_control;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

public class medical_report extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private LinearLayout virtualReportForm, directUploadSection, medicationsContainer;
    private RadioGroup radioGroupUploadType;
    private ImageView ivReportImage;
    private MaterialButton btnUploadImage, btnSaveReport, btnAddMedicine;
    private Uri selectedImageUri;
    private int medicineCount = 1; // Counter for medicine fields

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_report);
        // Initialize views and setup listeners after setting the content view
        initializeViews();
        setupRadioGroup();
    }

    @SuppressLint("WrongViewCast")
    private void initializeViews() {
        virtualReportForm = findViewById(R.id.virtual_report_form);
        directUploadSection = findViewById(R.id.direct_upload_section);
        radioGroupUploadType = findViewById(R.id.radioGroupUploadType);
        ivReportImage = findViewById(R.id.ivReportImage);
        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnSaveReport = findViewById(R.id.btnSaveReport);
        btnAddMedicine = findViewById(R.id.btnAddMedicine);
        medicationsContainer = findViewById(R.id.medications_container);

        btnUploadImage.setOnClickListener(v -> showImagePickerDialog());
        btnSaveReport.setOnClickListener(v -> saveReport());
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

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                ivReportImage.setImageBitmap(photo);
                ivReportImage.setVisibility(View.VISIBLE);
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    ivReportImage.setImageBitmap(bitmap);
                    ivReportImage.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                    showError("Failed to load image.");
                }
            }
        }
    }

    private void addMedicineField() {
        medicineCount++;

        // Inflate new medicine field layout
        View medicineView = LayoutInflater.from(this).inflate(R.layout.item_medicine_field, medicationsContainer, false);

        // Set dynamic hints
        TextInputLayout medicineNameLayout = medicineView.findViewById(R.id.medicineNameLayout);
        TextInputLayout dosageLayout = medicineView.findViewById(R.id.dosageLayout);

        medicineNameLayout.setHint("Medicine Name " + medicineCount);
        dosageLayout.setHint("Dosage " + medicineCount);

        // Add the new medicine field to the container
        medicationsContainer.addView(medicineView);
    }

    private void saveReport() {
        if (radioGroupUploadType.getCheckedRadioButtonId() == R.id.radioVirtualReport) {
            showSuccess("Virtual Report Saved Successfully");
        } else if (selectedImageUri != null || ivReportImage.getDrawable() != null) {
            showSuccess("Image Report Saved Successfully");
        } else {
            showError("Please upload an image.");
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
