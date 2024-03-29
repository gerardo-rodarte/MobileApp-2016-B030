package ipn.mobileapp.model.service;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import ipn.mobileapp.R;

public class GpsService extends Service implements LocationListener {

    private final Context context;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

    boolean canGetLocation = false;

    Location location;
    double latitude;
    double longitude;

    protected LocationManager locationManager;

    public GpsService(Context context) {
        this.context = context;

        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            getLocation();
    }

    public Location getLocation() {
        try {
            List<String> providers = locationManager.getProviders(true);
            location = null;
            if (providers != null) {
                this.canGetLocation = true;
                for (String provider : providers) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Location temp = locationManager.getLastKnownLocation(provider);
                    if (temp == null) {
                        continue;
                    }
                    if (location == null || temp.getAccuracy() < location.getAccuracy()) {
                        location = temp;
                    }
                }
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    public ipn.mobileapp.model.pojo.Location getAddress() {
        List<Address> addresses = null;
        Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ipn.mobileapp.model.pojo.Location location = new ipn.mobileapp.model.pojo.Location();
        try {
            if (addresses != null) {
                String postalCode = addresses.get(0).getPostalCode();
                location.setPostalCode(postalCode);
            }
        } catch (Exception e) {
            location = null;
        }

        return location;
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        return longitude;
    }

    public boolean validLocation() {
        return location != null;
    }


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
