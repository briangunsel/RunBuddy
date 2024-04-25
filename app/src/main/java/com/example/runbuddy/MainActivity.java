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
import android.widget.ListView;
import android.widget.ArrayAdapter;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.LinkedList;

import com.example.runbuddy.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.example.runbuddy.R;

import java.util.EventListener;
import java.util.List;

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
    private static final float STEP_THRESHOLD = 9.8f;
    private float[] lastAcceleration = new float[3];
    private Map<Integer, float[]> runs = new HashMap<>();
    private int runIndex = 0;
    private float speedSum = 0;
    private long numSpeeds = -5;
    private long stepIndex = 0;
    private float avgSpeed;
    private float runDistance = 0;
    private NavController navController;
    TextView speedTextView;
    TextView stepsViewText;
    TextView distanceViewText;
    TextView accelerationViewText;
    ArrayAdapter<String> adapter;
    ListView listView;
    List<String> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Runs
        listView = findViewById(R.id.listView);

        dataList = new ArrayList<>();

        for (Map.Entry<Integer, float[]> entry : runs.entrySet()) {
            int key = entry.getKey();
            float[] value = entry.getValue();
            String data = "Key: " + key + ", Value: " + value[0] + ", " + value[1];
            dataList.add(data);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, dataList);

        listView.setAdapter(adapter);


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

    @SuppressLint("SetTextI18n")
    private void updateSpeed(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastTimestamp;
            lastTimestamp = currentTime;

            // copy the accelerometer values to acceleration array
            System.arraycopy(event.values, 0, acceleration, 0, 3);

            double magnitude = Math.sqrt(Math.pow(acceleration[0], 2) + Math.pow(acceleration[1], 2) + Math.pow(acceleration[2], 2));
            double speed = magnitude * elapsedTime / 100;

            speed = Math.abs(speed - 2.5);

            speed = Math.round(speed * 10.0) / 10.0;
            speedTextView.setText("Speed: " + speed + " m/s");

            //Update speed sum
            if (numSpeeds >= 0) {
                speedSum += (float) speed;
            }
            numSpeeds++;
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

        // Add run to map
        avgSpeed = (float)(speedSum/numSpeeds);
        //runs.put(runIndex, new float[]{avgSpeed, (float)stepCount});
        runIndex++;

        //Reset speed sum
        speedSum = 0;
        numSpeeds = -5;

        //Update runs list
        @SuppressLint("DefaultLocale") String roundedSpeed = String.format("%.2f", avgSpeed);
        @SuppressLint("DefaultLocale") String roundedDistance = String.format("%.2f", runDistance);
        String data = "Run: " + runIndex + " Average Speed: " + roundedSpeed + " m/s Step Count: " + stepCount + " Distance: " + roundedDistance;
        dataList.add(data);
        adapter.notifyDataSetChanged();
        listView.setAdapter(adapter);

        stepCount = 0;
    }

    // detect steps using acceleration vector from accelerometer.
    @SuppressLint("SetTextI18n")
    private void detectSteps(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float z = event.values[2];

            if (Math.abs(z) > STEP_THRESHOLD) {
                if (z > 0) {
                    if (stepIndex > 7) {
                        stepCount++;
                        stepIndex = 0;
                    }
                    stepIndex++;
                }
            }

            stepsViewText.setText("Steps: " + stepCount);
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateDistance(SensorEvent event) {
        if(runActive) {
            float distance = stepCount * AVERAGE_STRIDE_LENGTH;
            runDistance = distance;

            distanceViewText.setText("Distance: " + distance + "m");
        }
    }

    @SuppressLint("SetTextI18n")
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
            totalLinearAcceleration = (float) (Math.round(totalLinearAcceleration * 10.0) / 10.0);

            accelerationViewText.setText("Acceleration: " + totalLinearAcceleration + " m/s^2");
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