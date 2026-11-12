package com.kitchenflow.kds.common.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class BusinessDayService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private final Clock clock;

    public BusinessDayService(Clock clock) {
        this.clock = clock;
    }

    public LocalDate businessDayOf(Instant instant, LocalTime cutoff) {
        var local = instant.atZone(BUSINESS_ZONE);
        return local.toLocalTime().isBefore(cutoff) ? local.toLocalDate().minusDays(1) : local.toLocalDate();
    }

    public LocalDate currentBusinessDay() {
        return businessDayOf(clock.instant(), LocalTime.of(4, 0));
    }
}
