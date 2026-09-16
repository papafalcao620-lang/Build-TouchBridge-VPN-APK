package com.touchbridge.vpn;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import android.view.accessibility.AccessibilityEvent;

public class TouchAccessibilityService extends AccessibilityService {
    private static TouchAccessibilityService instance;
    private Prefs prefs;
    private WindowManager wm;
    private View pointAView, pointBView;
    private boolean controllerVisible = false;
    private boolean held = false;

    public static TouchAccessibilityService getInstance() { return instance; }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = new Prefs(this);
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }

    public void showController() {
        if (!prefs.ready()) {
            Toast.makeText(this, "Defina os dois pontos primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        hideController();

        pointAView = marker("A", Color.rgb(40,150,70), false);
        pointBView = marker("B\nSEGURE", Color.rgb(210,90,35), true);

        add(pointAView, (int)prefs.ax()-28, (int)prefs.ay()-28, false);
        add(pointBView, (int)prefs.bx()-45, (int)prefs.by()-45, true);
        controllerVisible = true;

        Toast.makeText(this, "Ativo: segure B para ligar/desligar pelo ponto A.", Toast.LENGTH_SHORT).show();
    }

    private TextView marker(String text, int color, boolean touchable) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(11);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(color);

        if (touchable) {
            v.setOnTouchListener((view, e) -> {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!held) {
                        held = true;
                        tapA();
                    }
                    return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP ||
                    e.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (held) {
                        held = false;
                        tapA();
                    }
                    return true;
                }
                return true;
            });
        }
        return v;
    }

    private void add(View v, int x, int y, boolean touch) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (!touch) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            touch ? 90 : 56, touch ? 90 : 56,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = Math.max(0, x);
        p.y = Math.max(0, y);
        wm.addView(v, p);
    }

    private void tapA() {
        if (prefs == null) prefs = new Prefs(this);
        if (!prefs.ready()) return;

        Path path = new Path();
        path.moveTo(prefs.ax(), prefs.ay());
        long duration = Math.max(1, prefs.tapDuration());

        GestureDescription.StrokeDescription stroke =
            new GestureDescription.StrokeDescription(path, 0, duration);

        dispatchGesture(
            new GestureDescription.Builder().addStroke(stroke).build(),
            null, null);
    }

    public void hideController() {
        try { if (pointAView != null) wm.removeView(pointAView); } catch (Exception ignored) {}
        try { if (pointBView != null) wm.removeView(pointBView); } catch (Exception ignored) {}
        pointAView = null;
        pointBView = null;
        controllerVisible = false;
        held = false;
    }

    public boolean isControllerVisible() { return controllerVisible; }

    @Override public void onDestroy() {
        hideController();
        if (instance == this) instance = null;
        super.onDestroy();
    }
}
