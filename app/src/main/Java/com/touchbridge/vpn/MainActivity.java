package com.touchbridge.vpn;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;

import java.util.List;

public class MainActivity extends Activity {
    private Prefs prefs;
    private TextView status;
    private PointOverlay overlay;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = new Prefs(this);
        buildUi();
    }

    private TextView tv(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(Color.WHITE);
        t.setPadding(16, 12, 16, 12);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s); b.setAllCaps(false);
        return b;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(20,20,24));

        TextView title = tv("TouchBridge VPN", 26);
        root.addView(title);

        TextView help = tv(
            "Configure dois pontos na tela.\n\n" +
            "PONTO A = botão que deve receber o toque (ex.: botão VPN).\n" +
            "PONTO B = local onde você vai pressionar e segurar.\n\n" +
            "Ao segurar B, o app toca A uma vez. Ao soltar B, toca A novamente.",
            16);
        root.addView(help);

        Button a = button("📍 Definir PONTO A — botão VPN");
        a.setOnClickListener(v -> choosePoint(true));
        root.addView(a);

        Button c = button("📍 Definir PONTO B — área de pressão");
        c.setOnClickListener(v -> choosePoint(false));
        root.addView(c);

        Button acc = button("⚙ Ativar / configurar Acessibilidade");
        acc.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(acc);

        Button overlayBtn = button("▶ Ativar modo PRESSÃO");
        overlayBtn.setOnClickListener(v -> {
            TouchAccessibilityService svc = TouchAccessibilityService.getInstance();
            if (svc == null) {
                Toast.makeText(this, "Ative o serviço TouchBridge em Acessibilidade primeiro.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            } else {
                svc.showController();
            }
        });
        root.addView(overlayBtn);

        Button stop = button("■ Parar modo PRESSÃO");
        stop.setOnClickListener(v -> {
            TouchAccessibilityService svc = TouchAccessibilityService.getInstance();
            if (svc != null) svc.hideController();
        });
        root.addView(stop);

        Button clear = button("🗑 Limpar pontos");
        clear.setOnClickListener(v -> {
            prefs.setPointA(-1,-1); prefs.setPointB(-1,-1);
            if (overlay != null) overlay.hide();
            updateStatus();
        });
        root.addView(clear);

        status = tv("", 15);
        status.setPadding(16, 20, 16, 20);
        root.addView(status);

        setContentView(root);
        updateStatus();
    }

    private void updateStatus() {
        String a = prefs.ax() >= 0 ? String.format("A: %.0f, %.0f", prefs.ax(), prefs.ay()) : "A: não definido";
        String b = prefs.bx() >= 0 ? String.format("B: %.0f, %.0f", prefs.bx(), prefs.by()) : "B: não definido";
        status.setText(a + "\n" + b + "\n\n" +
                "Acessibilidade: " + (isAccessibilityEnabled() ? "ATIVA" : "DESATIVADA"));
    }

    private boolean isAccessibilityEnabled() {
        try {
            AccessibilityManager am = (AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> list = am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            for (AccessibilityServiceInfo i : list)
                if (i.getResolveInfo().serviceInfo.name.equals(TouchAccessibilityService.class.getName()))
                    return true;
        } catch(Exception ignored) {}
        return false;
    }

    private void choosePoint(boolean pointA) {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            Toast.makeText(this, "Permita 'aparecer sobre outros apps' e tente novamente.", Toast.LENGTH_LONG).show();
            return;
        }
        if (overlay != null) overlay.hide();
        overlay = new PointOverlay(this, pointA, (x,y) -> {
            if (pointA) prefs.setPointA(x,y); else prefs.setPointB(x,y);
            Toast.makeText(this, "Ponto salvo: " + Math.round(x) + ", " + Math.round(y), Toast.LENGTH_SHORT).show();
            updateStatus();
        });
        overlay.showPicker();
    }

    private void toggleOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        if (overlay == null) overlay = new PointOverlay(this, false, null);
        if (overlay.isShown()) overlay.hide(); else overlay.showMarkers();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }
}
