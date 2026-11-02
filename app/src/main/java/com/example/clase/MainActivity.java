package com.example.clase;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.clase.Database.DatabaseHelper;
import com.example.clase.modelo.Compra;
import com.example.clase.ui.CategoryDonutView;
import com.example.clase.ui.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK
                    | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private DatabaseHelper database;
    private View mainView;
    private boolean authenticated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainView = findViewById(R.id.main);
        mainView.setVisibility(View.INVISIBLE);
        applyInsets();
        database = new DatabaseHelper(this);
        bindNavigation();
        authenticateUser();
    }

    private void bindNavigation() {
        findViewById(R.id.openMapButton).setOnClickListener(view -> open(MapActivity.class));
        findViewById(R.id.mapNavigationButton).setOnClickListener(view -> open(MapActivity.class));
        findViewById(R.id.contactNavigationButton).setOnClickListener(view -> open(ContactsActivity.class));
        findViewById(R.id.accountsButton).setOnClickListener(view -> open(AccountsActivity.class));
        findViewById(R.id.transactionsButton).setOnClickListener(view -> open(TransactionsActivity.class));
        findViewById(R.id.debtsButton).setOnClickListener(view -> open(DebtsActivity.class));
        findViewById(R.id.mapPointsButton).setOnClickListener(view -> open(MapPointsActivity.class));
        findViewById(R.id.addPurchaseButton).setOnClickListener(view -> open(PurchaseFormActivity.class));
        findViewById(R.id.viewAllPurchasesButton).setOnClickListener(view -> open(PurchasesActivity.class));
    }

    private void authenticateUser() {
        BiometricManager manager = BiometricManager.from(this);
        if (manager.canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            showAuthenticationSetupDialog();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        authenticated = true;
                        mainView.setVisibility(View.VISIBLE);
                        loadDashboard();
                        Toast.makeText(MainActivity.this,
                                R.string.biometric_success, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errorMessage) {
                        super.onAuthenticationError(errorCode, errorMessage);
                        if (!authenticated) finish();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(MainActivity.this,
                                R.string.biometric_subtitle, Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_title))
                .setSubtitle(getString(R.string.biometric_subtitle))
                .setAllowedAuthenticators(AUTHENTICATORS)
                .setConfirmationRequired(false)
                .build();
        prompt.authenticate(promptInfo);
    }

    private void showAuthenticationSetupDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.biometric_title)
                .setMessage(R.string.biometric_unavailable)
                .setCancelable(false)
                .setNegativeButton(R.string.exit, (dialog, which) -> finish())
                .setPositiveButton(R.string.open_security_settings, (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)))
                .show();
    }

    private void loadDashboard() {
        double spent = database.getTotalEgresos();
        double available = database.getSaldoTotal();
        double budget = Math.max(0, spent + available);
        int percentage = budget <= 0
                ? 0
                : (int) Math.min(100, Math.round((spent / budget) * 100));

        ((TextView) findViewById(R.id.totalSpentValue)).setText(UiUtils.money(spent));
        ((TextView) findViewById(R.id.availableBalanceValue)).setText(UiUtils.money(available));
        ((TextView) findViewById(R.id.monthlyBudgetValue)).setText(UiUtils.money(budget));
        ((TextView) findViewById(R.id.budgetSpentText))
                .setText(getString(R.string.budget_spent_format, percentage));
        ((ProgressBar) findViewById(R.id.budgetProgress)).setProgress(percentage);
        ((TextView) findViewById(R.id.currentMonthText)).setText(
                new SimpleDateFormat("MMMM yyyy", new Locale("es", "CL"))
                        .format(new java.util.Date()));

        Map<String, Double> categoryTotals = database.getGastosPorCategoria();
        String[] categories = {
                getString(R.string.category_food),
                getString(R.string.category_transport),
                getString(R.string.category_studies),
                getString(R.string.category_personal)
        };
        double[] values = new double[categories.length];
        for (int index = 0; index < categories.length; index++) {
            values[index] = categoryTotals.getOrDefault(categories[index], 0.0);
        }
        int[] categoryPercentages = calculatePercentages(values);
        int[] categoryLabels = {
                R.id.foodPercentageText,
                R.id.transportPercentageText,
                R.id.studiesPercentageText,
                R.id.personalPercentageText
        };
        for (int index = 0; index < categories.length; index++) {
            ((TextView) findViewById(categoryLabels[index])).setText(
                    getString(R.string.category_percentage_format,
                            categories[index], categoryPercentages[index]));
        }
        ((CategoryDonutView) findViewById(R.id.categoryDonutView)).setValues(
                (float) values[0], (float) values[1], (float) values[2], (float) values[3]);

        int[] rows = {R.id.recentPurchaseRow1, R.id.recentPurchaseRow2, R.id.recentPurchaseRow3};
        int[] names = {R.id.recentPurchaseName1, R.id.recentPurchaseName2, R.id.recentPurchaseName3};
        int[] amounts = {R.id.recentPurchaseAmount1, R.id.recentPurchaseAmount2, R.id.recentPurchaseAmount3};
        List<Compra> recent = database.getComprasRecientes(3);
        for (int index = 0; index < rows.length; index++) {
            View row = findViewById(rows[index]);
            if (index >= recent.size()) {
                row.setVisibility(View.GONE);
                continue;
            }
            Compra purchase = recent.get(index);
            row.setVisibility(View.VISIBLE);
            ((TextView) findViewById(names[index])).setText(purchase.getDescripcion());
            ((TextView) findViewById(amounts[index])).setText(UiUtils.money(purchase.getTotal()));
        }
    }

    private int[] calculatePercentages(double[] values) {
        int[] percentages = new int[values.length];
        double[] remainders = new double[values.length];
        double total = 0;
        for (double value : values) total += value;
        if (total <= 0) return percentages;

        int assigned = 0;
        for (int index = 0; index < values.length; index++) {
            double exact = values[index] * 100 / total;
            percentages[index] = (int) Math.floor(exact);
            remainders[index] = exact - percentages[index];
            assigned += percentages[index];
        }
        while (assigned < 100) {
            int largestIndex = 0;
            for (int index = 1; index < remainders.length; index++) {
                if (remainders[index] > remainders[largestIndex]) largestIndex = index;
            }
            percentages[largestIndex]++;
            remainders[largestIndex] = -1;
            assigned++;
        }
        return percentages;
    }

    private void open(Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return windowInsets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (authenticated) loadDashboard();
    }

    @Override
    protected void onDestroy() {
        if (database != null) database.close();
        super.onDestroy();
    }
}
