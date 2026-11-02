package com.example.clase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Cuenta;
import com.example.clase.modelo.Movimiento;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class TransactionsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 2201;

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
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.transactions);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.transactions_subtitle);
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.addButton))
                .setText(R.string.new_transaction);
        findViewById(R.id.addButton).setOnClickListener(view -> showTransactionDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {
        List<Movimiento> movimientos = database.getMovimientos();
        itemsContainer.removeAllViews();
        emptyStateText.setText(R.string.no_transactions);
        emptyStateText.setVisibility(movimientos.isEmpty() ? View.VISIBLE : View.GONE);
        for (Movimiento movimiento : movimientos) {
            boolean income = DatabaseHelper.TIPO_INGRESO.equals(movimiento.getTipo());
            String location = movimiento.getLatitud() == null
                    ? ""
                    : " · GPS " + String.format(java.util.Locale.getDefault(), "%.4f, %.4f",
                    movimiento.getLatitud(), movimiento.getLongitud());
            String detail = movimiento.getCuenta() + " · " + UiUtils.date(movimiento.getFecha()) + location;
            String amount = (income ? "+" : "-") + UiUtils.money(movimiento.getMonto());
            UiUtils.addCard(this, itemsContainer, movimiento.getDescripcion(), detail, amount,
                    getString(R.string.delete), view -> confirmDelete(movimiento));
        }
    }

    private void showTransactionDialog() {
        List<Cuenta> accounts = database.getCuentas();
        if (accounts.isEmpty()) {
            Toast.makeText(this, R.string.create_account_first, Toast.LENGTH_LONG).show();
            startActivity(new android.content.Intent(this, AccountsActivity.class));
            return;
        }

        View content = getLayoutInflater().inflate(R.layout.dialog_transaction, null);
        Spinner accountSpinner = content.findViewById(R.id.transactionAccountSpinner);
        RadioGroup typeGroup = content.findViewById(R.id.transactionTypeGroup);
        TextInputLayout descriptionLayout = content.findViewById(R.id.transactionDescriptionLayout);
        TextInputLayout amountLayout = content.findViewById(R.id.transactionAmountLayout);
        TextInputEditText descriptionInput = content.findViewById(R.id.transactionDescriptionInput);
        TextInputEditText amountInput = content.findViewById(R.id.transactionAmountInput);
        CheckBox locationCheckBox = content.findViewById(R.id.useLocationCheckBox);
        accountSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, accounts));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_transaction)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String description = textOf(descriptionInput);
                    double amount;
                    try {
                        amount = Double.parseDouble(textOf(amountInput));
                    } catch (NumberFormatException exception) {
                        amountLayout.setError(getString(R.string.required_fields));
                        return;
                    }
                    if (description.isEmpty() || amount <= 0) {
                        descriptionLayout.setError(getString(R.string.required_fields));
                        return;
                    }
                    Location location = null;
                    if (locationCheckBox.isChecked()) {
                        if (!hasLocationPermission()) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST);
                            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
                            return;
                        }
                        location = getLastLocation();
                        if (location == null) {
                            Toast.makeText(this, R.string.location_not_attached, Toast.LENGTH_LONG).show();
                        }
                    }
                    Cuenta account = accounts.get(accountSpinner.getSelectedItemPosition());
                    String type = typeGroup.getCheckedRadioButtonId() == R.id.typeIncome
                            ? DatabaseHelper.TIPO_INGRESO : DatabaseHelper.TIPO_EGRESO;
                    Double latitude = location == null ? null : location.getLatitude();
                    Double longitude = location == null ? null : location.getLongitude();
                    if (database.insertMovimiento(account.getId(), type, amount, description,
                            System.currentTimeMillis(), latitude, longitude) != -1) {
                        Toast.makeText(this, R.string.transaction_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadTransactions();
                    }
                }));
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private Location getLastLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (gps == null) return network;
        if (network == null) return gps;
        return gps.getTime() >= network.getTime() ? gps : network;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void confirmDelete(Movimiento movimiento) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (!database.deleteMovimiento(movimiento.getId())) {
                        Toast.makeText(this, R.string.purchase_movement_locked, Toast.LENGTH_LONG).show();
                    }
                    loadTransactions();
                }).show();
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
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
