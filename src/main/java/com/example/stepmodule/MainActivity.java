package com.example.stepmodule;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Time;
import java.util.ArrayList;

import java.net.Inet4Address;

public class MainActivity extends Activity implements SensorEventListener {
    private SensorManager sensorManager;
    private boolean color = false;
    private View view;
    TextView textDir, textSteps;

    private FusedLocationProviderClient fusedLocationClient;

    private LocationRequest locationRequest;

    private LocationCallback locationCallback;

    private Location lastLocation = null;

    private float disp_distance;

    private int stepCount = 0;

    private float step_average;

    private Socket socket;

    private ArrayList<Float> step_length_array;

    private String[] tensStrings;
    private String[] teenStrings;
    private String[] onesStrings;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        textDir = (TextView) findViewById(R.id.direction);
        textSteps = (TextView) findViewById(R.id.steps);

        step_length_array = new ArrayList<Float>();
        step_average = 0;


        view = findViewById(R.id.textView);
        view.setBackgroundColor(Color.BLUE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(100);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            public void onLocationResult(final LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (final Location location : locationResult.getLocations()) {
                    float distance = 0.0f;
                    if (lastLocation != null) {
                        distance = location.distanceTo(lastLocation);
                    }
                    final float disp_distance = distance;
                    lastLocation = location;
                    final float step_length_temp;
                    if (stepCount > 4) {
                        step_length_temp = disp_distance / stepCount;
                        final float step_length = step_length_temp;
                        updateAverage(step_length_temp);
                        stepCount = 0;
                        textDir.post(new Runnable() {
                            @Override
                            public void run() {
                                textDir.setText(Float.toString(step_length));
                            }
                        });
                    }
              }
            };
        };

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startLocationUpdates();

        Thread networkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.w("THREAD", "IN NEW THREAD");
                try {
                    initSocket("10.141.143.253");
                    float[] sine = makesine();
                    for (float num : sine) {
                        writeToSocket(num);
                        Log.w("NETWORK THREAD", "WRITING TO SOCKET");
                        Thread.sleep(10);
                    }
                } catch (IOException e) {
                    Log.w("ONCREATE", e.getMessage());
                } catch (InterruptedException e) {

                }
            };
        });
        networkThread.start();

        tensStrings = new String[] {null, null, "twenty", "thirty", "fifty", "sixty", "seventy", "eighty", "ninety"};
        teenStrings = new String[] {"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                                    "eighteen", "nineteen"};
        onesStrings = new String[] {null, "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
    }

    private void updateAverage(float length) {
        step_length_array.add(length);
        float sum = 0;
        for (float entry : step_length_array) sum += entry;
        step_average = sum / step_length_array.size();
        Log.w("STEP LENGTH", "STEP LENGTH: " + length + "STEPS: " + stepCount + "AVERAGE: " + step_average);
    }

//    private void getDisplacement() {
//        if (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION )
//                == PackageManager.PERMISSION_GRANTED) {
//            fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
//                @Override
//                public void onSuccess(Location location) {
//                    if (location != null) {
//                        Log.w("LocationRequest", "LOCATION EVENT" + Float.toString(SystemClock.elapsedRealtime()));
//                        float distance = 0.0f;
//                        if (lastLocation != null) {
//                            distance = location.distanceTo(lastLocation);
//                            Log.w("LocationRequest", "LOCATION EVENT" + Float.toString(distance));
//                        }
//                        disp_distance = distance;
//                        lastLocation = location;
//                        textDir.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                textDir.setText(Float.toString(disp_distance));
//                            }
//                        });
//                    }
//                }
//            });
//        }
//    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            //if (++stepCount % 10 == 0) getDisplacement();
            ++stepCount;
            displaySteps(event);
            //checkShake(event);
        }
    }

    private void displaySteps(SensorEvent event) {
        textSteps.post(new Runnable() {
            @Override
            public void run() {
                textSteps.setText(Integer.toString(stepCount));
            }
        });

    }

    private void startLocationUpdates() {
        if ( ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION}, 6);
        }

        if (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    null /* Looper */);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void initSocket(String ip) throws IOException {
        socket = new Socket(ip, 3000, null, 0);
        if (socket.isConnected()) {
            Log.w("SOCKET", "SUCCESSFULLY CONNECTED");
        }
        else {
            Log.w("SOCKET", "NOT CONNECTED");
        }
    }

    public void writeToSocket(float val) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataOutputStream.writeFloat(val);
    }

    public float[] makesine() {
        int cycles = 5;
        int points = 10 * cycles * 2;
        float[] sines = new float[points];
        for (int i = 0; i < points; i++) {
            double radians = ( Math.PI / 10) * i;
            sines[i] = (float) Math.sin(radians);
        }
        return sines;
    }

    public String sendOutputText(float upcomingDist, String command) {
        if (upcomingDist >= 30f) Log.w("OUTPUT STRING", "INCOMING FLOAT TOO BIG");
        float stepsToTarget = upcomingDist / step_average;
        if (stepsToTarget < 1) stepsToTarget = 1;
        int tens = (int) stepsToTarget / 10;
        int ones = (int) stepsToTarget % 10;
        String tensPlace = null;
        String onesPlace = null;
        if (tens == 1) {
            onesPlace = teenStrings[ones];
        }
        else if (tens > 1) {
            tensPlace = tensStrings[tens];
        }
        onesPlace = onesStrings[ones];

        String output = "In ";
        if (!tensPlace.equals(null)) {
            output += tensPlace + " ";
        }
        if (!onesPlace.equals(null)) {
            output += onesPlace + " ";
        }

        output += "steps";

        if (command.equals(null)) {
            output = "Object " + output;
        }
        else {
            output += ", " + command;
        }

        return output;
    }
}