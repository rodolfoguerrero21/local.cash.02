package com.example.clase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Compra;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class PurchasesActivity extends AppCompatActivity {

    private DatabaseHelper database;
    private android.widget.LinearLayout itemsContainer;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management_list);
        applyInsets();
        database = new DatabaseHelper(this);
        itemsContainer = findViewById(R.id.itemsContainer);
        emptyStateText = findViewById(R.id.emptyStateText);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.purchases);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.purchases_subtitle);
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.addButton))
                .setText(R.string.new_purchase);
        findViewById(R.id.addButton).setOnClickListener(view ->
                startActivity(new Intent(this, PurchaseFormActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPurchases();
    }

    private void loadPurchases() {
        List<Compra> compras = database.getCompras();
        itemsContainer.removeAllViews();
        emptyStateText.setText(R.string.no_purchases);
        emptyStateText.setVisibility(compras.isEmpty() ? View.VISIBLE : View.GONE);
        for (Compra compra : compras) {
            String status = getString(compra.isProgramada() ? R.string.planned : R.string.completed);
            String detail = compra.getCategoria() + " · " + status + " · " + UiUtils.date(compra.getFecha()) +
                    " · " + compra.getCantidadProductos() + " productos";
            if (compra.getParticipantes() > 0) {
                detail += " · " + compra.getParticipantes() + " participantes";
            }
            UiUtils.addCard(this, itemsContainer, compra.getDescripcion(), detail,
                    UiUtils.money(compra.getTotal()), getString(R.string.delete),
                    view -> confirmDelete(compra));
        }
    }

    private void confirmDelete(Compra compra) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    database.deleteCompra(compra.getId());
                    loadPurchases();
                }).show();
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
