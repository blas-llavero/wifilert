package org.blasllavero.wifilert;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Experimental railway proximity detection, without proprietary location/ML libraries.
 * GPS motion + nearby OSM railway geometry. Not suitable for safety-critical uses.
 * Only the Overpass request shares approximate position; no coordinates are persisted.
 */
final class RailwayDetector implements LocationListener {
    interface Listener { void onTrainChanged(boolean detected); }
    private final Context context;
    private final LocationManager locations;
    private final Listener listener;
    private final ExecutorService fetcher = Executors.newSingleThreadExecutor();
    private final Object lock = new Object();
    private List<double[]> segments = new ArrayList<>();
    private volatile boolean stopped;
    private volatile boolean fetching;
    private volatile long lastFetchMillis;
    private double fetchedLat = Double.NaN, fetchedLon = Double.NaN;
    private long lastTrainEvidence;
    private boolean detected;
    private int consecutiveNearRail;
    private Location previous;
    private long lastConfirmedMillis;

    RailwayDetector(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }
    void start() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try { locations.requestLocationUpdates(LocationManager.GPS_PROVIDER, 8000, 35, this); }
        catch (Exception ignored) { }
    }
    void stop() {
        stopped = true;
        try { locations.removeUpdates(this); } catch (Exception ignored) { }
        fetcher.shutdownNow();
    }
    @Override public void onLocationChanged(Location location) {
        if (stopped || location == null || (location.hasAccuracy() && location.getAccuracy() > 120)) return;
        double speed = location.hasSpeed() ? location.getSpeed() : calculateSpeed(location, previous);
        previous = new Location(location);
        maybeDownload(location);
        boolean plausibleTrainSpeed = speed > 7.0 && speed < 120.0;
        boolean nearRail = isNearRail(location.getLatitude(), location.getLongitude(), 160);
        long now = SystemClock.elapsedRealtime();
        if (plausibleTrainSpeed && nearRail) {
            if (now - lastConfirmedMillis > 9000) {
                consecutiveNearRail++;
                lastConfirmedMillis = now;
            }
            if (consecutiveNearRail >= 2) {
                lastTrainEvidence = now;
                if (!detected) { detected = true; listener.onTrainChanged(true); }
            }
        } else {
            if (!nearRail) consecutiveNearRail = 0;
            if (detected && now - lastTrainEvidence > 12 * 60_000L) {
                detected = false; consecutiveNearRail = 0;
                listener.onTrainChanged(false);
            }
        }
    }
    private static double calculateSpeed(Location now, Location old) {
        if (old == null) return 0;
        long dt = now.getElapsedRealtimeNanos() - old.getElapsedRealtimeNanos();
        return dt <= 0 ? 0 : now.distanceTo(old) / (dt / 1_000_000_000.0);
    }
    private boolean isNearRail(double lat, double lon, double maxMetres) {
        synchronized (lock) {
            for (double[] line : segments) {
                if (metresToSegment(lat, lon, line) < maxMetres) return true;
            }
        }
        return false;
    }
    // Equirectangular projection near the device, suitable for short local rail segments.
    static double metresToSegment(double lat, double lon, double[] s) {
        double scale = Math.cos(Math.toRadians(lat)) * 111_320.0;
        double x1 = (s[1] - lon) * scale, y1 = (s[0] - lat) * 111_320.0;
        double x2 = (s[3] - lon) * scale, y2 = (s[2] - lat) * 111_320.0;
        double dx = x2 - x1, dy = y2 - y1;
        double t = dx * dx + dy * dy == 0 ? 0 : Math.max(0,
                Math.min(1, -(x1 * dx + y1 * dy) / (dx * dx + dy * dy)));
        return Math.hypot(x1 + t * dx, y1 + t * dy);
    }
    private void maybeDownload(Location loc) {
        long now = SystemClock.elapsedRealtime();
        if (fetching || (now - lastFetchMillis < 180_000L)) return;
        boolean moved = Double.isNaN(fetchedLat)
                || distanceMetres(fetchedLat, fetchedLon, loc.getLatitude(), loc.getLongitude()) > 3000;
        if (!moved && now - lastFetchMillis < 600_000L) return;
        fetching = true; lastFetchMillis = now;
        final double lat = Math.round(loc.getLatitude() * 10000.0) / 10000.0;
        final double lon = Math.round(loc.getLongitude() * 10000.0) / 10000.0;
        fetcher.execute(() -> {
            try {
                List<double[]> downloaded = retrieveRailLines(lat, lon);
                if (!stopped && !downloaded.isEmpty()) {
                    synchronized (lock) { segments = downloaded; fetchedLat = lat; fetchedLon = lon; }
                }
            } catch (Exception ignored) { /* no network, quota, tunnel, or malformed map */ }
            finally { fetching = false; }
        });
    }
    private static double distanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double y = (lat2 - lat1) * 111_320.0;
        double x = (lon2 - lon1) * 111_320.0 * Math.cos(Math.toRadians(lat1));
        return Math.hypot(x, y);
    }
    private static List<double[]> retrieveRailLines(double lat, double lon) throws Exception {
        String query = "[out:json][timeout:12];way[\"railway\"~\"^(rail|light_rail|subway)$\"]"
                + "(around:3500," + lat + "," + lon + ");out geom;";
        URL url = new URL("https://overpass-api.de/api/interpreter");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST"); conn.setConnectTimeout(9000); conn.setReadTimeout(15000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Wifilert/0.1 open-source Android connectivity monitor");
        try {
            byte[] body = ("data=" + java.net.URLEncoder.encode(query, "UTF-8"))
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = conn.getOutputStream()) { out.write(body); }
            if (conn.getResponseCode() != 200) throw new IllegalStateException("Overpass unavailable");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = conn.getInputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = input.read(buffer)) >= 0) {
                    bytes.write(buffer, 0, n);
                    if (bytes.size() > 3_000_000) throw new IllegalStateException("Rail map too large");
                }
            }
            JSONObject json = new JSONObject(bytes.toString("UTF-8"));
            JSONArray elements = json.optJSONArray("elements");
            List<double[]> result = new ArrayList<>();
            if (elements != null) for (int i = 0; i < elements.length(); i++) {
                JSONArray geom = elements.getJSONObject(i).optJSONArray("geometry");
                if (geom == null) continue;
                for (int j = 1; j < geom.length(); j++) {
                    JSONObject a = geom.getJSONObject(j - 1), b = geom.getJSONObject(j);
                    result.add(new double[]{a.getDouble("lat"), a.getDouble("lon"),
                            b.getDouble("lat"), b.getDouble("lon")});
                }
            }
            return result;
        } finally { conn.disconnect(); }
    }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }
}
