package com.example.trashmap.AI;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс для классификации отходов с использованием ML Kit
 * Автоматически определяет тип отходов по фотографии
 */
public class WasteClassifier {
    
    private static final String TAG = "WasteClassifier";
    private ImageLabeler labeler;
    private com.google.mlkit.vision.objects.ObjectDetector objectDetector;
    private TensorFlowLiteWasteClassifier tfliteClassifier;
    private android.content.Context context;
    
    // Маппинг ключевых слов ML Kit к типам отходов с приоритетами
    // Приоритет: 1 = высокий (специфичные слова), 2 = средний, 3 = низкий (общие слова)
    // ID типов соответствуют модели: 0-Батарейки, 1-Биологические отходы, 2-Бытовые отходы,
    // 3-Картон, 4-Макулатура, 5-Металл, 6-Обувь, 7-Одежда, 8-Пластик, 9-Стекло
    private static final Map<String, WasteTypeMapping> WASTE_KEYWORDS = new HashMap<String, WasteTypeMapping>() {{
        // Батарейки (0) - расширенный словарь
        put("battery", new WasteTypeMapping(0, 1));
        put("batteries", new WasteTypeMapping(0, 1));
        put("battery cell", new WasteTypeMapping(0, 1));
        put("cell", new WasteTypeMapping(0, 2));
        put("accumulator", new WasteTypeMapping(0, 1));
        put("lithium", new WasteTypeMapping(0, 1));
        put("lithium battery", new WasteTypeMapping(0, 1));
        put("lithium ion", new WasteTypeMapping(0, 1));
        put("aa battery", new WasteTypeMapping(0, 1));
        put("aaa battery", new WasteTypeMapping(0, 1));
        put("rechargeable", new WasteTypeMapping(0, 2));
        put("power cell", new WasteTypeMapping(0, 1));
        
        // Биологические отходы (1)
        put("organic waste", new WasteTypeMapping(1, 1));
        put("biowaste", new WasteTypeMapping(1, 1));
        put("biological waste", new WasteTypeMapping(1, 1));
        put("food waste", new WasteTypeMapping(1, 1));
        put("compost", new WasteTypeMapping(1, 1));
        put("organic", new WasteTypeMapping(1, 2));
        put("biodegradable", new WasteTypeMapping(1, 2));
        
        // Бытовые отходы (2) - низкий приоритет, общие слова
        put("garbage", new WasteTypeMapping(2, 3));
        put("trash", new WasteTypeMapping(2, 3));
        put("waste", new WasteTypeMapping(2, 3));
        put("rubbish", new WasteTypeMapping(2, 3));
        put("refuse", new WasteTypeMapping(2, 3));
        put("bin", new WasteTypeMapping(2, 3));
        put("dumpster", new WasteTypeMapping(2, 3));
        put("landfill", new WasteTypeMapping(2, 3));
        
        // Картон (3)
        put("cardboard", new WasteTypeMapping(3, 1));
        put("cardboard box", new WasteTypeMapping(3, 1));
        put("carton", new WasteTypeMapping(3, 1));
        put("cardboard container", new WasteTypeMapping(3, 1));
        
        // Макулатура (4) - расширенный словарь
        put("paper", new WasteTypeMapping(4, 1));
        put("newspaper", new WasteTypeMapping(4, 1));
        put("book", new WasteTypeMapping(4, 1));
        put("document", new WasteTypeMapping(4, 2));
        put("magazine", new WasteTypeMapping(4, 1));
        put("paper bag", new WasteTypeMapping(4, 1));
        put("envelope", new WasteTypeMapping(4, 1));
        put("notebook", new WasteTypeMapping(4, 1));
        put("notepad", new WasteTypeMapping(4, 1));
        put("paperboard", new WasteTypeMapping(4, 1));
        put("card", new WasteTypeMapping(4, 2));
        put("postcard", new WasteTypeMapping(4, 1));
        put("catalog", new WasteTypeMapping(4, 1));
        put("brochure", new WasteTypeMapping(4, 1));
        
        // Металл (5) - расширенный словарь
        put("aluminum", new WasteTypeMapping(5, 1));
        put("aluminium", new WasteTypeMapping(5, 1));
        put("aluminum can", new WasteTypeMapping(5, 1));
        put("steel", new WasteTypeMapping(5, 1));
        put("iron", new WasteTypeMapping(5, 1));
        put("can", new WasteTypeMapping(5, 1));
        put("tin", new WasteTypeMapping(5, 1));
        put("tin can", new WasteTypeMapping(5, 1));
        put("copper", new WasteTypeMapping(5, 1));
        put("brass", new WasteTypeMapping(5, 1));
        put("bronze", new WasteTypeMapping(5, 1));
        put("metal can", new WasteTypeMapping(5, 1));
        put("soda can", new WasteTypeMapping(5, 1));
        put("beer can", new WasteTypeMapping(5, 1));
        put("metal container", new WasteTypeMapping(5, 1));
        put("foil", new WasteTypeMapping(5, 2));
        put("aluminum foil", new WasteTypeMapping(5, 1));
        put("metal", new WasteTypeMapping(5, 3));
        put("metallic", new WasteTypeMapping(5, 2));
        
        // Обувь (6)
        put("shoes", new WasteTypeMapping(6, 1));
        put("footwear", new WasteTypeMapping(6, 1));
        put("boots", new WasteTypeMapping(6, 1));
        put("sneakers", new WasteTypeMapping(6, 1));
        put("sandals", new WasteTypeMapping(6, 1));
        put("slippers", new WasteTypeMapping(6, 1));
        
        // Одежда (7) - расширенный словарь
        put("clothing", new WasteTypeMapping(7, 1));
        put("clothes", new WasteTypeMapping(7, 1));
        put("textile", new WasteTypeMapping(7, 1));
        put("fabric", new WasteTypeMapping(7, 1));
        put("garment", new WasteTypeMapping(7, 1));
        put("shirt", new WasteTypeMapping(7, 1));
        put("t-shirt", new WasteTypeMapping(7, 1));
        put("pants", new WasteTypeMapping(7, 1));
        put("trousers", new WasteTypeMapping(7, 1));
        put("dress", new WasteTypeMapping(7, 1));
        put("jacket", new WasteTypeMapping(7, 1));
        put("coat", new WasteTypeMapping(7, 1));
        put("sweater", new WasteTypeMapping(7, 1));
        put("hoodie", new WasteTypeMapping(7, 1));
        put("jeans", new WasteTypeMapping(7, 1));
        put("shorts", new WasteTypeMapping(7, 1));
        put("socks", new WasteTypeMapping(7, 1));
        
        // Пластик (8) - расширенный словарь
        put("plastic", new WasteTypeMapping(8, 1));
        put("plastic bottle", new WasteTypeMapping(8, 1));
        put("plastic bag", new WasteTypeMapping(8, 1));
        put("plastic container", new WasteTypeMapping(8, 1));
        put("plastic packaging", new WasteTypeMapping(8, 1));
        put("polyethylene", new WasteTypeMapping(8, 1));
        put("polypropylene", new WasteTypeMapping(8, 1));
        put("pet", new WasteTypeMapping(8, 1));
        put("pvc", new WasteTypeMapping(8, 1));
        put("hdpe", new WasteTypeMapping(8, 1));
        put("ldpe", new WasteTypeMapping(8, 1));
        put("ps", new WasteTypeMapping(8, 1));
        put("polystyrene", new WasteTypeMapping(8, 1));
        put("packaging", new WasteTypeMapping(8, 2));
        put("bottle", new WasteTypeMapping(8, 2)); // По умолчанию пластик
        put("water bottle", new WasteTypeMapping(8, 1));
        put("soda bottle", new WasteTypeMapping(8, 1));
        put("soft drink", new WasteTypeMapping(8, 2));
        put("container", new WasteTypeMapping(8, 2));
        put("bag", new WasteTypeMapping(8, 2));
        put("shopping bag", new WasteTypeMapping(8, 1));
        put("straw", new WasteTypeMapping(8, 1));
        put("cup", new WasteTypeMapping(8, 2));
        put("disposable", new WasteTypeMapping(8, 2));
        
        // Стекло (9) - расширенный словарь
        put("glass", new WasteTypeMapping(9, 1));
        put("glass bottle", new WasteTypeMapping(9, 1));
        put("glass jar", new WasteTypeMapping(9, 1));
        put("jar", new WasteTypeMapping(9, 1));
        put("bottle glass", new WasteTypeMapping(9, 1));
        put("wine bottle", new WasteTypeMapping(9, 1));
        put("beer bottle", new WasteTypeMapping(9, 1));
        put("window", new WasteTypeMapping(9, 2));
        put("glassware", new WasteTypeMapping(9, 1));
        put("crystal", new WasteTypeMapping(9, 1));
        put("mirror", new WasteTypeMapping(9, 2));
        put("glass container", new WasteTypeMapping(9, 1));
    }};
    
