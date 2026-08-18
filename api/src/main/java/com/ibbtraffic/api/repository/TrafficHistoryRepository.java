package com.ibbtraffic.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ibbtraffic.api.entity.TrafficHistory;

@Repository
public interface TrafficHistoryRepository extends JpaRepository<TrafficHistory, Long> {

    // finding the data model needs
    TrafficHistory findByGeohashCodeAndDayOfWeekAndHourOfDayAndIsHoliday(
            String geohashCode,
            Integer dayOfWeek,
            Integer hourOfDay,
            Integer isHoliday
    );
}