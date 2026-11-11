package org.blasllavero.wifilert;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.text.DateFormat;
import java.util.Date;

/** User-started, locally evaluated monitoring. No analytics, ads, account, or cloud storage. */
public final class ConnectionService extends Service {
    static final String PREFS = "wifilert_status";
    private static final long SETTLE_MS = 1600;
    private SharedPreferences prefs;
    private ConnectivityManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RailwayDetector railDetector;
    private boolean registered;
    private int internetStable = -1, wifiStable = -1, wifiInternetStable = -1;
    private boolean trainDetected;
    private int mode;
    private NetworkState pending;
    private final Runnable applyPending = () -> {
        if (pending != null) apply(pending);
    };
    private final Runnable queryNetworks = () -> {
        NetworkState current = sample();
        if (pending == null || !pending.equals(current)) {
            pending = current;
            handler.removeCallbacks(applyPending);
            handler.postDelayed(applyPending, SETTLE_MS);
        }
    };
    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { schedule(); }
        @Override public void onLost(Network network) { schedule(); }
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) { schedule(); }
        @Override public void onUnavailable() { schedule(); }
    };
    private final ConnectivityManager.NetworkCallback wifiCallback = new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { schedule(); }
        @Override public void onLost(Network network) { schedule(); }
        @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) { schedule(); }
    };

    private void schedule() {
        handler.removeCallbacks(queryNetworks);
        handler.postDelayed(queryNetworks, 250);
    }

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        mode = prefs.getInt("mode", 0);
        Notifier.setupChannels(this);
        boolean locationServiceActive = mode == 2
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                int types = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
                if (locationServiceActive) types |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
                startForeground(Notifier.MONITOR_ID, Notifier.monitorNotification(this), types);
            } else if (Build.VERSION.SDK_INT >= 29 && locationServiceActive) {
                startForeground(Notifier.MONITOR_ID, Notifier.monitorNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(Notifier.MONITOR_ID, Notifier.monitorNotification(this));
            }
        } catch (SecurityException ex) {
            // On recovery in background, Android may withhold while-in-use location access.
            // Keep internet monitoring alive, but do not pretend that train auto-detection works.
            locationServiceActive = false;
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(Notifier.MONITOR_ID, Notifier.monitorNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else startForeground(Notifier.MONITOR_ID, Notifier.monitorNotification(this));
        }
        prefs.edit().putBoolean("running", true).putInt("internetState", -1)
                .putInt("wifiState", -1).putInt("trainState", mode == 1 ? 1 : -1).apply();
        trainDetected = mode == 1;
        if (!registered) {
            try {
                manager.registerDefaultNetworkCallback(callback);
                NetworkRequest request = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
                manager.registerNetworkCallback(request, wifiCallback);
                registered = true;
            } catch (RuntimeException err) { log(getString(R.string.permission_needed)); }
        }
        if (railDetector == null && mode == 2 && locationServiceActive) {
            railDetector = new RailwayDetector(this, detected -> handler.post(() -> {
                if (trainDetected != detected) {
                    trainDetected = detected;
                    log(getString(detected ? R.string.event_train_detected : R.string.event_train_ended));
                }
                prefs.edit().putInt("trainState", detected ? 1 : 0).apply();
            }));
            railDetector.start();
        }
        handler.post(queryNetworks);
        return START_STICKY;
    }

    private static final class NetworkState {
        final int internet, wifi, wifiInternet;
        NetworkState(int internet, int wifi, int wifiInternet) {
            this.internet = internet; this.wifi = wifi; this.wifiInternet = wifiInternet;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof NetworkState)) return false;
            NetworkState n = (NetworkState)o;
            return n.internet == internet && n.wifi == wifi && n.wifiInternet == wifiInternet;
        }
        @Override public int hashCode() { return 17 * internet + 31 * wifi + wifiInternet; }
    }

    private NetworkState sample() {
        int internet = 0, wifi = 0, wifiValidated = 0;
        try {
            Network active = manager.getActiveNetwork();
            NetworkCapabilities cap = active == null ? null : manager.getNetworkCapabilities(active);
            if (cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) internet = 1;
            for (Network network : manager.getAllNetworks()) {
                NetworkCapabilities c = manager.getNetworkCapabilities(network);
                if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifi = 1;
                    if (c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) wifiValidated = 1;
                }
            }
        } catch (RuntimeException ignored) {
            // A transient stale network can disappear during enumeration.
        }
        return new NetworkState(internet, wifi, wifiValidated);
    }

    private void apply(NetworkState state) {
        if (internetStable >= 0 && internetStable != state.internet && shouldAlert()
                && prefs.getBoolean("alertInternet", true)) {
            int msg = state.internet == 1 ? R.string.notif_internet_on_title : R.string.notif_internet_off_title;
            Notifier.sendAlert(this, msg, state.internet == 1 ? R.string.notif_body_on : R.string.notif_body_off);
            log(getString(msg));
        }
        if (wifiStable >= 0 && wifiStable != state.wifi && shouldAlert()
                && prefs.getBoolean("alertWifi", true)) {
            int title = state.wifi == 1 ? R.string.notif_wifi_back_title : R.string.notif_wifi_lost_title;
            Notifier.sendAlert(this, title, state.wifi == 1 ? R.string.notif_wifi_restored : R.string.notif_body_off);
            log(getString(title));
        }
        // Wi-Fi still associated but its own internet validation changed.
        if (state.wifi == 1 && wifiStable == 1 && wifiInternetStable >= 0
                && wifiInternetStable != state.wifiInternet && shouldAlert()
                && prefs.getBoolean("alertWifi", true)) {
            int title = state.wifiInternet == 1 ? R.string.notif_wifi_internet_back_title
                    : R.string.notif_wifi_no_internet_title;
            Notifier.sendAlert(this, title, state.wifiInternet == 1
                    ? R.string.notif_wifi_restored : R.string.notif_wifi_no_internet);
            log(getString(title));
        }
        internetStable = state.internet;
        wifiStable = state.wifi;
        wifiInternetStable = state.wifiInternet;
        prefs.edit().putInt("internetState", state.internet).putInt("wifiState", state.wifi).apply();
    }

    private boolean shouldAlert() { return mode != 2 || trainDetected; }
    private void log(String event) {
        String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new Date());
        prefs.edit().putString("lastEvent", date + " — " + event).apply();
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (railDetector != null) { railDetector.stop(); railDetector = null; }
        if (registered) {
            try { manager.unregisterNetworkCallback(callback); } catch (Exception ignored) { }
            try { manager.unregisterNetworkCallback(wifiCallback); } catch (Exception ignored) { }
            registered = false;
        }
        prefs.edit().putBoolean("running", false).putInt("trainState", -1).apply();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
