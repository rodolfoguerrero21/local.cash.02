package com.example.clase;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Deuda;
import com.example.clase.ui.UiUtils;

import java.util.List;

public class DebtsActivity extends AppCompatActivity {

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
        ((TextView) findViewById(R.id.screenTitle)).setText(R.string.debts);
        ((TextView) findViewById(R.id.screenSubtitle)).setText(R.string.debts_subtitle);
        findViewById(R.id.addButton).setVisibility(View.GONE);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams scrollParams =
                (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)
                        findViewById(R.id.itemsScroll).getLayoutParams();
        scrollParams.topToBottom = R.id.header;
        findViewById(R.id.itemsScroll).setLayoutParams(scrollParams);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDebts();
    }

    private void loadDebts() {
        List<Deuda> debts = database.getDeudas();
        itemsContainer.removeAllViews();
        emptyStateText.setText(R.string.no_debts);
        emptyStateText.setVisibility(debts.isEmpty() ? View.VISIBLE : View.GONE);
        for (Deuda debt : debts) {
            String detail = debt.getCompra() + " · " +
                    getString(debt.isPagada() ? R.string.paid : R.string.pending);
            String action = getString(debt.isPagada() ? R.string.mark_pending : R.string.mark_paid);
            UiUtils.addCard(this, itemsContainer, debt.getContacto(), detail,
                    UiUtils.money(debt.getMonto()), action, view -> {
                        database.setDeudaPagada(debt.getId(), !debt.isPagada());
                        loadDebts();
                    });
        }
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
