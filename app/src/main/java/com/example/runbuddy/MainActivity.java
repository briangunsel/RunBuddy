package com.example.runbuddy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.runbuddy.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int ACCELEROMETER_PERMISSION_REQUEST_CODE = 1002;
    private ActivityMainBinding binding;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] acceleration = new float[3];
    private float currentSpeed = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.startRunButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.startRunButton.setVisibility(View.GONE);
                binding.pauseRunButton.setVisibility(View.VISIBLE);
                binding.endRunButton.setVisibility(View.VISIBLE);

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

                onResume();
            }
        });
    }

    private void startAccelerometer() {
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onResume() {
        Toast.makeText(MainActivity.this, "Run Resumed", Toast.LENGTH_SHORT).show();

        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        Toast.makeText(MainActivity.this, "Run Paused", Toast.LENGTH_SHORT).show();

        super.onPause();
        if (accelerometer != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acceleration = event.values;
            updateSpeed();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // ...
    }

    private void updateSpeed() {
        float totalAcceleration = (float) Math.sqrt(acceleration[0] * acceleration[0] + acceleration[1] * acceleration[1] + acceleration[2] * acceleration[2]);
        currentSpeed += (float) (totalAcceleration * 0.1);
        Toast.makeText(MainActivity.this, "Current Speed (may be inaccurate): " + currentSpeed + " m/s", Toast.LENGTH_SHORT).show();
    }

    private void startNewRun() {
        Toast.makeText(this, "New run started.", Toast.LENGTH_SHORT).show();

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
        Toast.makeText(MainActivity.this, "Run stopped.", Toast.LENGTH_SHORT).show();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        currentSpeed = 0.0f;
    }
}
