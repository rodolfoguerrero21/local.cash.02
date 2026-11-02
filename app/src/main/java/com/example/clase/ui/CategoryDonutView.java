package com.example.clase.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.clase.R;

public class CategoryDonutView extends View {

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final float[] values = new float[4];
    private final int[] colors = {
            R.color.local_cash_teal,
            R.color.local_cash_green,
            R.color.local_cash_mint_dark,
            R.color.local_cash_gold
    };

    public CategoryDonutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);
        centerPaint.setColor(ContextCompat.getColor(context, R.color.local_cash_surface));
        textPaint.setColor(ContextCompat.getColor(context, R.color.local_cash_teal));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    public void setValues(float food, float transport, float studies, float personal) {
        values[0] = Math.max(0, food);
        values[1] = Math.max(0, transport);
        values[2] = Math.max(0, studies);
        values[3] = Math.max(0, personal);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float size = Math.min(getWidth(), getHeight());
        float strokeWidth = size * 0.18f;
        float padding = strokeWidth / 2f + dp(2);
        arcPaint.setStrokeWidth(strokeWidth);
        arcBounds.set(
                (getWidth() - size) / 2f + padding,
                (getHeight() - size) / 2f + padding,
                (getWidth() + size) / 2f - padding,
                (getHeight() + size) / 2f - padding
        );

        arcPaint.setColor(ContextCompat.getColor(getContext(), R.color.local_cash_progress_track));
        canvas.drawArc(arcBounds, -90f, 360f, false, arcPaint);

        float total = 0;
        for (float value : values) total += value;
        if (total > 0) {
            float startAngle = -90f;
            for (int index = 0; index < values.length; index++) {
                startAngle = drawSection(canvas, startAngle,
                        360f * values[index] / total, colors[index]);
            }
        }

        float centerRadius = size * 0.23f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, centerRadius, centerPaint);
        textPaint.setTextSize(size * 0.20f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = getHeight() / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText("$", getWidth() / 2f, baseline, textPaint);
    }

    private float drawSection(Canvas canvas, float startAngle, float sweepAngle, int colorResource) {
        arcPaint.setColor(ContextCompat.getColor(getContext(), colorResource));
        canvas.drawArc(arcBounds, startAngle, sweepAngle, false, arcPaint);
        return startAngle + sweepAngle;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
