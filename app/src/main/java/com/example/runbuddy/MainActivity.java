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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.LinkedList;

import com.example.runbuddy.databinding.ActivityMainBinding;

import java.util.EventListener;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int ACCELEROMETER_PERMISSION_REQUEST_CODE = 1002;
    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean runActive = false;
    private static final int WINDOW_SIZE = 10; // Adjust window size as needed
    private LinkedList<Float> accelerationBuffer = new LinkedList<>();
    private float prevAcceleration = 0.0f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private long lastTimestamp = 0;
    private float[] velocity = new float[3];
    private int currentSteps = 0;

    TextView speedTextView;
    TextView stepsViewText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        speedTextView = findViewById(R.id.speedTextView);
        stepsViewText = findViewById(R.id.stepsTextView);

        binding.startRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.GONE);
                binding.pauseRunButton.setVisibility(View.VISIBLE);
                binding.endRunButton.setVisibility(View.VISIBLE);
                binding.speedTextView.setVisibility(View.VISIBLE);
                binding.stepsTextView.setVisibility(View.VISIBLE);

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
                binding.stepsTextView.setVisibility(View.GONE);

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
                binding.stepsTextView.setVisibility(View.GONE);

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
                binding.stepsTextView.setVisibility(View.VISIBLE);

                Toast.makeText(MainActivity.this, "Run resumed!", Toast.LENGTH_SHORT).show();

                runActive = true;
                onResume();
            }
        });
    }

    private void startAccelerometer() {
        if (accelerometer != null && runActive) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (runActive && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (runActive) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(runActive) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                updateSpeed(event);
                detectSteps(event);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ...
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void updateSpeed(SensorEvent event) {
        if (runActive) {
            if (lastTimestamp != 0) {
                final float dt = (event.timestamp - lastTimestamp) * NS2S;
                final float[] linearAcceleration = new float[3];
                System.arraycopy(event.values, 0, linearAcceleration, 0, 3);

                // remove gravity (low-pass filter)
                final float alpha = 0.8f;
                float gravity[] = new float[3];
                gravity[0] = alpha * gravity[0] + (1 - alpha) * linearAcceleration[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * linearAcceleration[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * linearAcceleration[2];
                linearAcceleration[0] = linearAcceleration[0] - gravity[0];
                linearAcceleration[1] = linearAcceleration[1] - gravity[1];
                linearAcceleration[2] = linearAcceleration[2] - gravity[2];

                velocity[0] = 0.0f;
                velocity[1] = 0.0f;
                velocity[2] = 0.0f;

                velocity[0] += linearAcceleration[0] * dt;
                velocity[1] += linearAcceleration[1] * dt;
                velocity[2] += linearAcceleration[2] * dt;

                float speed = (float) Math.sqrt(velocity[0] * velocity[0] + velocity[1] * velocity[1] + velocity[2] * velocity[2]);

                lastTimestamp = event.timestamp;
                speedTextView.setText("Current Speed: " + String.format("%.2f", speed) + " m/s");
            } else {
                lastTimestamp = event.timestamp;
            }
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
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @SuppressLint("SetTextI18n")
    private void detectSteps(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2];

            // Add current acceleration to buffer
            accelerationBuffer.add(z);

            // Maintain buffer size
            if (accelerationBuffer.size() > WINDOW_SIZE) {
                accelerationBuffer.removeFirst();
            }

            // Calculate average acceleration over the window
            float avgAcceleration = 0.0f;
            for (float acc : accelerationBuffer) {
                avgAcceleration += acc;
            }
            avgAcceleration /= accelerationBuffer.size();

            // Check for step using thresholding
            if (z > 0 && avgAcceleration > 0 && prevAcceleration <= 0) {
                currentSteps++;
            }

            prevAcceleration = avgAcceleration;
            stepsViewText.setText("Steps: " + currentSteps);
        }
    }
}