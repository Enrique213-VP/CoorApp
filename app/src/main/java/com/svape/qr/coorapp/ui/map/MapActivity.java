package com.svape.qr.coorapp.ui.map;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.ImageHolder;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.svape.qr.coorapp.databinding.ActivityMapBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements LocationListener {
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_ETIQUETA = "extra_etiqueta";

    private static final int PERMISSION_REQUEST_LOCATION = 200;
    private static final String TAG = "MapActivity";

    private ActivityMapBinding binding;
    private LocationManager locationManager;
    private PointAnnotationManager pointAnnotationManager;
    private LocationComponentPlugin locationComponentPlugin;

    private double targetLatitude;
    private double targetLongitude;
    private String etiqueta;
    private Point currentUserLocation;

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
        currentUserLocation = point;
        if (targetLatitude != 0 && targetLongitude != 0) {
            fitCameraToBounds();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        targetLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
        etiqueta = getIntent().getStringExtra(EXTRA_ETIQUETA);
        binding.destinationLabel.setText("Destino: " + etiqueta);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        binding.backButton.setOnClickListener(v -> finish());

        initializeMap();
    }

    private void initializeMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS, style -> {
            initAnnotations();
            showTargetLocation();

            if (hasLocationPermission()) {
                setupLocationComponent();
                getUserLocation();
            } else {
                requestLocationPermission();
            }
        });
    }

    private void initAnnotations() {
        try {
            // Get the annotation plugin
            AnnotationPlugin annotationPlugin = binding.mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);

            // Create the point annotation manager using AnnotationType enum
            pointAnnotationManager = (PointAnnotationManager) annotationPlugin.createAnnotationManager(
                    AnnotationType.PointAnnotation,
                    new AnnotationConfig()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar PointAnnotationManager", e);
        }
    }

    private void showTargetLocation() {
        Point targetPoint = Point.fromLngLat(targetLongitude, targetLatitude);
        binding.mapView.getMapboxMap().setCamera(new CameraOptions.Builder()
                .center(targetPoint)
                .zoom(15.0)
                .build());

        addAnnotationAtPoint(targetLatitude, targetLongitude, etiqueta);
    }

    private void addAnnotationAtPoint(double latitude, double longitude, String title) {
        try {
            if (pointAnnotationManager != null) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_mylocation);

                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(longitude, latitude))
                        .withIconImage(icon)
                        .withTextField(title)
                        .withTextColor(Color.WHITE)
                        .withTextSize(14.0)
                        .withTextOffset(Arrays.asList(0.0, 2.0));

                pointAnnotationManager.create(options);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al añadir el marcador", e);
        }
    }

    private void setupLocationComponent() {
        try {
            locationComponentPlugin = binding.mapView.getPlugin(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID);
            if (locationComponentPlugin != null) {
                locationComponentPlugin.setEnabled(true);

                Bitmap bearingImage = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_compass);
                if (bearingImage != null) {
                    locationComponentPlugin.setLocationPuck(new LocationPuck2D(
                            ImageHolder.from(bearingImage),
                            null,
                            null
                    ));
                }

                locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al configurar el componente de ubicación", e);
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                10,
                this
        );

        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation != null) {
            updateUserLocation(lastKnownLocation);
        } else {
            simulateUserLocation();
        }
    }

    private void updateUserLocation(Location location) {
        currentUserLocation = Point.fromLngLat(location.getLongitude(), location.getLatitude());
        addUserMarker();
        fitCameraToBounds();
    }

    private void addUserMarker() {
        if (currentUserLocation != null && pointAnnotationManager != null) {
            try {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_compass);

                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(currentUserLocation)
                        .withIconImage(icon)
                        .withTextField("Mi ubicación")
                        .withTextColor(Color.WHITE)
                        .withTextSize(14.0)
                        .withTextOffset(Arrays.asList(0.0, 2.0));

                pointAnnotationManager.create(options);
            } catch (Exception e) {
                Log.e(TAG, "Error al añadir el marcador del usuario", e);
            }
        }
    }

    private void simulateUserLocation() {
        double offsetLat = 0.01;
        double offsetLng = 0.01;
        currentUserLocation = Point.fromLngLat(targetLongitude + offsetLng, targetLatitude + offsetLat);
        addUserMarker();
        fitCameraToBounds();
        Toast.makeText(this, "Usando ubicación simulada para demostración", Toast.LENGTH_SHORT).show();
    }

    private void fitCameraToBounds() {
        if (currentUserLocation != null) {
            List<Point> points = new ArrayList<>();
            points.add(currentUserLocation);
            points.add(Point.fromLngLat(targetLongitude, targetLatitude));

            EdgeInsets edgeInsets = new EdgeInsets(50, 50, 50, 50);
            binding.mapView.getMapboxMap().cameraForCoordinates(points, edgeInsets, null, null);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_REQUEST_LOCATION);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        updateUserLocation(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationComponent();
                getUserLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                simulateUserLocation();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationComponentPlugin != null) {
            locationComponentPlugin.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(this);
        }
    }
}