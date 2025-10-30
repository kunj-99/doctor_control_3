package com.infowave.doctor_control;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class AnimalVirtualReportActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_GALLERY = 1002;

    private RadioGroup radioGroupUploadType;
    private RadioButton radioVirtualReport, radioDirectUpload;
    private LinearLayout virtualForm, directUploadSection;
    private ImageView ivReportImage;
    private android.widget.Button btnUploadImage, btnCaptureImage, btnSave, btnAddMedicine, btnUploadDirect;
    private LinearLayout medsContainer;

    private TextInputEditText etAnimalName, etSpeciesBreed, etSex, etAge, etWeight,
            etAddress, etDate, etTemperature, etPulse, etSpo2, etBloodPressure,
            etRespiratoryRateBpm, etPainScore, etHydrationStatus, etMucousMembranes,
            etCrtSec, etBehaviorGait, etSkinCoat, etSymptoms, etRespiratorySystem,
            etReasons, etInvestigationNotes, etSignature, etReportType;

    // Vaccination UI (must exist in XML as per your last layout)
    private TextInputLayout tilVaccinationName;     // R.id.tilVaccinationName
    private TextInputEditText etVaccinationName;    // R.id.etVaccinationName
    private View tvVaccinationNotesLabel;           // R.id.tvVaccinationNotesLabel (TextView label)
    private TextInputLayout tilVaccineNotes;        // R.id.tilVaccineNotes
    private TextInputEditText etVaccineNotes;       // R.id.etVaccineNotes

    private CheckBox cbInvestigation;

    private Uri selectedImageUri;
    private Bitmap selectedBitmap;
    private RequestQueue requestQueue;
    private String appointmentId;

    // Vaccination intent flags/data
    private boolean hasVaccinationFromIntent = false;
    private String vaccinationIdFromIntent = null;
    private String vaccinationNameFromIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_virtual_report);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        WindowInsetsControllerCompat wic =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);
        wic.setAppearanceLightNavigationBars(false);

        final View root = findViewById(R.id.root_container_animal);
        final View statusScrim = findViewById(R.id.status_bar_scrim);
        final View navScrim = findViewById(R.id.navigation_bar_scrim);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (statusScrim != null) {
                statusScrim.getLayoutParams().height = sys.top;
                statusScrim.setLayoutParams(statusScrim.getLayoutParams());
                statusScrim.setVisibility(sys.top > 0 ? View.VISIBLE : View.GONE);
            }
            if (navScrim != null) {
                navScrim.getLayoutParams().height = sys.bottom;
                navScrim.setLayoutParams(navScrim.getLayoutParams());
                navScrim.setVisibility(sys.bottom > 0 ? View.VISIBLE : View.GONE);
            }
            v.setPadding(sys.left, 0, sys.right, 0);
            return insets;
        });

        initializeViews();
        setupRadioGroup();

        // ---- Read vaccination extras from Intent ----
        Intent it = getIntent();
        appointmentId = (it != null) ? it.getStringExtra("appointment_id") : null;
        if (it != null) {
            vaccinationNameFromIntent = trimOrNull(it.getStringExtra("vaccination_name"));
            vaccinationIdFromIntent   = trimOrNull(it.getStringExtra("vaccination_id"));
        }
        hasVaccinationFromIntent = !isEmpty(vaccinationNameFromIntent) || !isEmpty(vaccinationIdFromIntent);

        // Apply visibility & prefill rules as requested
        if (hasVaccinationFromIntent) {
            // Show both Name + Notes UI
            setVaccinationVisibility(true);
            // Prefill name from intent; clear notes so doctor can write
            if (etVaccinationName != null) etVaccinationName.setText(nvl(vaccinationNameFromIntent));
            if (etVaccineNotes != null) etVaccineNotes.setText("");
        } else {
            // Hide both if not present in Intent
            setVaccinationVisibility(false);
        }

        requestQueue = Volley.newRequestQueue(this);

        if (!TextUtils.isEmpty(appointmentId)) {
            fetchAppointmentDetails(appointmentId.trim());
        }
    }

    private void initializeViews() {
        radioGroupUploadType = findViewById(R.id.radioGroupUploadType);
        radioVirtualReport   = findViewById(R.id.radioVirtualReport);
        radioDirectUpload    = findViewById(R.id.radioDirectUpload);
        virtualForm          = findViewById(R.id.virtual_report_form);
        directUploadSection  = findViewById(R.id.direct_upload_section);
        ivReportImage        = findViewById(R.id.ivReportImage);
        btnUploadImage       = findViewById(R.id.btnUploadImage);
        btnCaptureImage      = findViewById(R.id.btnCaptureImage);
        btnSave              = findViewById(R.id.btnSaveReport);
        btnAddMedicine       = findViewById(R.id.btnAddMedicine);
        btnUploadDirect      = findViewById(R.id.btnUploadDirect);
        medsContainer        = findViewById(R.id.medications_container);

        etAnimalName         = findViewById(R.id.etAnimalName);
        etSpeciesBreed       = findViewById(R.id.etSpeciesBreed);
        etSex                = findViewById(R.id.etSex);
        etAge                = findViewById(R.id.etAge);
        etWeight             = findViewById(R.id.etWeight);
        etAddress            = findViewById(R.id.etAddress);
        etDate               = findViewById(R.id.etDate);
        etTemperature        = findViewById(R.id.etTemperature);
        etPulse              = findViewById(R.id.etPulse);
        etSpo2               = findViewById(R.id.etSpo2);
        etBloodPressure      = findViewById(R.id.etBloodPressure);
        etRespiratoryRateBpm = findViewById(R.id.etRespiratoryRateBpm);
        etPainScore          = findViewById(R.id.etPainScore);
        etHydrationStatus    = findViewById(R.id.etHydrationStatus);
        etMucousMembranes    = findViewById(R.id.etMucousMembranes);
        etCrtSec             = findViewById(R.id.etCrtSec);
        etBehaviorGait       = findViewById(R.id.etBehaviorGait);
        etSkinCoat           = findViewById(R.id.etSkinCoat);
        etSymptoms           = findViewById(R.id.etSymptoms);
        etRespiratorySystem  = findViewById(R.id.etRespiratorySystem);
        etReasons            = findViewById(R.id.etReasons);
        cbInvestigation      = findViewById(R.id.cbInvestigation);
        etInvestigationNotes = findViewById(R.id.etInvestigationNotes);
        etSignature          = findViewById(R.id.etSignature);
        etReportType         = findViewById(R.id.etReportType);

        // Vaccination UI (present in XML)
        tilVaccinationName      = findViewById(R.id.tilVaccinationName);
        etVaccinationName       = findViewById(R.id.etVaccinationName);
        tvVaccinationNotesLabel = findViewById(R.id.tvVaccinationNotesLabel);
        tilVaccineNotes         = findViewById(R.id.tilVaccineNotes);
        etVaccineNotes          = findViewById(R.id.etVaccineNotes);

        // Buttons
        btnUploadImage.setOnClickListener(v -> openGallery());
        btnCaptureImage.setOnClickListener(v -> openCamera());
        ivReportImage.setOnClickListener(v -> openGallery()); // optional
        btnUploadDirect.setOnClickListener(v -> uploadDirectImage());
        btnSave.setOnClickListener(v -> saveReport());
        btnAddMedicine.setOnClickListener(v -> addMedicineRow(null, null));

        // Investigation toggle
        final TextInputLayout tilInvestigationNotes = findViewById(R.id.tilInvestigationNotes);
        if (cbInvestigation != null && tilInvestigationNotes != null) {
            cbInvestigation.setOnCheckedChangeListener((button, checked) -> {
                tilInvestigationNotes.setVisibility(checked ? View.VISIBLE : View.GONE);
                if (!checked && etInvestigationNotes != null) etInvestigationNotes.setText(null);
            });
        }
    }

    private void setVaccinationVisibility(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        if (tilVaccinationName != null)      tilVaccinationName.setVisibility(vis);
        if (tvVaccinationNotesLabel != null) tvVaccinationNotesLabel.setVisibility(vis);
        if (tilVaccineNotes != null)         tilVaccineNotes.setVisibility(vis);

        if (!visible) {
            if (etVaccinationName != null) etVaccinationName.setText(null);
            if (etVaccineNotes != null)    etVaccineNotes.setText(null);
        }
    }

    private void uploadDirectImage() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
            return;
        }
        saveReport();
    }

    private void setupRadioGroup() {
        radioGroupUploadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVirtualReport) {
                virtualForm.setVisibility(View.VISIBLE);
                directUploadSection.setVisibility(View.GONE);
            } else {
                virtualForm.setVisibility(View.GONE);
                directUploadSection.setVisibility(View.VISIBLE);
            }
        });
        radioVirtualReport.setChecked(true);
    }

    private void openCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.DISPLAY_NAME, "animal_report_" + System.currentTimeMillis() + ".jpg");
        cv.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        selectedImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @SuppressLint("IntentReset")
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA && selectedImageUri != null) {
                previewImage(selectedImageUri);
            } else if (requestCode == REQUEST_GALLERY && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                previewImage(selectedImageUri);
            }
        }
    }

    private void previewImage(Uri uri) {
        try {
            selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            ivReportImage.setImageBitmap(selectedBitmap);
            ivReportImage.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReport() {
        int selectedRadioId = radioGroupUploadType.getCheckedRadioButtonId();
        if (selectedRadioId == -1) {
            Toast.makeText(this, "Please select an upload type.", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = ApiConfig.endpoint("Doctors/insert_vet_report.php");
        JSONObject postData = new JSONObject();

        try {
            // Identity & meta
            postData.put("appointment_id", nvl(appointmentId));
            postData.put("animal_name", text(etAnimalName));
            postData.put("species_breed", text(etSpeciesBreed));
            postData.put("sex", text(etSex));
            postData.put("age_years", text(etAge));
            postData.put("weight_kg", text(etWeight));
            postData.put("owner_address", text(etAddress));

            postData.put("report_date", text(etDate));
            postData.put("report_title", "Animal Virtual Report");
            postData.put("report_type", text(etReportType));
            postData.put("doctor_signature", text(etSignature));

            // Vitals
            postData.put("temperature_c", text(etTemperature));
            postData.put("pulse_bpm", text(etPulse));
            postData.put("spo2_pct", text(etSpo2));
            postData.put("bp_mmhg", text(etBloodPressure));
            postData.put("respiratory_rate_bpm", text(etRespiratoryRateBpm));
            postData.put("pain_score_0_10", text(etPainScore));
            postData.put("hydration_status", text(etHydrationStatus));
            postData.put("mucous_membranes", text(etMucousMembranes));
            postData.put("crt_sec", text(etCrtSec));

            // Systems/notes
            postData.put("behavior_gait", text(etBehaviorGait));
            postData.put("skin_coat", text(etSkinCoat));
            postData.put("symptoms", text(etSymptoms));
            postData.put("respiratory_system", text(etRespiratorySystem));
            postData.put("reasons", text(etReasons));

            // Investigation
            boolean requiresInv = cbInvestigation != null && cbInvestigation.isChecked();
            postData.put("requires_investigation", requiresInv ? 1 : 0);
            postData.put("investigation_notes", text(etInvestigationNotes));

            // Medications (two JSON arrays as strings)
            JSONArray medsNames = new JSONArray();
            JSONArray medsDoses = new JSONArray();
            for (int i = 0; i < medsContainer.getChildCount(); i++) {
                View row = medsContainer.getChildAt(i);
                TextInputEditText nameEt = row.findViewById(R.id.etMedicine);
                TextInputEditText doseEt = row.findViewById(R.id.etDosage);
                String mName = (nameEt != null && nameEt.getText() != null) ? nameEt.getText().toString().trim() : "";
                String mDose = (doseEt != null && doseEt.getText() != null) ? doseEt.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(mName)) medsNames.put(mName);
                if (!TextUtils.isEmpty(mDose)) medsDoses.put(mDose);
            }
            postData.put("medications_json", medsNames.toString());
            postData.put("dosage_json", medsDoses.toString());

            // Vaccination: send ONLY if it was present in Intent (shown to user)
            if (hasVaccinationFromIntent) {
                // ID from intent (optional)
                if (!isEmpty(vaccinationIdFromIntent)) {
                    postData.put("vaccination_id", vaccinationIdFromIntent);
                } else {
                    postData.put("vaccination_id", JSONObject.NULL);
                }
                // Name: prefer edited value in UI; fallback to intent
                String vxNameFinal = (etVaccinationName != null) ? text(etVaccinationName) : nvl(vaccinationNameFromIntent);
                postData.put("vaccination_name", vxNameFinal);

                // Notes typed by doctor
                String vxNotes = (etVaccineNotes != null) ? text(etVaccineNotes) : "";
                postData.put("vaccination_notes", vxNotes);
            }
            // If not present, we omit vaccination_* keys → backend ignores.

            // Attachment
            if (selectedRadioId == R.id.radioDirectUpload) {
                if (selectedBitmap == null) {
                    Toast.makeText(this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
                    return;
                }
                postData.put("attachment_url", getStringImage(selectedBitmap));
            } else {
                postData.put("attachment_url", "");
            }

            postData.put("is_followup", 0);

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
                        setResult(RESULT_OK);
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

    // ------------- helpers -------------

    private static String text(TextInputEditText et) {
        return (et != null && et.getText() != null) ? et.getText().toString().trim() : "";
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String getStringImage(Bitmap bmp) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);
    }

    private void addMedicineRow(String prefillName, String prefillDose) {
        View row = getLayoutInflater().inflate(R.layout._row_medicine_animal, medsContainer, false);
        TextInputEditText etName = row.findViewById(R.id.etMedicine);
        TextInputEditText etDose = row.findViewById(R.id.etDosage);
        ImageButton btnRemove = row.findViewById(R.id.btnRemoveRow);
        if (prefillName != null) etName.setText(prefillName);
        if (prefillDose != null) etDose.setText(prefillDose);
        btnRemove.setOnClickListener(v -> medsContainer.removeView(row));
        medsContainer.addView(row);
    }

    private void fetchAppointmentDetails(String id) {
        String url = ApiConfig.endpoint("Doctors/get_appointment_details.php", "appointment_id", id);
        loaderutil.showLoader(this);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET, url, null,
                res -> {
                    loaderutil.hideLoader();
                    try {
                        if (!res.optBoolean("success", false)) {
                            Toast.makeText(this, "Appointment not found", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        etAnimalName.setText(res.optString("full_name", ""));
                        etAge.setText(res.optString("age", ""));
                        etSex.setText(res.optString("sex", ""));
                        etAddress.setText(res.optString("address", ""));
                        etDate.setText(res.optString("date", ""));
                        etSignature.setText(res.optString("doctor_name", ""));
                        String speciesBreed = res.optString("species_breed", "");
                        if (!TextUtils.isEmpty(speciesBreed)) etSpeciesBreed.setText(speciesBreed);
                    } catch (Exception ex) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    loaderutil.hideLoader();
                    Toast.makeText(this, "Failed to fetch appointment details", Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(req);
    }
}
