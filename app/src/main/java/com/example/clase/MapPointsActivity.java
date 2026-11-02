package com.example.clase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.PuntoMapa;
import com.example.clase.ui.UiUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class MapPointsActivity extends AppCompatActivity {

    private DatabaseHelper database;
    private LinearLayout itemsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management_list);
        applyInsets();
        database = new DatabaseHelper(this);
        itemsContainer = findViewById(R.id.itemsContainer);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.saved_map_points);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.saved_map_points_subtitle);
        MaterialButton openMapButton = findViewById(R.id.addButton);
        openMapButton.setText(R.string.open_full_map);
        openMapButton.setIconResource(R.drawable.ic_map);
        openMapButton.setOnClickListener(view -> startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.emptyStateText).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPoints();
    }

    private void loadPoints() {
        List<PuntoMapa> points = new ArrayList<>();
        points.add(new PuntoMapa(MapActivity.MARKET_POINT_ID, getString(R.string.market_name),
                MapActivity.MARKET_LATITUDE, MapActivity.MARKET_LONGITUDE));
        points.add(new PuntoMapa(MapActivity.FAIR_POINT_ID, getString(R.string.fair_name),
                MapActivity.FAIR_LATITUDE, MapActivity.FAIR_LONGITUDE));
        points.add(new PuntoMapa(MapActivity.STORE_POINT_ID, getString(R.string.store_name),
                MapActivity.STORE_LATITUDE, MapActivity.STORE_LONGITUDE));
        points.addAll(database.getPuntosMapa());

        itemsContainer.removeAllViews();
        for (PuntoMapa point : points) {
            View.OnClickListener openPointListener = view -> openPoint(point);
            MaterialCardView card = UiUtils.addCard(
                    this,
                    itemsContainer,
                    point.getNombre(),
                    getString(R.string.map_point_location_format,
                            point.getLatitud(), point.getLongitud()),
                    null,
                    getString(R.string.view_on_map),
                    openPointListener);
            card.setOnClickListener(openPointListener);
            card.setClickable(true);
            card.setFocusable(true);
        }
    }

    private void openPoint(PuntoMapa point) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra(MapActivity.EXTRA_POINT_ID, point.getId());
        intent.putExtra(MapActivity.EXTRA_POINT_LATITUDE, point.getLatitud());
        intent.putExtra(MapActivity.EXTRA_POINT_LONGITUDE, point.getLongitud());
        startActivity(intent);
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.managementRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}
