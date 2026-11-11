package org.blasllavero.wifilert;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int REQ_NOTIFICATION = 101;
    private static final int REQ_LOCATION = 102;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private Spinner mode;
    private CheckBox wifiAlerts, internetAlerts;
    private Button monitorButton;
    private TextView monitorStatus, internetStatus, wifiStatus, trainStatus, lastEvent;
    private boolean pendingStart;
    private final Runnable refreshTask = new Runnable() {
        @Override public void run() { refresh(); ui.postDelayed(this, 1500); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        // Android 15+ draws beneath the navigation/status bars. Add those insets to the scroll viewport.
        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets b = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = b.left; top = b.top; right = b.right; bottom = b.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft(); top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight(); bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });
        root.requestApplyInsets();
        prefs = getSharedPreferences(ConnectionService.PREFS, MODE_PRIVATE);
        mode = findViewById(R.id.modeSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.modes,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setAdapter(adapter);
        mode.setSelection(prefs.getInt("mode", 0));
        wifiAlerts = findViewById(R.id.wifiAlerts);
        internetAlerts = findViewById(R.id.internetAlerts);
        wifiAlerts.setChecked(prefs.getBoolean("alertWifi", true));
        internetAlerts.setChecked(prefs.getBoolean("alertInternet", true));
        monitorButton = findViewById(R.id.monitorButton);
        monitorStatus = findViewById(R.id.monitorStatus);
        internetStatus = findViewById(R.id.internetStatus);
        wifiStatus = findViewById(R.id.wifiStatus);
        trainStatus = findViewById(R.id.trainStatus);
        lastEvent = findViewById(R.id.lastEvent);
        monitorButton.setOnClickListener(v -> {
            if (prefs.getBoolean("running", false)) stopMonitoring(); else startMonitoring();
        });
        findViewById(R.id.testButton).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
            } else {
                Notifier.sendTest(this);
                Toast.makeText(this, R.string.notif_test_title, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override protected void onResume() {
        super.onResume(); ui.removeCallbacks(refreshTask); ui.post(refreshTask);
    }
    @Override protected void onPause() {
        ui.removeCallbacks(refreshTask); super.onPause();
    }

    private void startMonitoring() {
        pendingStart = true;
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION); return;
        }
        if (mode.getSelectedItemPosition() == 2 && Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION); return;
        }
        pendingStart = false;
        prefs.edit().putInt("mode", mode.getSelectedItemPosition())
                .putBoolean("alertWifi", wifiAlerts.isChecked())
                .putBoolean("alertInternet", internetAlerts.isChecked()).apply();
        try {
            Intent intent = new Intent(this, ConnectionService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            // UI is refreshed by the foreground service after it starts.
            ui.postDelayed(this::refresh, 250);
        } catch (RuntimeException error) {
            Toast.makeText(this, getString(R.string.permission_needed) + ": " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
    private void stopMonitoring() {
        pendingStart = false;
        stopService(new Intent(this, ConnectionService.class));
        prefs.edit().putBoolean("running", false).apply();
        refresh();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
        if (pendingStart && (requestCode == REQ_NOTIFICATION || requestCode == REQ_LOCATION)) {
            if (granted) startMonitoring();
            else { pendingStart = false; Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show(); }
        } else if (requestCode == REQ_NOTIFICATION) {
            if (granted) {
                Notifier.sendTest(this);
                Toast.makeText(this, R.string.notif_test_title, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.permission_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refresh() {
        if (prefs == null) return;
        boolean running = prefs.getBoolean("running", false);
        monitorButton.setText(running ? R.string.label_stop : R.string.label_start);
        monitorStatus.setText(running ? R.string.state_running : R.string.state_stopped);
        mode.setEnabled(!running);
        wifiAlerts.setEnabled(!running);
        internetAlerts.setEnabled(!running);
        int online = prefs.getInt("internetState", -1);
        int wifi = prefs.getInt("wifiState", -1);
        int train = prefs.getInt("trainState", -1);
        internetStatus.setText(getString(R.string.label_internet) + ": " + statusValue(online));
        wifiStatus.setText(getString(R.string.label_wifi) + ": " + statusValue(wifi));
        String trainValue = train == 1 ? getString(R.string.state_rail_yes)
                : train == 0 ? getString(R.string.state_rail_no) : getString(R.string.state_rail_wait);
        trainStatus.setText(getString(R.string.label_train) + ": " + trainValue);
        lastEvent.setText(prefs.getString("lastEvent", getString(R.string.history_empty)));
    }
    private String statusValue(int state) {
        return state < 0 ? getString(R.string.state_unknown)
                : state == 1 ? getString(R.string.state_connected) : getString(R.string.state_disconnected);
    }
}
