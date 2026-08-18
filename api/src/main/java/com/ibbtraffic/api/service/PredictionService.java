package com.ibbtraffic.api.service;

import java.io.InputStream;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibbtraffic.api.entity.TrafficHistory;
import com.ibbtraffic.api.repository.TrafficHistoryRepository;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PostConstruct;

@Service
public class PredictionService {

    private OrtEnvironment environment;
    private OrtSession session;

    // map that holds the json dictionary from Python
    private Map<String, Integer> geohashMapping;

    @Autowired
    private TrafficHistoryRepository trafficHistoryRepository;

    @PostConstruct
    public void init() {
        try {
            // JSON mapping loading
            ObjectMapper mapper = new ObjectMapper();
            // path to the JSON file in the resources folder
            ClassPathResource jsonResource = new ClassPathResource("models/geohash_mapping.json");
            try (InputStream jsonStream = jsonResource.getInputStream()) {
                geohashMapping = mapper.readValue(jsonStream, new TypeReference<Map<String, Integer>>() {});
            }
            System.out.println(" Geohash Mapping loaded successfully! Total Coordinates: " + geohashMapping.size());

            // 7 parameter onnx model loading
            environment = OrtEnvironment.getEnvironment();
            // path to the ONNX model in the resources folder
            ClassPathResource modelResource = new ClassPathResource("models/traffic_model.onnx");

            try (InputStream inputStream = modelResource.getInputStream()) {
                byte[] modelBytes = inputStream.readAllBytes();
                session = environment.createSession(modelBytes, new OrtSession.SessionOptions());
            }
            System.out.println("7 Parameter XGBoost ONNX Model successfully loaded into memory!");

        } catch (Exception e) {
            System.err.println("Critical error occurred while starting service: " + e.getMessage());
            throw new RuntimeException("Service could not be started", e);
        }
    }

    // Predicting traffic speed using the ONNX model
    public float predictTraffic(float[] features) {
        try {
            float[][] inputData = new float[][]{features};
            OnnxTensor inputTensor = OnnxTensor.createTensor(environment, inputData);

            Map<String, OnnxTensor> inputs = Map.of("float_input", inputTensor);

            try (OrtSession.Result results = session.run(inputs)) {
                float[][] output = (float[][]) results.get(0).getValue();
                return output[0][0];
            }
        } catch (OrtException e) {
            throw new RuntimeException("Error occurred while running the ONNX model", e);
        }
    }

    // 7 Parameter Live Traffic Prediction Logic
    public float predictLiveTraffic(String geohashString, int hour, int dayOfWeek, int isHoliday, float temp, float precip, float wind) {

        // converting incoming Geohash text ("sx7cxs") to number (ID) from the Python dictionary
        Integer encodedGeohash = geohashMapping.get(geohashString);

        // Perfect Fallback Architecture: Don't crash the system if the model has never seen this location!
        if (encodedGeohash == null) {
            System.out.println("Model does not know this coordinate (" + geohashString + "). Fetching historical average from PostgreSQL...");

            // Referencing the 7-column database table to find the average speed for this geohash, day, hour, and holiday status
            TrafficHistory historyData = trafficHistoryRepository.findByGeohashCodeAndDayOfWeekAndHourOfDayAndIsHoliday(
                    geohashString, dayOfWeek, hour, isHoliday);

            // Return the average if data exists in database, otherwise provide default speed of 40 km/h.
            return (historyData != null) ? historyData.getAverageSpeed().floatValue() : 40.0f;
        }

        // If the location is understood by the model, create the 7-feature array.
        float[] features = new float[]{
                encodedGeohash.floatValue(), // f0: geohash_id
                (float) hour,                // f1: hour
                (float) dayOfWeek,           // f2: dayOfWeek
                (float) isHoliday,           // f3: isHoliday
                temp,                        // f4: temp
                precip,                      // f5: precip
                wind                         // f6: wind
        };

        // fire the model and return the predicted traffic speed
        return predictTraffic(features);
    }
}