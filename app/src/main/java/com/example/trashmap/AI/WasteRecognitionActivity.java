package com.example.trashmap.AI;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.Encyclopedia.EncyclopediaActivity;
import com.example.trashmap.Encyclopedia.InfoAboutItem;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.MainActivity;
import com.example.trashmap.ProfileActivity;
import com.example.trashmap.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WasteRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "WasteRecognition";
    private static final int REQUEST_IMAGE_CAPTURE = 102;

    private WasteClassifier wasteClassifier;
    private WasteRecommendations wasteRecommendations;

    private ImageView imageView;
    private Button btnTakePhoto;
    private Button btnSelectPhoto;
    private Button btnRecognize;
    private TextView resultText;
    private TextView recommendationsText;
    private TextView confidenceText;
    private ProgressBar progressBar;
    private LinearLayout resultContainer;
    private LinearLayout recommendationsContainer;
    private Button btnViewEncyclopedia;

    private Bitmap currentBitmap;
    private int recognizedType = -1;
    private String recognizedName = "";
    private float confidence = 0f;

    private List<GarbageType> garbageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_recognition);

        // Инициализация ИИ компонентов с контекстом для TensorFlow Lite модели
        wasteClassifier = new WasteClassifier(this);
        wasteRecommendations = new WasteRecommendations();

        // Получаем список типов отходов
        getGarbageTypes();

        // Инициализация UI
        initViews();

        // Настройка навигации
        setupNavigation();

        // Обработчики кнопок
        setupButtons();
    }

    private void initViews() {
        imageView = findViewById(R.id.wr_image_view);
        btnTakePhoto = findViewById(R.id.wr_btn_take_photo);
        btnSelectPhoto = findViewById(R.id.wr_btn_select_photo);
        btnRecognize = findViewById(R.id.wr_btn_recognize);
        resultText = findViewById(R.id.wr_result_text);
        recommendationsText = findViewById(R.id.wr_recommendations_text);
        confidenceText = findViewById(R.id.wr_confidence_text);
        progressBar = findViewById(R.id.wr_progress_bar);
        resultContainer = findViewById(R.id.wr_result_container);
        recommendationsContainer = findViewById(R.id.wr_recommendations_container);
        btnViewEncyclopedia = findViewById(R.id.wr_btn_view_encyclopedia);

        // Скрываем контейнеры результатов изначально
        resultContainer.setVisibility(View.GONE);
        recommendationsContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.wr_bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.menu_ai_recognition);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.menu_encyclopedia:
                    intent = new Intent(getApplicationContext(), EncyclopediaActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (java.io.Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                case R.id.menu_map:
                    intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (java.io.Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                case R.id.menu_profile:
                    intent = new Intent(getApplicationContext(), ProfileActivity.class);
                    intent.putExtra(Constant.GARBAGE_KEY, (java.io.Serializable) garbageList);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                case R.id.menu_ai_recognition:
                    return true;
            }
            return false;
        });
    }

    private void setupButtons() {
        // Выбор фото из галереи
        ActivityResultLauncher<String> mGetContent = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            imageView.setImageURI(uri);
                            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            btnRecognize.setEnabled(true);
                            clearResults();
                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка загрузки изображения", e);
                            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnSelectPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));

        // Съемка фото
        btnTakePhoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
            } else {
                dispatchTakePictureIntent();
            }
        });

        // Распознавание
        btnRecognize.setOnClickListener(v -> {
            if (currentBitmap != null) {
                recognizeWaste();
            } else {
                Toast.makeText(this, "Сначала выберите или сфотографируйте отходы", Toast.LENGTH_SHORT).show();
            }
        });

        // Переход к энциклопедии
        btnViewEncyclopedia.setOnClickListener(v -> {
            if (recognizedType >= 0 && garbageList != null) {
                Intent intent = new Intent(getApplicationContext(), InfoAboutItem.class);
                intent.putExtra(Constant.GARBAGE_KEY, (java.io.Serializable) garbageList);
                intent.putExtra(Constant.GARBAGE_TYPE, recognizedType);
                startActivity(intent);
            }
        });
    }

    private void recognizeWaste() {
        if (currentBitmap == null) {
            Toast.makeText(this, "Изображение не загружено", Toast.LENGTH_SHORT).show();
            return;
        }

        // Показываем прогресс
        progressBar.setVisibility(View.VISIBLE);
        btnRecognize.setEnabled(false);
        resultContainer.setVisibility(View.GONE);
        recommendationsContainer.setVisibility(View.GONE);

        wasteClassifier.classifyWaste(currentBitmap, new WasteClassifier.ClassificationCallback() {
            @Override
            public void onClassificationResult(int predictedType, String predictedName, float confidenceValue) {
                runOnUiThread(() -> {
                    recognizedType = predictedType;
                    recognizedName = predictedName;
                    confidence = confidenceValue;

                    // Отображаем результат
                    displayResult(predictedType, predictedName, confidenceValue);

                    // Получаем и отображаем рекомендации
                    displayRecommendations(predictedType);

                    progressBar.setVisibility(View.GONE);
                    btnRecognize.setEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Ошибка распознавания: " + error);
                    Toast.makeText(WasteRecognitionActivity.this,
                            "Ошибка распознавания: " + error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnRecognize.setEnabled(true);
                });
            }
        });
    }

    private void displayResult(int type, String name, float confidenceValue) {
        resultText.setText(String.format("Распознанный тип: %s", name));
        confidenceText.setText(String.format("Уверенность: %.0f%%", confidenceValue * 100));
        resultContainer.setVisibility(View.VISIBLE);
    }

    private void displayRecommendations(int type) {
        String recommendations = wasteRecommendations.getRecommendations(type);
        if (recommendations != null && !recommendations.isEmpty()) {
            recommendationsText.setText(recommendations);
            recommendationsContainer.setVisibility(View.VISIBLE);
            btnViewEncyclopedia.setVisibility(View.VISIBLE);
        }
    }

    private void clearResults() {
        resultContainer.setVisibility(View.GONE);
        recommendationsContainer.setVisibility(View.GONE);
        btnViewEncyclopedia.setVisibility(View.GONE);
        recognizedType = -1;
    }

    private String currentPhotoPath;

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "WR_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Ошибка создания файла", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                currentBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                imageView.setImageBitmap(currentBitmap);
                btnRecognize.setEnabled(true);
                clearResults();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка обработки фото", e);
                Toast.makeText(this, "Ошибка обработки фото", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Разрешение на камеру необходимо для съемки фото", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getGarbageTypes() {
        Intent i = getIntent();
        garbageList = (List<GarbageType>) i.getSerializableExtra(Constant.GARBAGE_KEY);
        if (garbageList == null) {
            garbageList = new java.util.ArrayList<>();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wasteClassifier != null) {
            wasteClassifier.close();
        }
    }
}

