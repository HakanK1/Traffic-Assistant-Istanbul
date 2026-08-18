package com.ibbtraffic.api.controller;

import com.ibbtraffic.api.service.PredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/v1/traffic")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService; // Dependency Injection of the PredictionService.
    }

    // API request
    public record LivePredictionRequest(
            String geohashString,
            int hour,
            int dayOfWeek,
            int isHoliday,
            float temp,
            float precip,
            float wind
    ) {}

    // adding the 7-parameter route prediction request
    public record RoutePredictionRequest(
            List<String> geohashList,
            int hour,
            int dayOfWeek,
            int isHoliday,
            float temp,
            float precip,
            float wind
    ) {}

    @PostMapping("/predict")
    public ResponseEntity<?> predict(@RequestBody LivePredictionRequest request) {
        try {
            // parameter passing, all the data.
            float predictedSpeed = predictionService.predictLiveTraffic(
                    request.geohashString(),
                    request.hour(),
                    request.dayOfWeek(),
                    request.isHoliday(),
                    request.temp(),
                    request.precip(),
                    request.wind()
            );

            return ResponseEntity.ok(Map.of(
                    "geohash", request.geohashString(),
                    "predicted_speed_kmh", predictedSpeed
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/predict-route")
    public ResponseEntity<?> predictRoute(@RequestBody RoutePredictionRequest request) {
        try {
            double totalTimeMinutes = 0.0;
            double assumedDistancePerGeohashKm = 0.6;

            for (String geohash : request.geohashList()) {
                // 7 parameter sending for route: Weather is taken into account for each point on the route
                float speed = predictionService.predictLiveTraffic(
                        geohash,
                        request.hour(),
                        request.dayOfWeek(),
                        request.isHoliday(),
                        request.temp(),
                        request.precip(),
                        request.wind()
                );

                if (speed < 5.0f) speed = 5.0f;

                double timeForSegment = (assumedDistancePerGeohashKm / speed) * 60.0;
                totalTimeMinutes += timeForSegment;
            }

            return ResponseEntity.ok(Map.of(
                    "estimated_time_minutes", Math.round(totalTimeMinutes),
                    "geohash_count", request.geohashList().size()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}