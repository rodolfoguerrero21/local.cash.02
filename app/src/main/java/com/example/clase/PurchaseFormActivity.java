package com.example.clase;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Contacto;
import com.example.clase.modelo.Cuenta;
import com.example.clase.modelo.ProductoCompra;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PurchaseFormActivity extends AppCompatActivity {

    private DatabaseHelper database;
    private TextInputLayout descriptionLayout;
    private TextInputEditText descriptionInput;
    private Spinner accountSpinner;
    private Spinner categorySpinner;
    private CheckBox scheduledCheckBox;
    private LinearLayout itemsContainer;
    private TextView contactsText;
    private TextView totalText;
    private com.google.android.material.button.MaterialButton dateButton;
    private List<Cuenta> accounts;
    private List<Contacto> contacts;
    private boolean[] selectedContacts;
    private long selectedDate = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_form);
        applyInsets();
        database = new DatabaseHelper(this);
        accounts = database.getCuentas();
        contacts = database.getContactos();
        selectedContacts = new boolean[contacts.size()];

        descriptionLayout = findViewById(R.id.purchaseDescriptionLayout);
        descriptionInput = findViewById(R.id.purchaseDescriptionInput);
        accountSpinner = findViewById(R.id.purchaseAccountSpinner);
        categorySpinner = findViewById(R.id.purchaseCategorySpinner);
        scheduledCheckBox = findViewById(R.id.scheduledPurchaseCheckBox);
        itemsContainer = findViewById(R.id.purchaseItemsContainer);
        contactsText = findViewById(R.id.selectedContactsText);
        totalText = findViewById(R.id.purchaseTotalText);
        dateButton = findViewById(R.id.purchaseDateButton);

        List<String> accountOptions = new ArrayList<>();
        if (accounts.isEmpty()) {
            accountOptions.add(getString(R.string.no_account_associated));
        } else {
            for (Cuenta account : accounts) accountOptions.add(account.toString());
        }
        accountSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, accountOptions));
        categorySpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, new String[]{
                getString(R.string.category_food),
                getString(R.string.category_transport),
                getString(R.string.category_studies),
                getString(R.string.category_personal)
        }));
        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        findViewById(R.id.addPurchaseItemButton).setOnClickListener(view -> addItemRow());
        findViewById(R.id.selectContactsButton).setOnClickListener(view -> selectContacts());
        findViewById(R.id.savePurchaseButton).setOnClickListener(view -> savePurchase());
        dateButton.setOnClickListener(view -> selectDate());

        updateDateButton();
        addItemRow();
    }

    private void addItemRow() {
        View row = getLayoutInflater().inflate(R.layout.item_purchase_input, itemsContainer, false);
        TextInputEditText quantityInput = row.findViewById(R.id.itemQuantityInput);
        TextInputEditText priceInput = row.findViewById(R.id.itemPriceInput);
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { updateTotal(); }
            @Override public void afterTextChanged(Editable editable) { }
        };
        quantityInput.addTextChangedListener(watcher);
        priceInput.addTextChangedListener(watcher);
        ImageButton removeButton = row.findViewById(R.id.removeItemButton);
        removeButton.setOnClickListener(view -> {
            if (itemsContainer.getChildCount() > 1) {
                itemsContainer.removeView(row);
                updateTotal();
            } else {
                Toast.makeText(this, R.string.one_product_required, Toast.LENGTH_SHORT).show();
            }
        });
        itemsContainer.addView(row);
        updateTotal();
    }

    private void selectDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDate);
        new DatePickerDialog(this, (picker, year, month, day) -> {
            calendar.set(year, month, day, 12, 0, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            selectedDate = calendar.getTimeInMillis();
            updateDateButton();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButton() {
        dateButton.setText(UiUtils.date(selectedDate));
    }

    private void selectContacts() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, R.string.create_contact_first, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[contacts.size()];
        for (int index = 0; index < contacts.size(); index++) names[index] = contacts.get(index).getNombre();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_contacts)
                .setMultiChoiceItems(names, selectedContacts,
                        (dialog, which, checked) -> selectedContacts[which] = checked)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> updateContactsText())
                .show();
    }

    private void updateContactsText() {
        List<String> names = new ArrayList<>();
        for (int index = 0; index < contacts.size(); index++) {
            if (selectedContacts[index]) names.add(contacts.get(index).getNombre());
        }
        contactsText.setText(names.isEmpty()
                ? getString(R.string.no_contacts_selected)
                : android.text.TextUtils.join(", ", names));
    }

    private void updateTotal() {
        totalText.setText(getString(R.string.total_format, UiUtils.money(calculateTotal())));
    }

    private double calculateTotal() {
        double total = 0;
        for (int index = 0; index < itemsContainer.getChildCount(); index++) {
            View row = itemsContainer.getChildAt(index);
            total += parseDouble((TextInputEditText) row.findViewById(R.id.itemQuantityInput))
                    * parseDouble((TextInputEditText) row.findViewById(R.id.itemPriceInput));
        }
        return total;
    }

    private void savePurchase() {
        String description = textOf(descriptionInput);
        if (description.isEmpty()) {
            descriptionLayout.setError(getString(R.string.required_fields));
            return;
        }

        List<ProductoCompra> products = new ArrayList<>();
        for (int index = 0; index < itemsContainer.getChildCount(); index++) {
            View row = itemsContainer.getChildAt(index);
            String name = textOf(row.findViewById(R.id.itemNameInput));
            int quantity;
            double price;
            try {
                quantity = Integer.parseInt(textOf(row.findViewById(R.id.itemQuantityInput)));
                price = Double.parseDouble(textOf(row.findViewById(R.id.itemPriceInput)));
            } catch (NumberFormatException exception) {
                Toast.makeText(this, R.string.required_fields, Toast.LENGTH_LONG).show();
                return;
            }
            if (name.isEmpty() || quantity <= 0 || price <= 0) {
                Toast.makeText(this, R.string.required_fields, Toast.LENGTH_LONG).show();
                return;
            }
            products.add(new ProductoCompra(name, quantity, price));
        }

        List<Long> participants = new ArrayList<>();
        for (int index = 0; index < contacts.size(); index++) {
            if (selectedContacts[index]) participants.add(contacts.get(index).getId());
        }
        Long accountId = accounts.isEmpty()
                ? null
                : accounts.get(accountSpinner.getSelectedItemPosition()).getId();
        String category = categorySpinner.getSelectedItem().toString();
        long result = database.insertCompra(description, category, selectedDate,
                scheduledCheckBox.isChecked(), accountId, products, participants);
        if (result != -1) {
            Toast.makeText(this, R.string.purchase_saved, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private double parseDouble(TextInputEditText input) {
        try {
            return Double.parseDouble(textOf(input));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.purchaseFormRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
