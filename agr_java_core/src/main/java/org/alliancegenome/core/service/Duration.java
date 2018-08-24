package org.alliancegenome.core.service;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Setter
@Getter
public class Duration {

    long seconds;
    long minutes;
    long hours;
    long days;
    java.time.Duration duration;
    String display;

    public Duration(LocalDateTime startTime, LocalDateTime endTime) {
        duration = java.time.Duration.between(startTime, endTime);
        display = LocalTime.MIDNIGHT.plus(duration).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        minutes = duration.toMinutes();
        hours = duration.toHours();
    }

    @Override
    public String toString() {
        return display;
    }
}
