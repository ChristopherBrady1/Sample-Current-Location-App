package com.example.getcurrentlocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    //initialize variable
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    boolean isMarkerRotating = false;
    SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;

    private float[] valuesAccelerometer;
    private float[] valuesMagneticField;

    private float[] matrixR;
    private float[] matrixI;
    private float[] matrixValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        valuesAccelerometer = new float[3];
        valuesMagneticField = new float[3];

        matrixR = new float[9];
        matrixI = new float[9];
        matrixValues = new float[3];



        //Assign variable
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        //init fuse location
        client = LocationServices.getFusedLocationProviderClient(this);

        //check permission
        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //when permission granted
            //call method
            getCurrentLocation(0);
        } else {
            //when permission denied
            //request permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    public Bitmap resizeMapIcons(String iconName,int width, int height){
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(),getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    private void getCurrentLocation(float azimuth) {
        //initialize task location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                //When success
                if (location != null){
                    //sync map
                    supportMapFragment.getMapAsync((new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            //Initialize lat lng
                            googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

                            //Bitmap marker_icon = BitmapFactory.decodeResource(getResources(),R.drawable.gps_arrow);


                            LatLng latLng = new LatLng(location.getLatitude()
                                ,location.getLongitude());
                            //create marker options
                            //Icons made by <a href="https://www.flaticon.com/authors/freepik" title="Freepik">Freepik</a> from <a href="https://www.flaticon.com/" title="Flaticon"> www.flaticon.com</a>

                            Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons("arrow",100,100))));
                            //changed icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                            //MarkerOptions options = new MarkerOptions().position(latLng)
                            //        .title("I am here");
                            //Zoom map
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                            //location.bearingTo()

                            LatLng newLocaation;
                            LatLng oldLocation;


                            rotateMarker(marker, azimuth);

                            //add marker on map
                           // googleMap.addMarker(options);
                        }
                    }));

                }
            }
        });

    }

    private double bearingBetweenLocations(LatLng latLng1,LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    private void rotateMarker(final Marker marker, final float toRotation) {
        if(!isMarkerRotating) {
            final Handler handler = new Handler();
            final long start = SystemClock.uptimeMillis();
            final float startRotation = marker.getRotation();
            final long duration = 2000;

            final Interpolator interpolator = new LinearInterpolator();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    isMarkerRotating = true;

                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);

                    float rot = t * toRotation + (1 - t) * startRotation;

                    float bearing =  -rot > 180 ? rot / 2 : rot;

                    marker.setRotation(bearing);

                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16);
                    } else {
                        isMarkerRotating = false;
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 44 ){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //when granted
                //call method
                getCurrentLocation(0);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        // It is good practice to check that we received the proper sensor event
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            // Convert the rotation-vector to a 4x4 matrix.
            SensorManager.getRotationMatrixFromVector(matrixR, event.values);
            SensorManager.remapCoordinateSystem(matrixR,
                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
                    matrixR);
            SensorManager.getOrientation(matrixR, matrixValues);

            // Optionally convert the result from radians to degrees
            matrixValues[0] = (float) Math.toDegrees(matrixValues[0]);
            matrixValues[1] = (float) Math.toDegrees(matrixValues[1]);
            matrixValues[2] = (float) Math.toDegrees(matrixValues[2]);

            /*tv.setText(" Yaw: " + matrixValues[0] + "\n Pitch: "
                        + matrixValues[1] + "\n Roll (not used): "
                    + matrixValues[2]);*/

            getCurrentLocation(matrixValues[0]);



        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}