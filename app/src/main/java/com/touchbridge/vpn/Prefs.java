package com.touchbridge.vpn;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String P = "touchbridge";
    private final SharedPreferences sp;

    public Prefs(Context c) {
        sp = c.getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public void setPointA(float x, float y) {
        sp.edit().putFloat("ax", x).putFloat("ay", y).apply();
    }
    public void setPointB(float x, float y) {
        sp.edit().putFloat("bx", x).putFloat("by", y).apply();
    }
    public float ax() { return sp.getFloat("ax", -1); }
    public float ay() { return sp.getFloat("ay", -1); }
    public float bx() { return sp.getFloat("bx", -1); }
    public float by() { return sp.getFloat("by", -1); }

    public boolean ready() {
        return ax() >= 0 && ay() >= 0 && bx() >= 0 && by() >= 0;
    }

    public void setEnabled(boolean v) { sp.edit().putBoolean("enabled", v).apply(); }
    public boolean enabled() { return sp.getBoolean("enabled", false); }

    public void setTapDuration(long v) { sp.edit().putLong("duration", v).apply(); }
    public long tapDuration() { return sp.getLong("duration", 40); }

    public void setCircleSize(int px) { sp.edit().putInt("circleSize", px).apply(); }
    public int circleSize() { return sp.getInt("circleSize", 90); }
}
