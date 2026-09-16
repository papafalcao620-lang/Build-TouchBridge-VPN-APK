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

    private int bOriginalX, bOriginalY;

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

        int bSize = prefs.circleSize();
        int aSize = 56;

        bOriginalX = (int)prefs.bx() - bSize/2;
        bOriginalY = (int)prefs.by() - bSize/2;

        pointAView = marker("A", Color.rgb(40,150,70), false);
        pointBView = marker("B\nSEGURE", Color.rgb(210,90,35), true);

        add(pointAView, (int)prefs.ax()-aSize/2, (int)prefs.ay()-aSize/2, false, aSize);
        add(pointBView, bOriginalX, bOriginalY, true, bSize);
        controllerVisible = true;

        Toast.makeText(this, "Ativo: segure e arraste B; solte para clicar A.", Toast.LENGTH_SHORT).show();
    }

    private TextView marker(String text, int color, boolean touchable) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.WHITE);
        v.setTextSize(11);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(color);

        if (touchable) {
            v.setOnTouchListener(new View.OnTouchListener() {
                float sx, sy;
                int ox, oy;

                @Override public boolean onTouch(View view, MotionEvent e) {
                    switch (e.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            sx = e.getRawX();
                            sy = e.getRawY();
                            WindowManager.LayoutParams p = (WindowManager.LayoutParams) view.getLayoutParams();
                            ox = p.x;
                            oy = p.y;
                            if (!held) {
                                held = true;
                                tapA();
                            }
                            return true;
                        }
                        case MotionEvent.ACTION_MOVE: {
                            WindowManager.LayoutParams p = (WindowManager.LayoutParams) view.getLayoutParams();
                            p.x = ox + (int)(e.getRawX() - sx);
                            p.y = oy + (int)(e.getRawY() - sy);
                            try { wm.updateViewLayout(view, p); } catch (Exception ignored) {}
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL: {
                            if (held) {
                                held = false;
                                tapA();
                            }
                            WindowManager.LayoutParams p = (WindowManager.LayoutParams) view.getLayoutParams();
                            p.x = bOriginalX;
                            p.y = bOriginalY;
                            try { wm.updateViewLayout(view, p); } catch (Exception ignored) {}
                            return true;
                        }
                    }
                    return true;
                }
            });
        }
        return v;
    }

    private void add(View v, int x, int y, boolean touch, int size) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (!touch) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            size, size,
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
