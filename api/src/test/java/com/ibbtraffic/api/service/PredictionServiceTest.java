package com.ibbtraffic.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class PredictionServiceTest {

    @Autowired
    private PredictionService predictionService;

    @Test
    void testModelSanityCheck() {
        System.out.println("Sanity (Smoke) Test starting for the 7-parameter model...");

        // creating a fake array in a length of 7 with random features.
        float[] dummyFeatures = new float[]{
                120.0f,  // f0: geohash_id (an example ID)
                18.0f,   // f1: hour
                0.0f,    // f2: dayOfWeek
                0.0f,    // f3: isHoliday (0: not a holiday)
                22.5f,   // f4: temp
                0.0f,    // f5: precip
                15.0f    // f6: wind
        };

        // Ensuring the model runs without throwing exceptions
        assertDoesNotThrow(() -> {
            float actualOutput = predictionService.predictTraffic(dummyFeatures);
            System.out.println(" Model ran successfully! Predicted speed: " + actualOutput + " km/h");

            // verifying if it is logical or not.
            assertTrue(actualOutput > 0, "predicted speed must be greater than 0!");
            assertTrue(actualOutput < 150, "Predicted speed must be less than 150 km/h!");

        }, "The ONNX engine threw an error during model prediction!");
    }
}