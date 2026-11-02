package com.example.clase.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.clase.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class UiUtils {

    private UiUtils() {
    }

    public static String money(double amount) {
        return "$" + NumberFormat.getIntegerInstance(new Locale("es", "CL")).format(Math.round(amount));
    }

    public static String date(long timestamp) {
        return new SimpleDateFormat("dd MMM yyyy", new Locale("es", "CL")).format(new Date(timestamp));
    }

    public static MaterialCardView addCard(
            Context context,
            LinearLayout container,
            String title,
            String detail,
            String trailing,
            String action,
            View.OnClickListener actionListener
    ) {
        return addCard(context, container, title, detail, trailing,
                null, null, action, actionListener);
    }

    public static MaterialCardView addCard(
            Context context,
            LinearLayout container,
            String title,
            String detail,
            String trailing,
            String primaryAction,
            View.OnClickListener primaryActionListener,
            String secondaryAction,
            View.OnClickListener secondaryActionListener
    ) {
        MaterialCardView card = new MaterialCardView(context);
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.local_cash_surface));
        card.setRadius(dp(context, 18));
        card.setCardElevation(dp(context, 1));
        card.setStrokeColor(ContextCompat.getColor(context, R.color.local_cash_border));
        card.setStrokeWidth(dp(context, 1));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = dp(context, 12);
        card.setLayoutParams(cardParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 14));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(context, R.color.local_cash_text));
        titleView.setTextSize(17);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        topRow.addView(titleView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        if (trailing != null && !trailing.isEmpty()) {
            TextView trailingView = new TextView(context);
            trailingView.setText(trailing);
            trailingView.setTextColor(ContextCompat.getColor(context, R.color.local_cash_teal));
            trailingView.setTextSize(17);
            trailingView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            topRow.addView(trailingView);
        }

        content.addView(topRow);

        if (detail != null && !detail.isEmpty()) {
            TextView detailView = new TextView(context);
            detailView.setText(detail);
            detailView.setTextColor(ContextCompat.getColor(context, R.color.local_cash_text_secondary));
            detailView.setTextSize(14);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            detailParams.topMargin = dp(context, 6);
            content.addView(detailView, detailParams);
        }

        if ((primaryAction != null && primaryActionListener != null)
                || (secondaryAction != null && secondaryActionListener != null)) {
            LinearLayout actions = new LinearLayout(context);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(android.view.Gravity.END);
            if (primaryAction != null && primaryActionListener != null) {
                actions.addView(actionButton(context, primaryAction, primaryActionListener));
            }
            if (secondaryAction != null && secondaryActionListener != null) {
                actions.addView(actionButton(context, secondaryAction, secondaryActionListener));
            }
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            actionsParams.topMargin = dp(context, 8);
            content.addView(actions, actionsParams);
        }

        card.addView(content);
        container.addView(card);
        return card;
    }

    private static MaterialButton actionButton(Context context, String label,
                                               View.OnClickListener listener) {
        MaterialButton button = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(label);
        button.setTextColor(ContextCompat.getColor(context, R.color.local_cash_teal));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(context, 8));
        button.setLayoutParams(params);
        return button;
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
