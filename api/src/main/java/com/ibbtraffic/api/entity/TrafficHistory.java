package com.ibbtraffic.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "traffic_history")
public class TrafficHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Encrypted code of regions like Kadıköy, Şişli (example: sxk9)
    @Column(name = "geohash_code", nullable = false, length = 10)
    private String geohashCode;

    // Day of the week (0: Monday, 6: Sunday)
    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    // Hour of the day (0 - 23)
    @Column(name = "hour_of_day", nullable = false)
    private Integer hourOfDay;

    // Holiday status (0: Working day, 1: Holiday)
    @Column(name = "is_holiday", nullable = false)
    private Integer isHoliday;

    // Average traffic speed in that region, on that day, at that hour, and in that holiday status
    @Column(name = "average_speed", nullable = false)
    private Double averageSpeed;

    // Average temperature (°C)
    @Column(name = "avg_temp", nullable = false)
    private Double avgTemp;

    // Average precipitation (mm)
    @Column(name = "avg_precip", nullable = false)
    private Double avgPrecip;

    // Average wind speed (km/h)
    @Column(name = "avg_wind", nullable = false)
    private Double avgWind;

    public TrafficHistory() {
        // needed by JPA
    }

    public TrafficHistory(String geohashCode, Integer dayOfWeek, Integer hourOfDay, Integer isHoliday,
                          Double averageSpeed, Double avgTemp, Double avgPrecip, Double avgWind) {
        this.geohashCode = geohashCode;
        this.dayOfWeek = dayOfWeek;
        this.hourOfDay = hourOfDay;
        this.isHoliday = isHoliday;
        this.averageSpeed = averageSpeed;
        this.avgTemp = avgTemp;
        this.avgPrecip = avgPrecip;
        this.avgWind = avgWind;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGeohashCode() { return geohashCode; }
    public void setGeohashCode(String geohashCode) { this.geohashCode = geohashCode; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getHourOfDay() { return hourOfDay; }
    public void setHourOfDay(Integer hourOfDay) { this.hourOfDay = hourOfDay; }

    public Integer getIsHoliday() { return isHoliday; }
    public void setIsHoliday(Integer isHoliday) { this.isHoliday = isHoliday; }

    public Double getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(Double averageSpeed) { this.averageSpeed = averageSpeed; }

    public Double getAvgTemp() { return avgTemp; }
    public void setAvgTemp(Double avgTemp) { this.avgTemp = avgTemp; }

    public Double getAvgPrecip() { return avgPrecip; }
    public void setAvgPrecip(Double avgPrecip) { this.avgPrecip = avgPrecip; }

    public Double getAvgWind() { return avgWind; }
    public void setAvgWind(Double avgWind) { this.avgWind = avgWind; }
}