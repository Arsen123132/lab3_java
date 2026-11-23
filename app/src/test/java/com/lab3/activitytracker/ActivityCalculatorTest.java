package com.lab3.activitytracker;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ActivityCalculatorTest {
    @Test
    public void calculatesDistance() {
        assertEquals(780.0, ActivityCalculator.distanceMeters(1000), 0.001);
    }

    @Test
    public void calculatesCalories() {
        assertEquals(40.0, ActivityCalculator.calories(1000), 0.001);
    }
}
