package com.example.clase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.PuntoMapa;

import java.util.HashMap;
import java.util.Map;

import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.Icon;
import org.maplibre.android.annotations.IconFactory;
import org.maplibre.android.annotations.Marker;
import org.maplibre.android.annotations.MarkerOptions;
import org.maplibre.android.camera.CameraPosition;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapView;
import org.maplibre.android.maps.MapLibreMap;

public class MapActivity extends AppCompatActivity {

    private static final String MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    public static final String EXTRA_POINT_ID = "point_id";
    public static final String EXTRA_POINT_LATITUDE = "point_latitude";
    public static final String EXTRA_POINT_LONGITUDE = "point_longitude";
    public static final String EXTRA_PURCHASE_LOCATION_MODE = "purchase_location_mode";
    public static final String EXTRA_SELECTED_POINT_NAME = "selected_point_name";
    public static final long MARKET_POINT_ID = -101;
    public static final long FAIR_POINT_ID = -102;
    public static final long STORE_POINT_ID = -103;
    public static final double MARKET_LATITUDE = -33.4372;
    public static final double MARKET_LONGITUDE = -70.6506;
    public static final double FAIR_LATITUDE = -33.4578;
    public static final double FAIR_LONGITUDE = -70.6819;
    public static final double STORE_LATITUDE = -33.4440;
    public static final double STORE_LONGITUDE = -70.6970;

