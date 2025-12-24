package com.example.trashmap.AI;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для работы с TensorFlow Lite моделью классификации отходов
 * Использует обученную модель для точного распознавания типов отходов
 */
public class TensorFlowLiteWasteClassifier {
    
    private static final String TAG = "TFLiteWasteClassifier";
    private static final String MODEL_FILE = "ml_models/waste_classifier.tflite";
    private static final String LABELS_FILE = "ml_models/labels.txt";  // Используем labels.txt из модели
    
    // Размеры входного изображения для модели (обычно 224x224 или 299x299)
    private static final int INPUT_IMAGE_WIDTH = 224;
    private static final int INPUT_IMAGE_HEIGHT = 224;
    
    private Interpreter tflite;
    private List<String> labels;
    private ImageProcessor imageProcessor;
    private Context context;
    private boolean isModelLoaded = false;
    
    // ID типов в приложении теперь совпадают с ID классов модели
    // Модель обучена на 10 классах: Батарейки, Биологические отходы, Бытовые отходы, 
    // Картон, Макулатура, Металл, Обувь, Одежда, Пластик, Стекло
    // Порядок соответствует файлу labels.txt
    
    public interface ClassificationCallback {
        void onClassificationResult(int predictedType, String predictedName, float confidence);
        void onError(String error);
    }
    
    public TensorFlowLiteWasteClassifier(Context context) {
        this.context = context;
        initializeImageProcessor();
        loadModel();
    }
    
