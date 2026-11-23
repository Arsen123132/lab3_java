package com.lab3.activitytracker;

public final class ActivityCalculator {
    private static final double STEP_LENGTH_METERS = 0.78;
    private static final double CALORIES_PER_STEP = 0.04;

    private ActivityCalculator() {
    }

    public static double distanceMeters(int steps) {
        return steps * STEP_LENGTH_METERS;
    }

    public static double calories(int steps) {
        return steps * CALORIES_PER_STEP;
    }
}

