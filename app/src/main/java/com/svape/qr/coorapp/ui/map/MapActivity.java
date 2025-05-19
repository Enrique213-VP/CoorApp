package com.svape.qr.coorapp.ui.map;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.EdgeInsets;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.Plugin;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.AnnotationType;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;
import com.svape.qr.coorapp.R;
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
    private PolylineAnnotationManager polylineAnnotationManager;

    private double targetLatitude;
    private double targetLongitude;
    private String etiqueta;
    private Point currentUserLocation;
    private boolean routeShown = false;
    private MediaPlayer mediaPlayer;
    private int clickCount = 0;
    private static final int MAX_SOUND_CLICKS = 3;

    private static final String[] MAP_STYLES = {
            Style.MAPBOX_STREETS,
            Style.SATELLITE_STREETS,
            Style.OUTDOORS,
            Style.LIGHT,
            Style.DARK
    };
    private int currentStyleIndex = 0;

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = point -> {
        currentUserLocation = point;
        if (routeShown) {
            drawRouteLine();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clickCount = 0;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        binding = ActivityMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hideSystemUI();

        targetLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, 0);
        targetLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, 0);
        etiqueta = getIntent().getStringExtra(EXTRA_ETIQUETA);
        binding.destinationLabel.setText("Destino: " + etiqueta);

        Log.d(TAG, "Recibiendo coordenadas: Latitud=" + targetLatitude + ", Longitud=" + targetLongitude);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        setupFloatingActionButtons();

        initializeMap();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();

        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN;

        flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        decorView.setSystemUiVisibility(flags);
    }


    private void setupFloatingActionButtons() {
        binding.fabBack.setOnClickListener(v -> finish());

        binding.fabRoute.setOnClickListener(v -> {
            if (clickCount < MAX_SOUND_CLICKS) {
                playRouteSound();
                clickCount++;
            }

            if (currentUserLocation != null) {
                routeShown = true;
                if (polylineAnnotationManager != null) {
                    polylineAnnotationManager.deleteAll();
                }
                drawRouteLine();
                Snackbar.make(binding.getRoot(), "Ruta a destino mostrada", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(binding.getRoot(), "Esperando ubicación actual...", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.fabMapStyle.setOnClickListener(v -> {
            changeMapStyle();
        });
    }



    private void changeMapStyle() {
        currentStyleIndex = (currentStyleIndex + 1) % MAP_STYLES.length;
        String newStyle = MAP_STYLES[currentStyleIndex];

        binding.mapView.getMapboxMap().loadStyleUri(newStyle, style -> {
            if (hasLocationPermission()) {
                setupLocationComponent();
            }

            if (routeShown && currentUserLocation != null) {
                drawRouteLine();
            }

            if (targetLatitude != 0 && targetLongitude != 0) {
                addAnnotationAtPoint(targetLatitude, targetLongitude, etiqueta);
            }

            if (currentUserLocation != null) {
                addUserMarker();
            }
        });

        String styleName = "Estilo: ";
        switch (newStyle) {
            case Style.MAPBOX_STREETS:
                styleName += "Calles";
                break;
            case Style.SATELLITE_STREETS:
                styleName += "Satélite";
                break;
            case Style.OUTDOORS:
                styleName += "Exterior";
                break;
            case Style.LIGHT:
                styleName += "Claro";
                break;
            case Style.DARK:
                styleName += "Oscuro";
                break;
        }
        Snackbar.make(binding.getRoot(), styleName, Snackbar.LENGTH_SHORT).show();
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
            AnnotationPlugin annotationPlugin = binding.mapView.getPlugin(Plugin.MAPBOX_ANNOTATION_PLUGIN_ID);

            pointAnnotationManager = (PointAnnotationManager) annotationPlugin.createAnnotationManager(
                    AnnotationType.PointAnnotation,
                    new AnnotationConfig()
            );

            polylineAnnotationManager = (PolylineAnnotationManager) annotationPlugin.createAnnotationManager(
                    AnnotationType.PolylineAnnotation,
                    new AnnotationConfig()
            );

        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar AnnotationManagers", e);
        }
    }

    private void playRouteSound() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.splash_sound);

        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
        });

        mediaPlayer.start();
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
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.icon);

                if (icon == null) {
                    icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_dialog_map);
                }

               Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, 48, 48, true);

                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(longitude, latitude))
                        .withIconImage(scaledIcon)
                        .withTextField(title)
                        .withTextColor(Color.WHITE);

                try {
                    options.withTextSize(14.0);
                    options.withTextOffset(Arrays.asList(0.0, 2.0));
                } catch (NoSuchMethodError e) {
                    Log.d(TAG, "withTextSize no disponible en esta versión");
                }

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

                try {
                    LocationPuck2D locationPuck2D = new LocationPuck2D();
                    locationComponentPlugin.setLocationPuck(locationPuck2D);
                } catch (NoSuchMethodError e) {
                    Log.d(TAG, "setLocationPuck no disponible en esta versión");
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
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user);

                if (icon == null) {
                    icon = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_compass);
                }

                Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, 40, 40, true);

                PointAnnotationOptions options = new PointAnnotationOptions()
                        .withPoint(currentUserLocation)
                        .withIconImage(scaledIcon)
                        .withTextField("Mi ubicación")
                        .withTextColor(Color.BLUE);

                try {
                    options.withTextSize(14.0);
                    options.withTextOffset(Arrays.asList(0.0, 2.0));
                } catch (NoSuchMethodError e) {
                    Log.d(TAG, "withTextSize no disponible en esta versión");
                }

                pointAnnotationManager.create(options);
            } catch (Exception e) {
                Log.e(TAG, "Error al añadir el marcador del usuario", e);
            }
        }
    }

    private void drawRouteLine() {
        if (currentUserLocation == null) return;

        try {
            Point destPoint = Point.fromLngLat(targetLongitude, targetLatitude);

            List<Point> points = new ArrayList<>();
            points.add(currentUserLocation);
            points.add(destPoint);

            polylineAnnotationManager.deleteAll();

            int primaryColor = ContextCompat.getColor(this, R.color.primary);

            PolylineAnnotationOptions polylineOptions = new PolylineAnnotationOptions()
                    .withPoints(points)
                    .withLineColor(primaryColor)
                    .withLineWidth(5.0);

            polylineAnnotationManager.create(polylineOptions);

            fitCameraToBounds();

        } catch (Exception e) {
            Log.e(TAG, "Error al mostrar la ruta", e);
            Toast.makeText(this, "Error al mostrar la ruta", Toast.LENGTH_SHORT).show();
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

            EdgeInsets edgeInsets = new EdgeInsets(100, 100, 100, 100);
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
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

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}