    // Контекстные правила: комбинации ключевых слов, которые однозначно определяют тип
    // ID соответствуют модели: 0-Батарейки, 1-Биологические отходы, 2-Бытовые отходы,
    // 3-Картон, 4-Макулатура, 5-Металл, 6-Обувь, 7-Одежда, 8-Пластик, 9-Стекло
    private static final Map<String, Integer> CONTEXT_RULES = new HashMap<String, Integer>() {{
        // Если есть "glass" И "bottle" -> стекло (9) (приоритет над общим "bottle")
        put("glass+bottle", 9);
        put("glass+jar", 9);
        
        // Если есть "plastic" И "bottle" -> пластик (8)
        put("plastic+bottle", 8);
        put("plastic+bag", 8);
        put("plastic+container", 8);
        
        // Если есть "aluminum" ИЛИ "steel" И "can" -> металл (5)
        put("aluminum+can", 5);
        put("aluminium+can", 5);
        put("steel+can", 5);
        put("metal+can", 5);
        
        // Если есть "paper" И "bag" -> макулатура (4)
        put("paper+bag", 4);
        put("cardboard+box", 4);
        
        // Если есть "lithium" И "battery" -> батарейки (0)
        put("lithium+battery", 0);
        put("rechargeable+battery", 0);
    }};
    
