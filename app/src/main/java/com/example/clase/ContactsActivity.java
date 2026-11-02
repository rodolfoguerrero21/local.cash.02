package com.example.clase;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Contacto;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

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
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.contacts);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.contacts_subtitle);
        ((com.google.android.material.button.MaterialButton) findViewById(R.id.addButton))
                .setText(R.string.new_contact);
        findViewById(R.id.addButton).setOnClickListener(view -> showContactDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void loadContacts() {
        List<Contacto> contactos = database.getContactos();
        itemsContainer.removeAllViews();
        emptyStateText.setText(R.string.no_contacts);
        emptyStateText.setVisibility(contactos.isEmpty() ? View.VISIBLE : View.GONE);
        for (Contacto contacto : contactos) {
            String detail = contacto.getTelefono();
            if (contacto.getEmail() != null && !contacto.getEmail().isEmpty()) {
                detail += detail.isEmpty() ? contacto.getEmail() : " · " + contacto.getEmail();
            }
            UiUtils.addCard(this, itemsContainer, contacto.getNombre(), detail, null,
                    getString(R.string.delete), view -> confirmDelete(contacto));
        }
    }

    private void showContactDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        TextInputLayout nameLayout = content.findViewById(R.id.contactNameLayout);
        TextInputLayout phoneLayout = content.findViewById(R.id.contactPhoneLayout);
        TextInputEditText nameInput = content.findViewById(R.id.contactNameInput);
        TextInputEditText phoneInput = content.findViewById(R.id.contactPhoneInput);
        TextInputEditText emailInput = content.findViewById(R.id.contactEmailInput);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.new_contact)
                .setView(content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    String name = textOf(nameInput);
                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.required_fields));
                        return;
                    }
                    String phone = textOf(phoneInput);
                    if (phone.length() != 9) {
                        phoneLayout.setError(getString(R.string.phone_required_chile));
                        return;
                    }
                    if (database.insertContacto(name, "+56 " + phone, textOf(emailInput)) != -1) {
                        Toast.makeText(this, R.string.contact_saved, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadContacts();
                    }
                }));
        dialog.show();
    }

    private void confirmDelete(Contacto contacto) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        if (!database.deleteContacto(contacto.getId())) {
                            Toast.makeText(this, R.string.contact_in_use, Toast.LENGTH_LONG).show();
                        }
                    } catch (SQLiteConstraintException exception) {
                        Toast.makeText(this, R.string.contact_in_use, Toast.LENGTH_LONG).show();
                    }
                    loadContacts();
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
