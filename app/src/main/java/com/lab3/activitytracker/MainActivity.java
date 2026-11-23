package com.lab3.activitytracker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String PREFS_NAME = "activity_tracker";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_DATE = "date";
    private static final String KEY_COUNTER_BASELINE = "counter_baseline";

    private TextView stepsValue;
    private TextView caloriesValue;
    private TextView distanceValue;
    private TextView sensorStatus;
    private SensorManager sensorManager;
    private Sensor stepCounter;
    private Sensor stepDetector;
    private Sensor accelerometer;
    private SharedPreferences preferences;
    private int todaySteps;
    private float counterBaseline = -1f;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startSensors();
                } else {
                    sensorStatus.setText(R.string.permission_required);
                    AppLogger.w("ACTIVITY_RECOGNITION permission denied", null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepsValue = findViewById(R.id.stepsValue);
        caloriesValue = findViewById(R.id.caloriesValue);
        distanceValue = findViewById(R.id.distanceValue);
        sensorStatus = findViewById(R.id.sensorStatus);
        Button resetButton = findViewById(R.id.resetButton);

        preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        resetDailyDataIfNeeded();
        restoreCounterBaseline();
        updateUi();

        resetButton.setOnClickListener(view -> {
            todaySteps = 0;
            counterBaseline = -1f;
            saveState();
            updateUi();
            AppLogger.i("Daily activity values reset");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetDailyDataIfNeeded();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
        } else {
            startSensors();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        AppLogger.d("Sensor listeners stopped");
    }

    private void startSensors() {
        if (sensorManager == null) {
            sensorStatus.setText(R.string.sensor_unavailable);
            return;
        }

        stepCounter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (stepCounter != null) {
            sensorManager.registerListener(this, stepCounter, SensorManager.SENSOR_DELAY_NORMAL);
            sensorStatus.setText(R.string.sensor_step_counter);
            AppLogger.i("Step counter sensor started");
        } else if (stepDetector != null) {
            sensorManager.registerListener(this, stepDetector, SensorManager.SENSOR_DELAY_NORMAL);
            sensorStatus.setText(R.string.sensor_step_detector);
            AppLogger.i("Step detector sensor started");
        } else if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            sensorStatus.setText(R.string.sensor_accelerometer);
            AppLogger.i("Accelerometer fallback started");
        } else {
            sensorStatus.setText(R.string.sensor_unavailable);
            AppLogger.w("No activity sensors available", null);
        }
    }

    private long lastStepTime;
    private double previousMagnitude = 9.81;

    @Override
    public void onSensorChanged(SensorEvent event) {
        resetDailyDataIfNeeded();

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float cumulativeCount = event.values[0];
            if (counterBaseline < 0f) {
                counterBaseline = cumulativeCount;
                saveState();
            }
            int detectedSteps = Math.max(0, (int) (cumulativeCount - counterBaseline));
            todaySteps = detectedSteps;
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            todaySteps++;
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectStepWithAccelerometer(event.values);
        }

        saveState();
        updateUi();
    }

    private void detectStepWithAccelerometer(float[] values) {
        double magnitude = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
        double change = Math.abs(magnitude - previousMagnitude);
        previousMagnitude = magnitude;

        long now = System.currentTimeMillis();
        if (change > 2.0 && now - lastStepTime > 350) {
            lastStepTime = now;
            todaySteps++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        AppLogger.d("Sensor accuracy changed: " + sensor.getType() + " -> " + accuracy);
    }

    private void resetDailyDataIfNeeded() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = preferences.getString(KEY_DATE, "");
        if (!today.equals(savedDate)) {
            todaySteps = 0;
            counterBaseline = -1f;
            preferences.edit()
                    .putString(KEY_DATE, today)
                    .putInt(KEY_STEPS, 0)
                    .putFloat(KEY_COUNTER_BASELINE, -1f)
                    .apply();
            AppLogger.i("Started new activity day: " + today);
        } else {
            todaySteps = preferences.getInt(KEY_STEPS, 0);
        }
    }

    private void restoreCounterBaseline() {
        counterBaseline = preferences.getFloat(KEY_COUNTER_BASELINE, -1f);
    }

    private void saveState() {
        preferences.edit()
                .putInt(KEY_STEPS, todaySteps)
                .putFloat(KEY_COUNTER_BASELINE, counterBaseline)
                .apply();
    }

    private void updateUi() {
        stepsValue.setText(String.format(Locale.getDefault(), "%d", todaySteps));
        caloriesValue.setText(String.format(Locale.getDefault(), "%.1f", ActivityCalculator.calories(todaySteps)));
        double meters = ActivityCalculator.distanceMeters(todaySteps);
        if (meters >= 1000) {
            distanceValue.setText(String.format(Locale.getDefault(), "%.2f km", meters / 1000));
        } else {
            distanceValue.setText(String.format(Locale.getDefault(), "%.0f m", meters));
        }
    }
}