    private MapView mapView;
    private MapLibreMap mapLibreMap;
    private DatabaseHelper database;
    private LocationManager locationManager;
    private Marker userLocationMarker;
    private Icon userLocationIcon;
    private Icon newPointIcon;
    private boolean mapFeaturesReady;
    private boolean purchaseLocationMode;
    private boolean centerOnNextLocation = true;
    private final Map<Marker, PuntoMapa> userCreatedMarkers = new HashMap<>();
    private Marker selectedUserMarker;
    private long focusedPointId = -1;
    private MaterialCardView mapInstructionCard;
    private MaterialCardView pointActionsCard;
    private TextView selectedPointName;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            showUserLocation(location, true);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
        }

        @Override
        @SuppressWarnings("deprecation")
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapLibre.getInstance(this);
        setContentView(R.layout.activity_map);
        database = new DatabaseHelper(this);
        purchaseLocationMode = getIntent().getBooleanExtra(EXTRA_PURCHASE_LOCATION_MODE, false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mapRoot), (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        findViewById(R.id.locationButton).setOnClickListener(view -> {
            centerOnNextLocation = true;
            requestUserLocation();
        });
        mapInstructionCard = findViewById(R.id.mapInstructionCard);
        if (purchaseLocationMode) {
            ((TextView) findViewById(R.id.mapInstructionText))
                    .setText(R.string.purchase_map_instruction);
        }
        pointActionsCard = findViewById(R.id.pointActionsCard);
        selectedPointName = findViewById(R.id.selectedPointName);
        findViewById(R.id.deletePointButton).setOnClickListener(view -> deleteSelectedPoint());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        focusedPointId = getIntent().getLongExtra(EXTRA_POINT_ID, -1);
        boolean openSavedPoint = getIntent().hasExtra(EXTRA_POINT_LATITUDE)
                && getIntent().hasExtra(EXTRA_POINT_LONGITUDE);
        LatLng initialPosition = openSavedPoint
                ? new LatLng(
                getIntent().getDoubleExtra(EXTRA_POINT_LATITUDE, -33.4489),
                getIntent().getDoubleExtra(EXTRA_POINT_LONGITUDE, -70.6693))
                : new LatLng(-33.4489, -70.6693);
        centerOnNextLocation = !openSavedPoint;
        mapView.getMapAsync(map -> {
            mapLibreMap = map;
            mapLibreMap.setCameraPosition(new CameraPosition.Builder()
                    .target(initialPosition)
                    .zoom(openSavedPoint ? 16.0 : 11.5)
                    .build());
            mapLibreMap.setStyle(MAP_STYLE_URL, style -> initializeMapFeatures());
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        if (mapFeaturesReady && hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopLocationUpdates();
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        database.close();
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (hasLocationPermission()) {
            centerOnNextLocation = true;
            startLocationUpdates();
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
        }
    }

    private void initializeMapFeatures() {
        userLocationIcon = createMarkerIcon(R.color.local_cash_teal, "YO");
        newPointIcon = createMarkerIcon(R.color.local_cash_teal_dark, "+");

        addPredefinedMarker(
                MARKET_POINT_ID,
                new LatLng(MARKET_LATITUDE, MARKET_LONGITUDE),
                R.string.market_name,
                R.string.market_detail,
                createMarkerIcon(R.color.local_cash_green, "$"));
        addPredefinedMarker(
                FAIR_POINT_ID,
                new LatLng(FAIR_LATITUDE, FAIR_LONGITUDE),
                R.string.fair_name,
                R.string.fair_detail,
                createMarkerIcon(R.color.local_cash_teal, "F"));
        addPredefinedMarker(
                STORE_POINT_ID,
                new LatLng(STORE_LATITUDE, STORE_LONGITUDE),
                R.string.store_name,
                R.string.store_detail,
                createMarkerIcon(R.color.local_cash_teal_dark, "A"));
        loadSavedPoints();

        mapLibreMap.addOnMapLongClickListener(point -> {
            hidePointActions();
            showAddPointDialog(point);
            return true;
        });
        mapLibreMap.setOnMarkerClickListener(marker -> {
            if (userCreatedMarkers.containsKey(marker)) {
                showPointActions(marker);
            } else {
                hidePointActions();
            }
            return false;
        });

        mapFeaturesReady = true;
        requestUserLocation();
    }

    private void loadSavedPoints() {
        for (PuntoMapa point : database.getPuntosMapa()) {
            Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                    .position(new LatLng(point.getLatitud(), point.getLongitud()))
                    .title(point.getNombre())
                    .snippet(formatCoordinates(point.getLatitud(), point.getLongitud()))
                    .icon(newPointIcon));
            userCreatedMarkers.put(marker, point);
            if (point.getId() == focusedPointId) {
                mapLibreMap.selectMarker(marker);
                showPointActions(marker);
            }
        }
    }

    private void addPredefinedMarker(long pointId, LatLng position, int titleResource,
                                     int detailResource, Icon icon) {
        Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                .position(position)
                .title(getString(titleResource))
                .snippet(getString(detailResource))
                .icon(icon));
        if (pointId == focusedPointId) {
            mapLibreMap.selectMarker(marker);
        }
    }

    private void showAddPointDialog(LatLng point) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_map_point, null);
        TextInputLayout nameInputLayout = dialogView.findViewById(R.id.pointNameInputLayout);
        TextInputEditText nameInput = dialogView.findViewById(R.id.pointNameInput);
        android.widget.TextView coordinatesText = dialogView.findViewById(R.id.pointCoordinates);
        coordinatesText.setText(formatCoordinates(point));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_map_point)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save_point, null)
                .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String pointName = nameInput.getText() == null
                        ? ""
                        : nameInput.getText().toString().trim();

                if (pointName.isEmpty()) {
                    nameInputLayout.setError(getString(R.string.point_name_required));
                    return;
                }

                long pointId = database.insertPuntoMapa(
                        pointName, point.getLatitude(), point.getLongitude());
                if (pointId == -1) {
                    Toast.makeText(this, R.string.point_save_error, Toast.LENGTH_LONG).show();
                    return;
                }
                if (purchaseLocationMode) {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_SELECTED_POINT_NAME, pointName);
                    result.putExtra(EXTRA_POINT_LATITUDE, point.getLatitude());
                    result.putExtra(EXTRA_POINT_LONGITUDE, point.getLongitude());
                    setResult(RESULT_OK, result);
                    Toast.makeText(this, R.string.purchase_location_selected,
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    finish();
                    return;
                }
                Marker marker = mapLibreMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title(pointName)
                        .snippet(formatCoordinates(point))
                        .icon(newPointIcon));
                userCreatedMarkers.put(marker, new PuntoMapa(
                        pointId, pointName, point.getLatitude(), point.getLongitude()));
                Toast.makeText(this, R.string.point_added, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
            nameInput.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });

        dialog.show();
    }

    private void showPointActions(Marker marker) {
        selectedUserMarker = marker;
        selectedPointName.setText(marker.getTitle());
        mapInstructionCard.setVisibility(View.GONE);
        pointActionsCard.setVisibility(View.VISIBLE);
    }

    private void hidePointActions() {
        selectedUserMarker = null;
        pointActionsCard.setVisibility(View.GONE);
        mapInstructionCard.setVisibility(View.VISIBLE);
    }

    private void deleteSelectedPoint() {
        if (selectedUserMarker == null || mapLibreMap == null) {
            return;
        }

        Marker markerToDelete = selectedUserMarker;
        PuntoMapa pointToDelete = userCreatedMarkers.get(markerToDelete);
        if (pointToDelete == null || !database.deletePuntoMapa(pointToDelete.getId())) {
            Toast.makeText(this, R.string.point_delete_error, Toast.LENGTH_LONG).show();
            return;
        }
        mapLibreMap.deselectMarker(markerToDelete);
        mapLibreMap.removeMarker(markerToDelete);
        userCreatedMarkers.remove(markerToDelete);
        hidePointActions();
        Toast.makeText(this, R.string.point_deleted, Toast.LENGTH_SHORT).show();
    }

    private void requestUserLocation() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        startLocationUpdates();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission() || locationManager == null || !mapFeaturesReady) {
            return;
        }

        stopLocationUpdates();
        Location lastKnownLocation = null;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    3f,
                    locationListener
            );
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener
            );
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        }

        if (lastKnownLocation != null) {
            showUserLocation(lastKnownLocation, false);
        } else {
            Toast.makeText(this, R.string.searching_location, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    private void showUserLocation(Location location, boolean isFreshUpdate) {
        if (mapLibreMap == null || userLocationIcon == null) {
            return;
        }

        LatLng userPosition = new LatLng(location.getLatitude(), location.getLongitude());
        if (userLocationMarker != null) {
            mapLibreMap.removeMarker(userLocationMarker);
        }

        userLocationMarker = mapLibreMap.addMarker(new MarkerOptions()
                .position(userPosition)
                .title(getString(R.string.user_location))
                .snippet(getString(R.string.user_location_detail))
                .icon(userLocationIcon));

        if (centerOnNextLocation && isFreshUpdate) {
            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 12.8), 1000);
            centerOnNextLocation = false;
        }
    }

    private Icon createMarkerIcon(int colorResource, String label) {
        int width = dp(48);
        int height = dp(58);
        float centerX = width / 2f;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markerPaint.setColor(ContextCompat.getColor(this, colorResource));

        Path pointer = new Path();
        pointer.moveTo(centerX - dp(11), dp(34));
        pointer.lineTo(centerX, dp(56));
        pointer.lineTo(centerX + dp(11), dp(34));
        pointer.close();
        canvas.drawPath(pointer, markerPaint);
        canvas.drawCircle(centerX, dp(22), dp(19), markerPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(this, R.color.white));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(dp(label.length() > 1 ? 12 : 18));
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = dp(22) - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, centerX, baseline, textPaint);

        return IconFactory.getInstance(this).fromBitmap(bitmap);
    }

    private String formatCoordinates(LatLng point) {
        return formatCoordinates(point.getLatitude(), point.getLongitude());
    }

    private String formatCoordinates(double latitude, double longitude) {
        return String.format(
                java.util.Locale.getDefault(),
                "%.5f, %.5f",
                latitude,
                longitude
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
