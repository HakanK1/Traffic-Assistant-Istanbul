package com.ibbtraffic.api.dto;

public record PredictionRequest(
        String geohashCode, // String like 'sxk9qd' will come from the Frontend; will be converted to a number (ID) in the Service 
        Integer hour,       // Hour between 0-23
        Integer dayOfWeek,  // Day between 0-6
        Integer isHoliday,  // 0: Working day, 1: Holiday
        Float temp,         // Temperature forecast
        Float precip,       // Precipitation forecast
        Float wind          // Wind speed forecast
) {}