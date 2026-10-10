package com.mbstu.labbooking.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class PenaltyCalculator {

    public static final double LATE_FINE_PER_DAY = 20.0;   // Tk per day
    public static final double DAMAGE_FINE       = 500.0;  // flat fine

    /** Days late (0 if returned on time). */
    public static long daysLate(LocalDate dueDate, LocalDate returnDate) {
        long diff = ChronoUnit.DAYS.between(dueDate, returnDate);
        return Math.max(diff, 0);
    }

    public static double lateFine(LocalDate dueDate, LocalDate returnDate) {
        return daysLate(dueDate, returnDate) * LATE_FINE_PER_DAY;
    }
}
