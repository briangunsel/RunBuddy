package com.example.runbuddy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.LinkedList;

import com.example.runbuddy.databinding.ActivityMainBinding;

import java.util.EventListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int ACCELEROMETER_PERMISSION_REQUEST_CODE = 1002;
    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float AVERAGE_STRIDE_LENGTH = 0.73f;
    private boolean runActive = false;
    private float[] acceleration = new float[3];
    private long lastTimestamp = 0; // Store the timestamp of the last sensor update
    private int stepCount = 0;
    private double magPrev = 0;
    private float[] lastAcceleration = new float[3];
    TextView speedTextView;
    TextView stepsViewText;
    TextView distanceViewText;
    TextView accelerationViewText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        speedTextView = findViewById(R.id.speedTextView);
        stepsViewText = findViewById(R.id.stepsTextView);
        distanceViewText = findViewById(R.id.distanceTextView);
        accelerationViewText = findViewById(R.id.accelerationTextView);

        binding.startRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.GONE);
                binding.pauseRunButton.setVisibility(View.VISIBLE);
                binding.endRunButton.setVisibility(View.VISIBLE);
                binding.speedTextView.setVisibility(View.VISIBLE);
                binding.accelerationTextView.setVisibility(View.VISIBLE);
                binding.stepsTextView.setVisibility(View.VISIBLE);
                binding.distanceTextView.setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "New run started!", Toast.LENGTH_SHORT).show();

                runActive = true;
                startNewRun();
            }
        });

        binding.pauseRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.GONE);
                binding.pauseRunButton.setVisibility(View.GONE);
                binding.endRunButton.setVisibility(View.VISIBLE);
                binding.resumeRunButton.setVisibility(View.VISIBLE);
                binding.speedTextView.setVisibility(View.GONE);
                binding.accelerationTextView.setVisibility(View.GONE);
                binding.stepsTextView.setVisibility(View.GONE);
                binding.distanceTextView.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this, "Run paused!", Toast.LENGTH_SHORT).show();

                runActive = false;
                onPause();
            }
        });

        binding.endRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.VISIBLE);
                binding.pauseRunButton.setVisibility(View.GONE);
                binding.endRunButton.setVisibility(View.GONE);
                binding.resumeRunButton.setVisibility(View.GONE);
                binding.speedTextView.setVisibility(View.GONE);
                binding.accelerationTextView.setVisibility(View.GONE);
                binding.stepsTextView.setVisibility(View.GONE);
                binding.distanceTextView.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this, "Run stopped!", Toast.LENGTH_SHORT).show();

                runActive = false;
                stopRun();
            }
        });

        binding.resumeRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.GONE);
                binding.pauseRunButton.setVisibility(View.VISIBLE);
                binding.endRunButton.setVisibility(View.VISIBLE);
                binding.resumeRunButton.setVisibility(View.GONE);
                binding.speedTextView.setVisibility(View.VISIBLE);
                binding.accelerationTextView.setVisibility(View.VISIBLE);
                binding.stepsTextView.setVisibility(View.VISIBLE);
                binding.distanceTextView.setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "Run resumed!", Toast.LENGTH_SHORT).show();

                runActive = true;
                onResume();
            }
        });
    }

    public void registerListener() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    private void startAccelerometer() {
        if (accelerometer != null && runActive) {
            registerListener();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (runActive && accelerometer != null) {
            registerListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (runActive) {
            unregisterListener();
        }
    }

    // Data pre-processing when receiving updated data from sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            updateAcceleration(event);
            detectSteps(event);
            updateDistance(event);
            updateSpeed(event);

            System.arraycopy(event.values, 0, lastAcceleration, 0, 3);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ...
    }

    /*
    @SuppressLint("SetTextI18n")
    private void updateSpeed(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTimestamp;
            lastTimestamp = currentTime;

            // Copy the accelerometer values to acceleration array
            System.arraycopy(event.values, 0, acceleration, 0, 3);

            // Calculate magnitude of acceleration
            double magnitude = Math.sqrt(Math.pow(acceleration[0], 2) + Math.pow(acceleration[1], 2) + Math.pow(acceleration[2], 2));

            // Integrate acceleration to obtain velocity using trapezoidal rule
            // v(t) = v(t-1) + 0.5 * (a(t) + a(t-1)) * dt
            for (int i = 0; i < 3; i++) {
                velocity[i] += (float) (0.5 * (acceleration[i] + lastAcceleration[i]) * elapsedTime / 1000); // Convert ms to s
            }

            // Calculate speed (magnitude of velocity)
            double speed = Math.sqrt(Math.pow(velocity[0], 2) + Math.pow(velocity[1], 2) + Math.pow(velocity[2], 2));

            speedTextView.setText("Speed: " + speed + " m/s");

            // Update last acceleration values
            System.arraycopy(acceleration, 0, lastAcceleration, 0, 3);
        }
    }
    */

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateSpeed(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTimestamp;
            lastTimestamp = currentTime;

            System.arraycopy(event.values, 0, acceleration, 0, 3);

            double magnitude = Math.sqrt(Math.pow(acceleration[0], 2) + Math.pow(acceleration[1], 2) + Math.pow(acceleration[2], 2));
            double speed = magnitude * elapsedTime / 1000; // Convert ms to s

            speed = speed - 0.6;

            speedTextView.setText(String.format("Speed: %.1f m/s", speed));
        }
    }



    private void startNewRun() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, ACCELEROMETER_PERMISSION_REQUEST_CODE);
        } else {
            startAccelerometer();
        }
    }


    private void stopRun() {
        unregisterListener();
    }

    // detect steps using acceleration vector from accelerometer.
    @SuppressLint("SetTextI18n")
    private void detectSteps(SensorEvent event) {
        int step_threshold = 5; // decrease for faster pace, increase for slower pace.

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        double mag = Math.sqrt(x*x + y*y + z*z);
        double magDelta = mag - magPrev;
        magPrev = mag;

        if(magDelta > step_threshold) {
            stepCount++;
        }

        stepsViewText.setText("Steps: " + stepCount);


        // old version:
        /*
        float acceleration = (float) Math.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1] + event.values[2] * event.values[2]);

        if (acceleration > STEP_THRESHOLD && !isPeak) {
            stepCount++;
            isPeak = true;
        }

        if (acceleration < STEP_THRESHOLD) {
            isPeak = false;
        }

        stepsViewText.setText("Steps: " + stepCount);
        */
    }

    @SuppressLint("SetTextI18n")
    private void updateDistance(SensorEvent event) {
        if(runActive) {
            float distance = stepCount * AVERAGE_STRIDE_LENGTH;

            distanceViewText.setText("Distance: " + distance + "m");
        }
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateAcceleration(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] rawAcceleration = event.values.clone();
            float[] smoothedAcceleration = lowPassFilter(rawAcceleration);
            float[] gravity = lowPassFilter(smoothedAcceleration);

            // remove gravity
            float[] linearAcceleration = new float[3];
            for (int i = 0; i < 3; i++) {
                linearAcceleration[i] = smoothedAcceleration[i] - gravity[i];
            }

            float totalLinearAcceleration = (float) Math.sqrt(linearAcceleration[0] * linearAcceleration[0] + linearAcceleration[1] * linearAcceleration[1] + linearAcceleration[2] * linearAcceleration[2]);

            totalLinearAcceleration = (float) (totalLinearAcceleration - 1.5);

            accelerationViewText.setText(String.format("Acceleration: %.1f m/s2", totalLinearAcceleration));
        }
    }

    // helper function for acceleration calculations.
    private float[] lowPassFilter(float[] input) {
        float alpha = 0.8f; // higher alpha = smoother output
        float[] output = new float[3];
        if (output == null) return input;

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
}
