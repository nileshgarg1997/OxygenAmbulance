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
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
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

import java.util.List;

public class DriverMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    boolean isPermissionGranter;
    GoogleMap googleMap;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseAuth fAuth;
    FirebaseDatabase firebaseDatabase;
    FirebaseUser currentUser;
    LatLng driverCurrentLocation;
    DatabaseReference assignedCustomerRef, assignedCustomerPickupRef;
    String driverID, customerID = "",userID;
    Marker pickupMarker;
    private boolean currentLogoutDriverStatus = false;
    ValueEventListener assignedCustomerPickupRefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        fAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        currentUser = fAuth.getCurrentUser();
        driverID = currentUser.getUid();
        userID = currentUser.getUid();



        checkPermission();
        if (isPermissionGranter) {
            if (checkGoogleServices()) {
                SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
                supportMapFragment.getMapAsync(this);
                if (isPermissionGranter) {
//                    checkGps();
                }
            } else {
                Toast.makeText(this, "Google Play Services not Available", Toast.LENGTH_SHORT).show();
            }
        }

        getAssignedCustomerRequest();
    }

    private void getAssignedCustomerRequest() {

        assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers")
                .child(driverID).child("CustomerRideID");

        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    customerID = snapshot.getValue().toString();

                    getAssignedCustomerPickupLocation();
                } else {
                    customerID = "";
                    if (pickupMarker != null) {
                        pickupMarker.remove();
                    }
                    if (assignedCustomerPickupRefListener != null) {
                        assignedCustomerPickupRef.removeEventListener(assignedCustomerPickupRefListener);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void getAssignedCustomerPickupLocation() {

        assignedCustomerPickupRef = FirebaseDatabase.getInstance().getReference().child("Customer's Requests")
                .child(customerID).child("l");

        assignedCustomerPickupRefListener = assignedCustomerPickupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) snapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;

                    if (customerLocationMap.get(0) != null) {
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(1) != null) {
                        LocationLng = Double.parseDouble(customerLocationMap.get(1).toString());
                    }

                    LatLng driverLatlng = new LatLng(LocationLat, LocationLng);

                    pickupMarker=googleMap.addMarker(new MarkerOptions().position(driverLatlng).title("Patient Location"));
                    Toast.makeText(DriverMapActivity.this, "Booking Received!! Click on the Marker", Toast.LENGTH_LONG).show();
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLatlng, 15));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void requestCompletedButton(View view) {



        DatabaseReference removeCustomerRef = firebaseDatabase.getReference().child("Users").child("Drivers").child(driverID).child(customerID);

        GeoFire geoFire = new GeoFire(removeCustomerRef);
        geoFire.removeLocation(userID);
        customerID="";
        pickupMarker.remove();
        Toast.makeText(this, "Request Completed Successfully!!!", Toast.LENGTH_SHORT).show();



//        fAuth.signOut();
//        Intent i = new Intent(DriverMapActivity.this, DriverLoginActivity.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(i);
//        Toast.makeText(this, "Login Again for new Request!!!", Toast.LENGTH_LONG).show();
//        finish();


    }

    private void checkPermission() {

        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        isPermissionGranter = true;
                        checkGps();
                        Toast.makeText(DriverMapActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(DriverMapActivity.this, "User Cancelled Dialog", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        }
        return false;
    }


    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(DriverMapActivity.this);
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
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You are here...!!");

                            googleMap.addMarker(markerOptions);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                        Toast.makeText(Trial.this, "Location: "+location.getLatitude()+": "+location.getLongitude(), Toast.LENGTH_SHORT).show();
                        }
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
                            resolvableApiException.startResolutionForResult(DriverMapActivity.this, 101);
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            sendIntentException.printStackTrace();
                        }
                    }
                    if (e.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                        Toast.makeText(DriverMapActivity.this, "Settings not Available", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }


    private void getCurrentLocationUpdate() {

        if (getApplicationContext() != null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(DriverMapActivity.this);
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
                    driverCurrentLocation = latLng;
//                googleMap.addMarker(markerOptions);
//                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                Toast.makeText(Trial.this, "Location: "+locationResult.getLastLocation().getLatitude()+": "+locationResult.getLastLocation().getLongitude(), Toast.LENGTH_SHORT).show();

                    if (!currentLogoutDriverStatus) {

                        DatabaseReference DriverAvailabilityRef = firebaseDatabase.getReference().child("Drivers Available");
                        GeoFire geoFireAvailability = new GeoFire(DriverAvailabilityRef);

                        DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
                        GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

                        switch (customerID) {

                            case "":
                                geoFireWorking.removeLocation(userID);
                                geoFireAvailability.setLocation(userID, new GeoLocation(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
                                break;
                            default:
                                geoFireAvailability.removeLocation(userID);
                                geoFireWorking.setLocation(userID, new GeoLocation(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
//                                Toast.makeText(DriverMapActivity.this, "Booking Request Received", Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                }
            }, Looper.getMainLooper());
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Now Gps is enabled!! Please Login Again", Toast.LENGTH_LONG).show();
                Intent i = new Intent(getApplicationContext(), DriverLoginActivity.class);
                startActivity(i);
            }
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Denied Gps enable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!currentLogoutDriverStatus) {
            disconnectDriver();
        }
    }

    private void disconnectDriver() {
        String userID = currentUser.getUid();
        DatabaseReference DriverAvailabilityRef = firebaseDatabase.getReference().child("Drivers Available");

        GeoFire geoFire = new GeoFire(DriverAvailabilityRef);
        geoFire.removeLocation(userID);
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

                currentLogoutDriverStatus = true;
                disconnectDriver();
                fAuth.signOut();
                Intent i = new Intent(DriverMapActivity.this, DriverLoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                Toast.makeText(this, "Successfully Logout!!!", Toast.LENGTH_SHORT).show();
                finish();

                break;
        }
        return super.onOptionsItemSelected(item);
    }


}