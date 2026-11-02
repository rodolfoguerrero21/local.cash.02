package com.example.clase;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Cuenta;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class AccountsActivity extends AppCompatActivity {

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
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.accounts);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.accounts_subtitle);
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.addButton))
                .setText(R.string.new_account);
        findViewById(R.id.addButton).setOnClickListener(view -> showAccountDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccounts();
    }

    private void loadAccounts() {
        List<Cuenta> cuentas = database.getCuentas();
        itemsContainer.removeAllViews();
        emptyStateText.setText(R.string.no_accounts);
        emptyStateText.setVisibility(cuentas.isEmpty() ? View.VISIBLE : View.GONE);
        for (Cuenta cuenta : cuentas) {
            UiUtils.addCard(this, itemsContainer, cuenta.getNombre(), cuenta.getTipo(),
                    UiUtils.money(cuenta.getSaldo()), getString(R.string.delete),
                    view -> confirmDelete(cuenta));
        }
    }

    private void showAccountDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_account, null);
        TextInputLayout nameLayout = content.findViewById(R.id.accountNameLayout);
        TextInputLayout balanceLayout = content.findViewById(R.id.accountBalanceLayout);
        TextInputEditText nameInput = content.findViewById(R.id.accountNameInput);
        TextInputEditText balanceInput = content.findViewById(R.id.accountBalanceInput);
        RadioGroup typeGroup = content.findViewById(R.id.accountTypeGroup);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_account)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = textOf(nameInput);
                    double balance;
                    try {
                        balance = Double.parseDouble(textOf(balanceInput));
                    } catch (NumberFormatException exception) {
                        balanceLayout.setError(getString(R.string.required_fields));
                        return;
                    }
                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.required_fields));
                        return;
                    }
                    String type = getString(R.string.cash);
                    if (typeGroup.getCheckedRadioButtonId() == R.id.typeSavings) type = getString(R.string.savings);
                    if (typeGroup.getCheckedRadioButtonId() == R.id.typeCard) type = getString(R.string.card);
                    if (database.insertCuenta(name, type, balance) != -1) {
                        Toast.makeText(this, R.string.account_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadAccounts();
                    }
                }));
        dialog.show();
    }

    private void confirmDelete(Cuenta cuenta) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        if (!database.deleteCuenta(cuenta.getId())) {
                            Toast.makeText(this, R.string.account_in_use, Toast.LENGTH_LONG).show();
                        }
                    } catch (SQLiteConstraintException exception) {
                        Toast.makeText(this, R.string.account_in_use, Toast.LENGTH_LONG).show();
                    }
                    loadAccounts();
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
