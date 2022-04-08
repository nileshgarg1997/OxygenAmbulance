package com.app.totoambulance;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.HashMap;
import java.util.List;

public class PatientMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    boolean isPermissionGranter;
    GoogleMap googleMap;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseAuth fAuth;
    String userID;
    LatLng userCurrentLocation;
    FirebaseDatabase firebaseDatabase;
    FirebaseUser currentUser;
    DatabaseReference userDatabaseRef;
    DatabaseReference driverAvailableRef;
    DatabaseReference driverLocationRef;
    DatabaseReference driverRef;
    Marker driverMarker,pickupMarker;
    Button bookBtn;
    TextView textView;
    private Boolean driverFound = false;
    String driverFoundId;
    int radius = 1;
    private boolean currentLogoutUserStatus = false, requestType = false;
    ValueEventListener DriverLocationRefListener;
    GeoQuery geoQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_map);

        bookBtn = findViewById(R.id.bookBtn);
        textView=findViewById(R.id.textview_id);
        fAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = fAuth.getCurrentUser();
        userID = currentUser.getUid();
        userDatabaseRef = firebaseDatabase.getReference().child("Customer's Requests");
        driverAvailableRef = firebaseDatabase.getReference().child("Drivers Available");
        driverLocationRef = firebaseDatabase.getReference().child("Drivers Working");


        checkPermission();
        if (isPermissionGranter) {
            if (checkGoogleServices()) {
                SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
                supportMapFragment.getMapAsync(this);
                if (isPermissionGranter) {
                    checkGps();
                }
            } else {
                Toast.makeText(this, "Google Play Services not Available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkPermission() {

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        isPermissionGranter = true;
                        Toast.makeText(PatientMapActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), "");
                        intent.setData(uri);
                        startActivity(intent);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    private boolean checkGoogleServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (result == ConnectionResult.SUCCESS) {
            return true;
        } else if (googleApiAvailability.isUserResolvableError(result)) {
            Dialog dialog = googleApiAvailability.getErrorDialog(this, result, 201, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    Toast.makeText(PatientMapActivity.this, "User Cancelled Dialog", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        }
        return false;
    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PatientMapActivity.this);
        googleMap = map;
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//                        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You are here...!!");
//
//                        googleMap.addMarker(markerOptions);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                        Toast.makeText(Trial.this, "Location: "+location.getLatitude()+": "+location.getLongitude(), Toast.LENGTH_SHORT).show();

                    }
                });


    }

    private void checkGps() {

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest).setAlwaysShow(true);

        Task<LocationSettingsResponse> locationSettingsResponseTask = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        locationSettingsResponseTask.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    getCurrentLocationUpdate();

//                    Toast.makeText(Trial.this, "Gps is already enable", Toast.LENGTH_SHORT).show();
                } catch (ApiException e) {
                    if (e.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        try {
                            resolvableApiException.startResolutionForResult(PatientMapActivity.this, 101);
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            sendIntentException.printStackTrace();
                        }
                    }
                    if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        Toast.makeText(PatientMapActivity.this, "Settings not Available", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }


    private void getCurrentLocationUpdate() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PatientMapActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                LatLng latLng = new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
//                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You are here...!!");
//
//                googleMap.addMarker(markerOptions);
//                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                Toast.makeText(Trial.this, "Location: "+locationResult.getLastLocation().getLatitude()+": "+locationResult.getLastLocation().getLongitude(), Toast.LENGTH_SHORT).show();


            }
        }, Looper.getMainLooper());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Now Gps is enabled", Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Denied Gps enable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map_menu_buttons, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingsMenuBtn:

                break;
            case R.id.logoutMenuBtn:

                fAuth.signOut();
                Intent i = new Intent(PatientMapActivity.this, UserLoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                Toast.makeText(this, "Successfully Logout!!!", Toast.LENGTH_SHORT).show();
                finish();

                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void bookButton(View view) {

        bookBtn.setVisibility(View.GONE);
        textView.setVisibility(View.VISIBLE);

        if (requestType) {

            requestType = false;
            geoQuery.removeAllListeners();
            driverLocationRef.removeEventListener(DriverLocationRefListener);

            if (driverFound!=null) {
                driverRef=FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                driverRef.setValue(true);
                driverFoundId=null;
            }
            driverFound=false;
            radius=1;

            GeoFire geoFire = new GeoFire(userDatabaseRef);
            geoFire.removeLocation(userID);

            if (pickupMarker!=null){
                pickupMarker.remove();
            }

            textView.setText("Book TOTO Ambulance");

        } else {

            requestType = true;
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PatientMapActivity.this);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            GeoFire geoFire = new GeoFire(userDatabaseRef);
                            geoFire.setLocation(userID, new GeoLocation(location.getLatitude(), location.getLongitude()));

                            userCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.addMarker(new MarkerOptions().position(userCurrentLocation).title("Patient's Point"));

                        }
                    });
            textView.setText("Getting TOTO Driver...");
            getClosestDriverCab();
        }
    }

    private void getClosestDriverCab() {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(PatientMapActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        GeoFire geoFire = new GeoFire(driverAvailableRef);
                        userCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(userCurrentLocation.latitude, userCurrentLocation.longitude), radius);

                        geoQuery.removeAllListeners();

                        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                            @Override
                            public void onKeyEntered(String key, GeoLocation location) {

                                if (!driverFound && requestType) {
                                    driverFound = true;
                                    driverFoundId = key;

                                    driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
                                    HashMap driverMap = new HashMap();
                                    driverMap.put("CustomerRideID", userID);
                                    driverRef.updateChildren(driverMap);

                                    getDriverLocation();
                                    textView.setText("Looking for Driver Location...");
                                }
                            }

                            @Override
                            public void onKeyExited(String key) {

                            }

                            @Override
                            public void onKeyMoved(String key, GeoLocation location) {

                            }

                            @Override
                            public void onGeoQueryReady() {

                                if (!driverFound) {
                                    radius = radius + 1;
                                    getClosestDriverCab();
                                }
                            }

                            @Override
                            public void onGeoQueryError(DatabaseError error) {

                            }
                        });

                    }
                });


    }

    //and then we get to the driver location - to tell customer where is the driver
    private void getDriverLocation() {

        DriverLocationRefListener = driverLocationRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && requestType) {
                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            textView.setText("Driver Found");

                            if (driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if (driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }

                            LatLng driverLatlng = new LatLng(LocationLat, LocationLng);
                            if (driverMarker != null) {
                                driverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(userCurrentLocation.latitude);
                            location1.setLongitude(userCurrentLocation.longitude);


                            Location location2 = new Location("");
                            location2.setLatitude(driverLatlng.latitude);
                            location2.setLongitude(driverLatlng.longitude);

                            float distance = location1.distanceTo(location2);

                            if (distance<90){
                                textView.setText("TOTO Reached");
                            }else {
                                
                                textView.setText("Distance in meters: " + String.valueOf(distance));
                            }
                            driverMarker = googleMap.addMarker(new MarkerOptions().position(driverLatlng).title("Your driver is here"));
                            Toast.makeText(PatientMapActivity.this, "Booking Confirmed!! TOTO is on the way..", Toast.LENGTH_LONG).show();
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLatlng,15));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}