    // Вспомогательный класс для хранения типа и приоритета
    private static class WasteTypeMapping {
        int type;
        int priority; // 1 = высокий, 2 = средний, 3 = низкий
        
        WasteTypeMapping(int type, int priority) {
            this.type = type;
            this.priority = priority;
        }
    }
    
    // Вспомогательный класс для хранения меток объектов
    private static class ObjectLabelInfo {
        String text;
        float confidence;
        
        ObjectLabelInfo(String text, float confidence) {
            this.text = text;
            this.confidence = confidence;
        }
    }
    
    public interface ClassificationCallback {
        void onClassificationResult(int predictedType, String predictedName, float confidence);
        void onError(String error);
    }
    
    public WasteClassifier() {
        // Инициализация Image Labeling для текстовых меток
        ImageLabelerOptions labelOptions = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.3f)
                .build();
        labeler = ImageLabeling.getClient(labelOptions);
        
        // Инициализация Object Detection для обнаружения объектов
        ObjectDetectorOptions detectorOptions = new ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build();
        objectDetector = ObjectDetection.getClient(detectorOptions);
    }
    
    /**
     * Конструктор с контекстом для инициализации TensorFlow Lite модели
     */
    public WasteClassifier(android.content.Context context) {
        this();
        this.context = context;
        // Пытаемся загрузить TensorFlow Lite модель
        try {
            tfliteClassifier = new TensorFlowLiteWasteClassifier(context);
            if (tfliteClassifier.isModelLoaded()) {
                Log.d(TAG, "TensorFlow Lite модель успешно загружена и будет использоваться");
            } else {
                Log.w(TAG, "TensorFlow Lite модель не загружена, будет использоваться ML Kit");
            }
        } catch (Exception e) {
            Log.w(TAG, "Не удалось инициализировать TensorFlow Lite модель", e);
            tfliteClassifier = null;
        }
    }
    
    /**
     * Классифицирует изображение отходов используя TensorFlow Lite модель (приоритет) или ML Kit
     * @param bitmap Изображение для классификации
     * @param callback Callback с результатом классификации
     */
    public void classifyWaste(Bitmap bitmap, ClassificationCallback callback) {
        // ПРИОРИТЕТ 1: Используем TensorFlow Lite модель, если она загружена
        if (tfliteClassifier != null && tfliteClassifier.isModelLoaded()) {
            Log.d(TAG, "Используем TensorFlow Lite модель для классификации");
            // Создаем адаптер для преобразования callback
            tfliteClassifier.classifyWaste(bitmap, new TensorFlowLiteWasteClassifier.ClassificationCallback() {
                @Override
                public void onClassificationResult(int predictedType, String predictedName, float confidence) {
                    callback.onClassificationResult(predictedType, predictedName, confidence);
                }
                
                @Override
                public void onError(String error) {
                    callback.onError(error);
                }
            });
            return;
        }
        
        // ПРИОРИТЕТ 2: Используем ML Kit (Object Detection + Image Labeling)
        Log.d(TAG, "Используем ML Kit для классификации");
        classifyWasteWithMLKit(bitmap, callback);
    }
    
    /**
     * Классифицирует изображение отходов используя комбинацию Object Detection и Image Labeling
     * @param bitmap Изображение для классификации
     * @param callback Callback с результатом классификации
     */
    private void classifyWasteWithMLKit(Bitmap bitmap, ClassificationCallback callback) {
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            
            // Используем Object Detection для обнаружения объектов
            objectDetector.process(image)
                    .addOnSuccessListener(detectedObjects -> {
                        Log.d(TAG, "Обнаружено объектов: " + detectedObjects.size());
                        
                        // Если объекты обнаружены, используем их классификацию
                        if (!detectedObjects.isEmpty()) {
                            List<ObjectLabelInfo> objectLabels = new ArrayList<>();
                            
                            // Собираем метки от обнаруженных объектов
                            for (DetectedObject detectedObject : detectedObjects) {
                                for (DetectedObject.Label label : detectedObject.getLabels()) {
                                    objectLabels.add(new ObjectLabelInfo(label.getText(), label.getConfidence()));
                                    Log.d(TAG, "Объект обнаружен: " + label.getText() + 
                                          " (уверенность: " + label.getConfidence() + 
                                          ", координаты: " + detectedObject.getBoundingBox() + ")");
                                }
                            }
                            
                            // Если есть метки от объектов, используем их
                            if (!objectLabels.isEmpty()) {
                                int predictedType = predictWasteTypeFromObjects(objectLabels);
                                String predictedName = getWasteTypeName(predictedType);
                                float confidence = calculateConfidenceFromObjects(objectLabels, predictedType);
                                
                                Log.d(TAG, "Распознан тип по объектам: " + predictedName + 
                                      " (id: " + predictedType + ", уверенность: " + confidence + ")");
                                
                                callback.onClassificationResult(predictedType, predictedName, confidence);
                                return;
                            }
                        }
                        
                        // Если Object Detection не дал результатов, используем Image Labeling
                        Log.d(TAG, "Object Detection не дал результатов, используем Image Labeling");
                        useImageLabeling(image, callback);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Object Detection не удался, используем Image Labeling", e);
                        // Если Object Detection не работает, используем Image Labeling как запасной вариант
                        useImageLabeling(image, callback);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания InputImage", e);
            callback.onError("Ошибка обработки изображения: " + e.getMessage());
        }
    }
    
    /**
     * Использует Image Labeling как запасной вариант
     */
    private void useImageLabeling(InputImage image, ClassificationCallback callback) {
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    if (labels.isEmpty()) {
                        callback.onError("Не удалось распознать объект на изображении");
                        return;
                    }
                    
                    // Анализируем метки и определяем тип отходов
                    int predictedType = predictWasteType(labels);
                    String predictedName = getWasteTypeName(predictedType);
                    float confidence = calculateConfidence(labels, predictedType);
                    
                    Log.d(TAG, "Распознан тип по меткам: " + predictedName + " (id: " + predictedType + 
                            ", уверенность: " + confidence + ")");
                    
                    callback.onClassificationResult(predictedType, predictedName, confidence);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при классификации", e);
                    callback.onError("Ошибка распознавания: " + e.getMessage());
                });
    }
    
    /**
     * Предсказывает тип отходов на основе меток объектов (Object Detection)
     */
    private int predictWasteTypeFromObjects(List<ObjectLabelInfo> objectLabels) {
        Map<Integer, Float> typeScores = new HashMap<>();
        List<String> allLabels = new ArrayList<>();
        
        // Собираем все метки в список для контекстного анализа
        for (ObjectLabelInfo label : objectLabels) {
            allLabels.add(label.text.toLowerCase().trim());
        }
        
        // Логируем все метки для отладки
        Log.d(TAG, "Получено меток от Object Detection: " + objectLabels.size());
        for (ObjectLabelInfo label : objectLabels) {
            Log.d(TAG, "  - " + label.text + " (уверенность: " + label.confidence + ")");
        }
        
        // ШАГ 1: Проверяем контекстные правила
        for (Map.Entry<String, Integer> rule : CONTEXT_RULES.entrySet()) {
            String[] keywords = rule.getKey().split("\\+");
            int ruleType = rule.getValue();
            
            boolean allKeywordsFound = true;
            float minConfidence = 1.0f;
            
            for (String keyword : keywords) {
                boolean keywordFound = false;
                for (ObjectLabelInfo label : objectLabels) {
                    String labelText = label.text.toLowerCase().trim();
                    if (labelText.contains(keyword)) {
                        keywordFound = true;
                        minConfidence = Math.min(minConfidence, label.confidence);
                        break;
                    }
                }
                if (!keywordFound) {
                    allKeywordsFound = false;
                    break;
                }
            }
            
            if (allKeywordsFound) {
                float contextScore = minConfidence * 5.0f;
                typeScores.put(ruleType, typeScores.getOrDefault(ruleType, 0f) + contextScore);
                Log.d(TAG, "Контекстное правило применено: '" + rule.getKey() + "' -> тип " + ruleType);
            }
        }
        
        // ШАГ 2: Подсчитываем баллы по отдельным меткам
        for (ObjectLabelInfo label : objectLabels) {
            String labelText = label.text.toLowerCase().trim();
            float confidence = label.confidence;
            
            boolean found = false;
            for (Map.Entry<String, WasteTypeMapping> entry : WASTE_KEYWORDS.entrySet()) {
                String keyword = entry.getKey().toLowerCase();
                WasteTypeMapping mapping = entry.getValue();
                
                if (labelText.equals(keyword) || 
                    labelText.contains(" " + keyword + " ") || 
                    labelText.startsWith(keyword + " ") || 
                    labelText.endsWith(" " + keyword) ||
                    (keyword.length() > 4 && labelText.contains(keyword))) {
                    
                    int type = mapping.type;
                    int priority = mapping.priority;
                    float weightedScore = confidence * (4 - priority);
                    
                    typeScores.put(type, typeScores.getOrDefault(type, 0f) + weightedScore);
                    found = true;
                    Log.d(TAG, "Найдено совпадение: '" + keyword + "' -> тип " + type);
                }
            }
            
            if (!found) {
                String[] words = labelText.split("[\\s,.-]+");
                for (String word : words) {
                    if (word.length() > 3) {
                        WasteTypeMapping mapping = WASTE_KEYWORDS.get(word);
                        if (mapping != null) {
                            int type = mapping.type;
                            int priority = mapping.priority;
                            float weightedScore = confidence * (4 - priority) * 0.5f;
                            
                            typeScores.put(type, typeScores.getOrDefault(type, 0f) + weightedScore);
                            Log.d(TAG, "Частичное совпадение: '" + word + "' -> тип " + type);
                        }
                    }
                }
            }
        }
        
        // Находим тип с максимальным баллом
        int bestType = 0;
        float maxScore = 0f;
        
        for (Map.Entry<Integer, Float> entry : typeScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestType = entry.getKey();
            }
        }
        
        if (maxScore < 0.2f) {
            Log.d(TAG, "Низкий балл (" + maxScore + "), возвращаем бытовые отходы");
            return 0;
        }
        
        Log.d(TAG, "Выбран тип по объектам: " + bestType + " (" + getWasteTypeName(bestType) + ") с баллом " + maxScore);
        return bestType;
    }
    
    /**
     * Вычисляет уверенность для меток объектов
     */
    private float calculateConfidenceFromObjects(List<ObjectLabelInfo> objectLabels, int predictedType) {
        float totalConfidence = 0f;
        float maxConfidence = 0f;
        int count = 0;
        
        for (ObjectLabelInfo label : objectLabels) {
            String labelText = label.text.toLowerCase().trim();
            
            for (Map.Entry<String, WasteTypeMapping> entry : WASTE_KEYWORDS.entrySet()) {
                String keyword = entry.getKey().toLowerCase();
                WasteTypeMapping mapping = entry.getValue();
                
                if (mapping.type == predictedType) {
                    if (labelText.equals(keyword) || 
                        labelText.contains(" " + keyword + " ") || 
                        labelText.startsWith(keyword + " ") || 
                        labelText.endsWith(" " + keyword) ||
                        (keyword.length() > 4 && labelText.contains(keyword))) {
                        
                        totalConfidence += label.confidence;
                        maxConfidence = Math.max(maxConfidence, label.confidence);
                        count++;
                    }
                }
            }
        }
        
        if (count > 0) {
            float avgConfidence = totalConfidence / count;
            return (avgConfidence * 0.6f + maxConfidence * 0.4f);
        }
        
        return 0.5f;
    }
    
    /**
     * Предсказывает тип отходов на основе меток ML Kit с использованием контекстных правил
     */
    private int predictWasteType(List<ImageLabel> labels) {
        Map<Integer, Float> typeScores = new HashMap<>();
        List<String> allLabels = new ArrayList<>();
        
        // Собираем все метки в список для контекстного анализа
        for (ImageLabel label : labels) {
            allLabels.add(label.getText().toLowerCase().trim());
        }
        
        // Логируем все метки для отладки
        Log.d(TAG, "Получено меток от ML Kit: " + labels.size());
        for (ImageLabel label : labels) {
            Log.d(TAG, "  - " + label.getText() + " (уверенность: " + label.getConfidence() + ")");
        }
        
        // ШАГ 1: Проверяем контекстные правила (комбинации ключевых слов)
        for (Map.Entry<String, Integer> rule : CONTEXT_RULES.entrySet()) {
            String[] keywords = rule.getKey().split("\\+");
            int ruleType = rule.getValue();
            
            // Проверяем, есть ли все ключевые слова из правила в метках
            boolean allKeywordsFound = true;
            float minConfidence = 1.0f;
            
            for (String keyword : keywords) {
                boolean keywordFound = false;
                for (ImageLabel label : labels) {
                    String labelText = label.getText().toLowerCase().trim();
                    if (labelText.contains(keyword)) {
                        keywordFound = true;
                        minConfidence = Math.min(minConfidence, label.getConfidence());
                        break;
                    }
                }
                if (!keywordFound) {
                    allKeywordsFound = false;
                    break;
                }
            }
            
            // Если все ключевые слова найдены, применяем правило с высоким приоритетом
            if (allKeywordsFound) {
                float contextScore = minConfidence * 5.0f; // Высокий вес для контекстных правил
                typeScores.put(ruleType, typeScores.getOrDefault(ruleType, 0f) + contextScore);
                Log.d(TAG, "Контекстное правило применено: '" + rule.getKey() + "' -> тип " + ruleType + 
                      " (балл: " + contextScore + ")");
            }
        }
        
        // ШАГ 2: Подсчитываем баллы для каждого типа отходов по отдельным меткам
        for (ImageLabel label : labels) {
            String labelText = label.getText().toLowerCase().trim();
            float confidence = label.getConfidence();
            
            // Сначала проверяем точные совпадения (более длинные фразы)
            boolean found = false;
            for (Map.Entry<String, WasteTypeMapping> entry : WASTE_KEYWORDS.entrySet()) {
                String keyword = entry.getKey().toLowerCase();
                WasteTypeMapping mapping = entry.getValue();
                
                // Проверяем точное совпадение или вхождение слова целиком
                if (labelText.equals(keyword) || 
                    labelText.contains(" " + keyword + " ") || 
                    labelText.startsWith(keyword + " ") || 
                    labelText.endsWith(" " + keyword) ||
                    (keyword.length() > 4 && labelText.contains(keyword))) {
                    
                    int type = mapping.type;
                    int priority = mapping.priority;
                    
                    // Учитываем приоритет: высокий приоритет увеличивает вес
                    float weightedScore = confidence * (4 - priority); // 1->3x, 2->2x, 3->1x
                    
                    typeScores.put(type, typeScores.getOrDefault(type, 0f) + weightedScore);
                    found = true;
                    Log.d(TAG, "Найдено совпадение: '" + keyword + "' -> тип " + type + 
                          " (приоритет: " + priority + ", вес: " + weightedScore + ")");
                }
            }
            
            // Если не нашли точного совпадения, проверяем части слов
            if (!found) {
                String[] words = labelText.split("[\\s,.-]+");
                for (String word : words) {
                    if (word.length() > 3) { // Игнорируем слишком короткие слова
                        WasteTypeMapping mapping = WASTE_KEYWORDS.get(word);
                        if (mapping != null) {
                            int type = mapping.type;
                            int priority = mapping.priority;
                            float weightedScore = confidence * (4 - priority) * 0.5f; // Меньший вес для частичных совпадений
                            
                            typeScores.put(type, typeScores.getOrDefault(type, 0f) + weightedScore);
                            Log.d(TAG, "Частичное совпадение: '" + word + "' -> тип " + type);
                        }
                    }
                }
            }
        }
        
        // ШАГ 3: Применяем правила исключения (если есть контекстное правило, снижаем вес общих слов)
        // Например, если есть "glass bottle", то просто "bottle" не должно давать баллы пластику
        for (Map.Entry<String, Integer> rule : CONTEXT_RULES.entrySet()) {
            String[] keywords = rule.getKey().split("\\+");
            int ruleType = rule.getValue();
            
            // Проверяем, применено ли это правило
            boolean ruleApplied = false;
            for (String keyword : keywords) {
                boolean keywordFound = false;
                for (String label : allLabels) {
                    if (label.contains(keyword)) {
                        keywordFound = true;
                        break;
                    }
                }
                if (!keywordFound) {
                    break;
                }
                ruleApplied = true;
            }
            
            // Если правило применено, снижаем вес конфликтующих общих слов
            if (ruleApplied) {
                // Например, если "glass+bottle" применено (тип 2), снижаем вес "bottle" для типа 1
                if (ruleType == 2 && typeScores.containsKey(1)) {
                    // Снижаем балл пластика, если есть стеклянная бутылка
                    typeScores.put(1, typeScores.get(1) * 0.3f);
                    Log.d(TAG, "Применено правило исключения: снижен вес типа 1 из-за правила 'glass+bottle'");
                }
            }
        }
        
        // Логируем все набранные баллы
        Log.d(TAG, "Итоговые баллы по типам:");
        for (Map.Entry<Integer, Float> entry : typeScores.entrySet()) {
            Log.d(TAG, "  Тип " + entry.getKey() + " (" + getWasteTypeName(entry.getKey()) + "): " + entry.getValue());
        }
        
        // Находим тип с максимальным баллом
        int bestType = 0; // По умолчанию - бытовые отходы
        float maxScore = 0f;
        
        for (Map.Entry<Integer, Float> entry : typeScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestType = entry.getKey();
            }
        }
        
        // Если не нашли совпадений или балл слишком низкий, возвращаем бытовые отходы
        if (maxScore < 0.2f) {
            Log.d(TAG, "Низкий балл (" + maxScore + "), возвращаем бытовые отходы");
            return 0;
        }
        
        Log.d(TAG, "Выбран тип: " + bestType + " (" + getWasteTypeName(bestType) + ") с баллом " + maxScore);
        return bestType;
    }
    
    /**
     * Вычисляет уверенность в предсказании
     */
    private float calculateConfidence(List<ImageLabel> labels, int predictedType) {
        float totalConfidence = 0f;
        float maxConfidence = 0f;
        int count = 0;
        
        for (ImageLabel label : labels) {
            String labelText = label.getText().toLowerCase().trim();
            float labelConfidence = label.getConfidence();
            
            // Проверяем совпадения для предсказанного типа
            for (Map.Entry<String, WasteTypeMapping> entry : WASTE_KEYWORDS.entrySet()) {
                String keyword = entry.getKey().toLowerCase();
                WasteTypeMapping mapping = entry.getValue();
                
                if (mapping.type == predictedType) {
                    if (labelText.equals(keyword) || 
                        labelText.contains(" " + keyword + " ") || 
                        labelText.startsWith(keyword + " ") || 
                        labelText.endsWith(" " + keyword) ||
                        (keyword.length() > 4 && labelText.contains(keyword))) {
                        
                        totalConfidence += labelConfidence;
                        maxConfidence = Math.max(maxConfidence, labelConfidence);
                        count++;
                    }
                }
            }
        }
        
        // Используем среднее значение или максимальное, в зависимости от количества совпадений
        if (count > 0) {
            float avgConfidence = totalConfidence / count;
            // Комбинируем среднее и максимальное для более точной оценки
            return (avgConfidence * 0.6f + maxConfidence * 0.4f);
        }
        
        return 0.5f; // Значение по умолчанию при отсутствии совпадений
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
     * Освобождает ресурсы
     */
    public void close() {
        if (labeler != null) {
            labeler.close();
        }
        if (objectDetector != null) {
            objectDetector.close();
        }
        if (tfliteClassifier != null) {
            tfliteClassifier.close();
        }
    }
}

