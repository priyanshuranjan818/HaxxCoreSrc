package com.zenin;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdvancedSecurity {
    private static final String TAG = "ZENIN_SECURITY";
    private final Context context;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public AdvancedSecurity(Context context) {
        this.context = context;
    }
    
    // 🚀 Start security monitoring
    public void startSecurityMonitoring() {
        Log.e(TAG, "Initializing root detection system...");
        
        // Initial check after 3 seconds
        scheduler.schedule(this::runRootCheck, 3, TimeUnit.SECONDS);
        
        // Continuous monitoring every 15 seconds
        scheduler.scheduleAtFixedRate(this::runRootCheck, 15, 15, TimeUnit.SECONDS);
    }
    
    // 🔐 Only root detection
    private void runRootCheck() {
        Log.e(TAG, "🔍 Checking for root access...");
        
        if (isDeviceRooted()) {
            Log.e(TAG, "💀 ROOT DETECTED! CRASHING APP!");
            forceCrash();
        } else {
            Log.e(TAG, "✅ No root detected - Device is clean");
        }
    }
    
    // 🛡️ Detect root via file paths
    private boolean isDeviceRooted() {
        String[] paths = {
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su",
            "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su",
            "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
            "/magisk", "/data/magisk", "/cache/magisk", "/system/bin/busybox",
            "/system/xbin/busybox", "/data/local/busybox", "/data/local/xbin/busybox"
        };
        
        for (String path : paths) {
            if (new File(path).exists()) {
                Log.e(TAG, "Root detected: " + path);
                return true;
            }
        }
        
        // Check for test keys in build tags
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            Log.e(TAG, "Root detected: test-keys in build tags");
            return true;
        }
        
        // Check for Magisk specific files
        String[] magiskPaths = {
            "/data/adb/magisk", "/cache/magisk", "/data/magisk", 
            "/data/adb/magisk.db", "/data/adb/magisk_simple"
        };
        
        for (String path : magiskPaths) {
            if (new File(path).exists()) {
                Log.e(TAG, "Magisk detected: " + path);
                return true;
            }
        }
        
        return false;
    }
    
    // 💥 Force crash - Simple and effective
    private void forceCrash() {
        Log.e(TAG, "💀 FORCING CRASH DUE TO ROOT DETECTION!");
        
        // Method 1: Null pointer exception (Guaranteed crash)
        String nullString = null;
        nullString.length();
        
        // Method 2: System exit (Backup)
        System.exit(1);
    }
    
    // 🛑 Stop security monitoring
    public void stopSecurityMonitoring() {
        try {
            scheduler.shutdown();
            Log.e(TAG, "Root detection monitoring stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping monitoring: " + e.getMessage());
        }
    }
}