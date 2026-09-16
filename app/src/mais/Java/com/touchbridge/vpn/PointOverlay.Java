package com.touchbridge.vpn;

import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class PointOverlay {
    public interface Callback { void saved(float x, float y); }

    private final Context c;
    private final WindowManager wm;
    private final Prefs prefs;
    private boolean shown;
    private boolean picker;
    private boolean pointA;
    private Callback callback;
    private View markerA, markerB, pickerView;

    public PointOverlay(Context c, boolean pointA, Callback cb) {
        this.c = c; this.pointA = pointA; this.callback = cb;
        wm = (WindowManager)c.getSystemService(Context.WINDOW_SERVICE);
        prefs = new Prefs(c);
    }

    private WindowManager.LayoutParams lp(int w, int h, int x, int y, boolean touch) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (!touch) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w,h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x=x; p.y=y;
        return p;
    }

    private TextView marker(String text, int color) {
        TextView v = new TextView(c);
        v.setText(text); v.setTextColor(Color.WHITE); v.setTextSize(12);
        v.setGravity(Gravity.CENTER);
        v.setBackgroundColor(color);
        return v;
    }

    public void showPicker() {
        hide();
        picker = true;
        TextView v = marker(pointA ? "A" : "B", Color.rgb(30,120,220));
        v.setText("MOVA-ME");
        v.setTextSize(14);
        v.setOnTouchListener(new View.OnTouchListener() {
            float sx, sy; int ox, oy;
            public boolean onTouch(View view, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN) {
                    sx=e.getRawX(); sy=e.getRawY();
                    ox=((WindowManager.LayoutParams)view.getLayoutParams()).x;
                    oy=((WindowManager.LayoutParams)view.getLayoutParams()).y;
                    return true;
                }
                if (e.getAction()==MotionEvent.ACTION_MOVE) {
                    WindowManager.LayoutParams p=(WindowManager.LayoutParams)view.getLayoutParams();
                    p.x=ox+(int)(e.getRawX()-sx); p.y=oy+(int)(e.getRawY()-sy);
                    wm.updateViewLayout(view,p); return true;
                }
                if (e.getAction()==MotionEvent.ACTION_UP) {
                    float x=e.getRawX(), y=e.getRawY();
                    hide();
                    if(callback!=null) callback.saved(x,y);
                    return true;
                }
                return true;
            }
        });
        pickerView=v;
        wm.addView(v, lp(120,80,200,400,true));
        shown=true;
        Toast.makeText(c, "Arraste o marcador e solte exatamente no local desejado.", Toast.LENGTH_LONG).show();
    }

    public void showMarkers() {
        hide();
        if (!prefs.ready()) {
            Toast.makeText(c, "Defina os dois pontos primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        markerA=marker("A", Color.rgb(30,150,70));
        markerB=marker("B", Color.rgb(210,100,30));
        wm.addView(markerA, lp(56,56,(int)prefs.ax()-28,(int)prefs.ay()-28,false));
        wm.addView(markerB, lp(56,56,(int)prefs.bx()-28,(int)prefs.by()-28,false));
        shown=true;
    }

    public boolean isShown(){return shown;}

    public void hide() {
        try { if(markerA!=null) wm.removeView(markerA); } catch(Exception ignored){}
        try { if(markerB!=null) wm.removeView(markerB); } catch(Exception ignored){}
        try { if(pickerView!=null) wm.removeView(pickerView); } catch(Exception ignored){}
        markerA=markerB=pickerView=null; shown=false;
    }
}
