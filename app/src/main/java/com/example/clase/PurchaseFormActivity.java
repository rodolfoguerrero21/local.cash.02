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
import com.example.clase.modelo.Compra;
import com.example.clase.modelo.Contacto;
import com.example.clase.modelo.Cuenta;
import com.example.clase.modelo.ProductoCompra;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PurchaseFormActivity extends AppCompatActivity {

    public static final String EXTRA_PURCHASE_ID = "purchase_id";

    private DatabaseHelper database;
    private TextInputLayout descriptionLayout;
    private TextInputEditText descriptionInput;
    private Spinner accountSpinner;
    private Spinner categorySpinner;
    private CheckBox scheduledCheckBox;
    private LinearLayout itemsContainer;
    private LinearLayout participantContributionsContainer;
    private TextView contactsText;
    private TextView totalText;
    private TextView accountAmountText;
    private com.google.android.material.button.MaterialButton dateButton;
    private List<Cuenta> accounts;
    private List<Contacto> contacts;
    private boolean[] selectedContacts;
    private final Map<Long, TextInputEditText> contributionInputs = new LinkedHashMap<>();
    private final Map<Long, TextInputLayout> contributionLayouts = new LinkedHashMap<>();
    private final Map<Long, String> contributionValues = new LinkedHashMap<>();
    private long selectedDate = System.currentTimeMillis();
    private long editingPurchaseId = -1;

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
        participantContributionsContainer = findViewById(R.id.participantContributionsContainer);
        contactsText = findViewById(R.id.selectedContactsText);
        totalText = findViewById(R.id.purchaseTotalText);
        accountAmountText = findViewById(R.id.purchaseAccountAmountText);
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

        editingPurchaseId = getIntent().getLongExtra(EXTRA_PURCHASE_ID, -1);
        if (editingPurchaseId == -1) {
            updateDateButton();
            addItemRow();
        } else {
            loadPurchaseForEditing();
        }
    }

    private void addItemRow() {
        addItemRow(null);
    }

    private void addItemRow(ProductoCompra product) {
        View row = getLayoutInflater().inflate(R.layout.item_purchase_input, itemsContainer, false);
        TextInputEditText nameInput = row.findViewById(R.id.itemNameInput);
        TextInputEditText quantityInput = row.findViewById(R.id.itemQuantityInput);
        TextInputEditText priceInput = row.findViewById(R.id.itemPriceInput);
        if (product != null) {
            nameInput.setText(product.getNombre());
            quantityInput.setText(String.valueOf(product.getCantidad()));
            priceInput.setText(editableNumber(product.getPrecioUnitario()));
        }
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

    private void loadPurchaseForEditing() {
        Compra purchase = database.getCompra(editingPurchaseId);
        if (purchase == null) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.purchaseFormTitle)).setText(R.string.edit_future_purchase);
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.savePurchaseButton))
                .setText(R.string.update_purchase);
        descriptionInput.setText(purchase.getDescripcion());
        selectedDate = purchase.getFecha();
        scheduledCheckBox.setChecked(purchase.isProgramada());
        selectAccount(purchase.getCuentaId());
        selectCategory(purchase.getCategoria());
        updateDateButton();

        itemsContainer.removeAllViews();
        List<ProductoCompra> products = database.getProductosCompra(editingPurchaseId);
        for (ProductoCompra product : products) addItemRow(product);
        if (products.isEmpty()) addItemRow();

        Map<Long, Double> contributions = database.getAportesCompra(editingPurchaseId);
        for (int index = 0; index < contacts.size(); index++) {
            Double contribution = contributions.get(contacts.get(index).getId());
            if (contribution == null) continue;
            selectedContacts[index] = true;
            contributionValues.put(contacts.get(index).getId(), editableNumber(contribution));
        }
        updateContactsText();
    }

    private void selectAccount(Long accountId) {
        if (accountId == null) return;
        for (int index = 0; index < accounts.size(); index++) {
            if (accounts.get(index).getId() == accountId) {
                accountSpinner.setSelection(index);
                return;
            }
        }
    }

    private void selectCategory(String category) {
        for (int index = 0; index < categorySpinner.getCount(); index++) {
            if (category.equals(categorySpinner.getItemAtPosition(index).toString())) {
                categorySpinner.setSelection(index);
                return;
            }
        }
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
        boolean[] pendingSelection = selectedContacts.clone();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.select_contacts)
                .setMultiChoiceItems(names, pendingSelection,
                        (dialog, which, checked) -> pendingSelection[which] = checked)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    selectedContacts = pendingSelection;
                    updateContactsText();
                })
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
        renderContributionInputs();
    }

    private void renderContributionInputs() {
        for (Map.Entry<Long, TextInputEditText> entry : contributionInputs.entrySet()) {
            contributionValues.put(entry.getKey(), textOf(entry.getValue()));
        }
        participantContributionsContainer.removeAllViews();
        contributionInputs.clear();
        contributionLayouts.clear();

        boolean hasParticipants = false;
        for (int index = 0; index < contacts.size(); index++) {
            if (!selectedContacts[index]) continue;
            hasParticipants = true;
            Contacto contact = contacts.get(index);
            View row = getLayoutInflater().inflate(
                    R.layout.item_participant_contribution,
                    participantContributionsContainer,
                    false);
            ((TextView) row.findViewById(R.id.participantNameText)).setText(contact.getNombre());
            TextInputLayout amountLayout = row.findViewById(R.id.participantContributionLayout);
            TextInputEditText amountInput = row.findViewById(R.id.participantContributionInput);
            String previousValue = contributionValues.get(contact.getId());
            if (previousValue != null) amountInput.setText(previousValue);
            amountInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                    amountLayout.setError(null);
                    contributionValues.put(contact.getId(), text.toString());
                    updateTotal();
                }
                @Override public void afterTextChanged(Editable editable) { }
            });
            contributionInputs.put(contact.getId(), amountInput);
            contributionLayouts.put(contact.getId(), amountLayout);
            participantContributionsContainer.addView(row);
        }
        participantContributionsContainer.setVisibility(hasParticipants ? View.VISIBLE : View.GONE);
        updateTotal();
    }

    private void updateTotal() {
        double total = calculateTotal();
        double accountAmount = Math.max(0, total - calculateParticipantContributions());
        totalText.setText(getString(R.string.total_format, UiUtils.money(total)));
        accountAmountText.setText(accounts.isEmpty()
                ? getString(R.string.no_account_charge)
                : getString(R.string.account_charge_format, UiUtils.money(accountAmount)));
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

    private double calculateParticipantContributions() {
        double total = 0;
        for (TextInputEditText input : contributionInputs.values()) {
            total += parseDouble(input);
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

        Map<Long, Double> participantContributions = new LinkedHashMap<>();
        double totalContributions = 0;
        for (int index = 0; index < contacts.size(); index++) {
            if (!selectedContacts[index]) continue;
            long contactId = contacts.get(index).getId();
            double contribution = parseDouble(contributionInputs.get(contactId));
            if (contribution <= 0) {
                contributionLayouts.get(contactId)
                        .setError(getString(R.string.participant_contribution_required));
                return;
            }
            participantContributions.put(contactId, contribution);
            totalContributions += contribution;
        }
        double purchaseTotal = 0;
        for (ProductoCompra product : products) purchaseTotal += product.getSubtotal();
        if (totalContributions > purchaseTotal + 0.01) {
            Toast.makeText(this, R.string.contributions_exceed_total, Toast.LENGTH_LONG).show();
            return;
        }
        Long accountId = accounts.isEmpty()
                ? null
                : accounts.get(accountSpinner.getSelectedItemPosition()).getId();
        String category = categorySpinner.getSelectedItem().toString();
        boolean saved = editingPurchaseId == -1
                ? database.insertCompra(description, category, selectedDate,
                        scheduledCheckBox.isChecked(), accountId, products,
                        participantContributions) != -1
                : database.updateCompra(editingPurchaseId, description, category, selectedDate,
                        scheduledCheckBox.isChecked(), accountId, products,
                        participantContributions);
        if (saved) {
            Toast.makeText(this, editingPurchaseId == -1
                    ? R.string.purchase_saved
                    : R.string.purchase_updated, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String editableNumber(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
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
