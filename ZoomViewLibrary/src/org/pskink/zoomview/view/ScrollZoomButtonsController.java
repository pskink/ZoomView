package org.pskink.zoomview.view;

import java.text.NumberFormat;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.PaintDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ZoomButtonsController;

public class ScrollZoomButtonsController extends ZoomButtonsController {
    private final static String TAG = "ScrollZoomButtonsController";

    private TextView mZoomLabel;
    private NumberFormat mZoomFormat;

    public ScrollZoomButtonsController(ZoomView ownerView) {
        super(ownerView);

        Context context = ownerView.getContext();
        mZoomLabel = new TextView(context);
        mZoomFormat = NumberFormat.getPercentInstance();

        setAutoDismissed(true);
        setOnZoomListener(ownerView);
        setZoomSpeed(25);
        setZoomInEnabled(true);
        setZoomOutEnabled(false);

        ViewGroup container = getContainer();
        View controls = getZoomControls();
        LayoutParams p0 = controls.getLayoutParams();
        container.removeView(controls);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        mZoomLabel.setPadding(12, 0, 12, 0);
        mZoomLabel.setTypeface(Typeface.DEFAULT_BOLD);
        mZoomLabel.setTextColor(0xff000000);
        PaintDrawable d = new PaintDrawable(0xeeffffff);
        d.setCornerRadius(6);
        mZoomLabel.setBackgroundDrawable(d);
        mZoomLabel.setTextSize(20);
        mZoomLabel.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        p1.gravity = Gravity.CENTER_HORIZONTAL;
        layout.addView(mZoomLabel, p1);
        layout.addView(controls);
        container.addView(layout, p0);
    }

    public void setText(String label, float scale) {
        mZoomLabel.setText(label + mZoomFormat.format(scale));
    }
}