    /**
     * Инициализация процессора изображений для предобработки
     * Важно: нормализация будет выполнена вручную после resize
     */
    private void initializeImageProcessor() {
        imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(INPUT_IMAGE_HEIGHT, INPUT_IMAGE_WIDTH, ResizeOp.ResizeMethod.BILINEAR))
                .build();
    }
    
    /**
     * Нормализует значения пикселей из диапазона [0, 255] в [0, 1]
     * Это соответствует rescale=1./255 при обучении модели
     * Возвращает новый TensorBuffer с нормализованными значениями
     */
    private TensorBuffer normalizeTensorBuffer(TensorBuffer buffer) {
        float[] floatArray = buffer.getFloatArray();
        // Нормализуем каждый пиксель: делим на 255.0
        for (int i = 0; i < floatArray.length; i++) {
            floatArray[i] = floatArray[i] / 255.0f;
        }
        // Создаем новый TensorBuffer с нормализованными значениями
        TensorBuffer normalizedBuffer = TensorBuffer.createFixedSize(
            buffer.getShape(), 
            org.tensorflow.lite.DataType.FLOAT32
        );
        normalizedBuffer.loadArray(floatArray);
        return normalizedBuffer;
    }
    
    /**
     * Загрузка модели TensorFlow Lite из assets
     */
    private void loadModel() {
        try {
            // Проверяем наличие файла модели
            String[] files = context.getAssets().list("ml_models");
            boolean modelExists = false;
            if (files != null) {
                for (String file : files) {
                    if (file.equals("waste_classifier.tflite")) {
                        modelExists = true;
                        break;
                    }
                }
            }
            
            if (!modelExists) {
                Log.w(TAG, "Файл модели " + MODEL_FILE + " не найден в assets/ml_models/");
                Log.w(TAG, "Доступные файлы: " + (files != null ? java.util.Arrays.toString(files) : "нет"));
                isModelLoaded = false;
                return;
            }
            
            // Загружаем модель из assets
            ByteBuffer modelBuffer = loadModelFile(MODEL_FILE);
            tflite = new Interpreter(modelBuffer);
            isModelLoaded = true;
            Log.d(TAG, "Модель успешно загружена");
            
            // Загружаем метки классов
            loadLabels();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки модели", e);
            isModelLoaded = false;
        }
    }
    
    /**
     * Загрузка файла модели из assets
     */
    private ByteBuffer loadModelFile(String modelPath) throws IOException {
        try {
            java.io.InputStream inputStream = context.getAssets().open(modelPath);
            byte[] modelBytes = new byte[inputStream.available()];
            inputStream.read(modelBytes);
            inputStream.close();
            
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocateDirect(modelBytes.length);
            buffer.put(modelBytes);
            return buffer.asReadOnlyBuffer();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки файла модели: " + modelPath, e);
            throw e;
        }
    }
    
    /**
     * Загрузка меток классов из файла
     */
    private void loadLabels() {
        try {
            java.io.InputStream inputStream = context.getAssets().open(LABELS_FILE);
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, "UTF-8"));
            labels = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Пропускаем пустые строки
                if (line.isEmpty()) {
                    continue;
                }
                // Удаляем номера в начале строки (например, "1Батарейки" → "Батарейки")
                line = line.replaceFirst("^\\d+\\s*", "");
                if (!line.isEmpty()) {
                    labels.add(line);
                }
            }
            reader.close();
            inputStream.close();
            
            if (labels.size() != 10) {
                Log.w(TAG, "Ожидалось 10 меток, загружено: " + labels.size());
            }
            Log.d(TAG, "Загружено меток из файла: " + labels.size());
            for (int i = 0; i < labels.size(); i++) {
                Log.d(TAG, "  [" + i + "] " + labels.get(i));
            }
        } catch (IOException e) {
            Log.w(TAG, "Не удалось загрузить метки из файла, используем стандартные", e);
            // Используем стандартные метки, если файл не найден
            labels = getDefaultLabels();
        }
    }
    
    /**
     * Стандартные метки классов (если файл labels.txt отсутствует)
     * Соответствует порядку классов в обученной модели
     * Порядок: 0-Батарейки, 1-Биологические отходы, 2-Бытовые отходы, 3-Картон,
     * 4-Макулатура, 5-Металл, 6-Обувь, 7-Одежда, 8-Пластик, 9-Стекло
     */
    private List<String> getDefaultLabels() {
        List<String> defaultLabels = new ArrayList<>();
        defaultLabels.add("Батарейки");
        defaultLabels.add("Биологические отходы");
        defaultLabels.add("Бытовые отходы");
        defaultLabels.add("Картон");
        defaultLabels.add("Макулатура");
        defaultLabels.add("Металл");
        defaultLabels.add("Обувь");
        defaultLabels.add("Одежда");
        defaultLabels.add("Пластик");
        defaultLabels.add("Стекло");
        return defaultLabels;
    }
    
    /**
     * Классифицирует изображение отходов с помощью TensorFlow Lite модели
     * @param bitmap Изображение для классификации
     * @param callback Callback с результатом классификации
     */
    public void classifyWaste(Bitmap bitmap, ClassificationCallback callback) {
        if (!isModelLoaded || tflite == null) {
            callback.onError("Модель не загружена. Убедитесь, что файл " + MODEL_FILE + " находится в папке assets/");
            return;
        }
        
        try {
            // Проверяем входное изображение
            if (bitmap == null || bitmap.isRecycled()) {
                callback.onError("Изображение недействительно");
                return;
            }
            
            Log.d(TAG, "Начало классификации. Размер изображения: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            
            // Предобработка изображения
            TensorImage tensorImage = new TensorImage(org.tensorflow.lite.DataType.FLOAT32);
            tensorImage.load(bitmap);
            
            // Применяем предобработку (resize)
            tensorImage = imageProcessor.process(tensorImage);
            
            // Получаем информацию о входном тензоре
            TensorBuffer inputBuffer = tensorImage.getTensorBuffer();
            
            // Нормализуем значения из [0, 255] в [0, 1] (как при обучении: rescale=1./255)
            inputBuffer = normalizeTensorBuffer(inputBuffer);
            
            // Получаем информацию о выходном тензоре
            int[] outputShape = tflite.getOutputTensor(0).shape();
            int numClasses = outputShape[outputShape.length - 1]; // Последний размер - количество классов
            
            Log.d(TAG, "Входной тензор: " + java.util.Arrays.toString(tflite.getInputTensor(0).shape()));
            Log.d(TAG, "Выходной тензор: " + java.util.Arrays.toString(outputShape));
            Log.d(TAG, "Количество классов: " + numClasses);
            
            TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, org.tensorflow.lite.DataType.FLOAT32);
            
            // Запускаем инференс
            long startTime = System.currentTimeMillis();
            
            // Запускаем инференс используя простой метод run()
            tflite.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());
            
            long inferenceTime = System.currentTimeMillis() - startTime;
            
            Log.d(TAG, "Инференс выполнен за " + inferenceTime + " мс");
            
            // Получаем результаты из выходного буфера
            float[] probabilities = outputBuffer.getFloatArray();
            
            // Логируем все вероятности для отладки
            Log.d(TAG, "Все вероятности классов:");
            for (int i = 0; i < Math.min(probabilities.length, labels.size()); i++) {
                Log.d(TAG, String.format("  [%d] %s: %.4f", i, labels.get(i), probabilities[i]));
            }
            
            // Обрабатываем результаты
            processResults(probabilities, callback);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при классификации", e);
            e.printStackTrace();
            callback.onError("Ошибка обработки изображения: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает результаты инференса и определяет тип отходов
     */
    private void processResults(float[] probabilities, ClassificationCallback callback) {
        // Проверяем, что массив вероятностей не пустой
        if (probabilities == null || probabilities.length == 0) {
            Log.e(TAG, "Массив вероятностей пуст");
            callback.onError("Ошибка: модель не вернула результаты");
            return;
        }
        
        // Находим индекс класса с максимальной вероятностью
        int maxIndex = 0;
        float maxProbability = probabilities[0];
        
        for (int i = 1; i < probabilities.length; i++) {
            if (probabilities[i] > maxProbability) {
                maxProbability = probabilities[i];
                maxIndex = i;
            }
        }
        
        // Проверяем уверенность модели
        // Если максимальная вероятность слишком низкая, это может указывать на проблему
        if (maxProbability < 0.1f) {
            Log.w(TAG, "Низкая уверенность модели: " + maxProbability + ". Возможно, изображение не подходит для классификации.");
        }
        
        // ID класса модели теперь совпадает с ID типа в приложении
        int predictedType = maxIndex;
        if (predictedType < 0 || predictedType >= 10) {
            // Если индекс выходит за пределы, используем бытовые отходы по умолчанию
            predictedType = 2;
            Log.w(TAG, "Индекс класса " + maxIndex + " выходит за пределы, используем бытовые отходы");
        }
        
        String predictedName = getWasteTypeName(predictedType);
        float confidence = maxProbability;
        
        // Логируем топ-3 предсказания для отладки
        Log.d(TAG, "Топ-3 предсказания:");
        // Создаем массив индексов для сортировки
        Integer[] indices = new Integer[probabilities.length];
        for (int i = 0; i < probabilities.length; i++) {
            indices[i] = i;
        }
        java.util.Arrays.sort(indices, (a, b) -> Float.compare(probabilities[b], probabilities[a]));
        
        for (int i = 0; i < Math.min(3, indices.length); i++) {
            int idx = indices[i];
            String label = (idx < labels.size()) ? labels.get(idx) : "unknown";
            Log.d(TAG, String.format("  %d. [%d] %s: %.4f (%.1f%%)", 
                i + 1, idx, label, probabilities[idx], probabilities[idx] * 100));
        }
        
        Log.d(TAG, "Финальный результат: " + predictedName + " (id: " + predictedType + 
              ", уверенность: " + String.format("%.2f%%", confidence * 100) + ")");
        
        callback.onClassificationResult(predictedType, predictedName, confidence);
    }
    
    /**
     * Получает название типа отходов по ID
     * Порядок соответствует модели: 0-Батарейки, 1-Биологические отходы, 2-Бытовые отходы,
     * 3-Картон, 4-Макулатура, 5-Металл, 6-Обувь, 7-Одежда, 8-Пластик, 9-Стекло
     */
    private String getWasteTypeName(int typeId) {
        String[] types = {
            "Батарейки",           // 0
            "Биологические отходы", // 1
            "Бытовые отходы",       // 2
            "Картон",              // 3
            "Макулатура",          // 4
            "Металл",              // 5
            "Обувь",               // 6
            "Одежда",              // 7
            "Пластик",             // 8
            "Стекло"               // 9
        };
        
        if (typeId >= 0 && typeId < types.length) {
            return types[typeId];
        }
        return "Бытовые отходы";
    }
    
    /**
     * Проверяет, загружена ли модель
     */
    public boolean isModelLoaded() {
        return isModelLoaded;
    }
    
    /**
     * Освобождает ресурсы
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
            isModelLoaded = false;
        }
    }
}

