package com.zenin;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import com.zenin.utils.FPrefs;
import com.zenin.utils.FeatureFlags;
import java.io.File;

import com.haxxcore.engine.HaxxCore;
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback;
import top.niunaijun.blackbox.app.configuration.ClientConfiguration;
import top.niunaijun.blackbox.core.env.BEnvironment;

public class BoxApplication extends Application {
    private static final String TAG = "ZeninLoader";
    private static final String BGMI_PACKAGE = "com.pubg.imobile";

    static {
        System.loadLibrary("zenin"); 
    }
    public static native String getSdkKey();
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        FPrefs prefs = FPrefs.with(base);
        try {
            HaxxCore.get().doAttachBaseContext(base, new ClientConfiguration() {

                @Override
                public String getHostPackageName() {
                    return base.getPackageName();
                }

                @Override
                public boolean isHideRoot() {
                    return true;
                }

                public boolean isHideXposed() {
                    return true;
                }

                @Override
                public boolean isEnableDaemonService() {
                    return false;
                }

                @Override
                public boolean requestInstallPackage(File file, int userId) {
                    PackageInfo packageInfo = base.getPackageManager()
                            .getPackageArchiveInfo(file.getAbsolutePath(), 0);
                    return false;
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HaxxCore.get().doCreate();
        
        // ZCore SDK activation removed for HaxxCore (Offline/No-Key required)
        // BoxActivate.activateBox(getSdkKey()); 

        HaxxCore.get().addAppLifecycleCallback(new AppLifecycleCallback() {

            @Override
            public void beforeCreateApplication(String packageName, String processName, Context context, int userId) {
                // Fix 1: Inject libbgmi.so into the virtual BGMI process
                if (FeatureFlags.ENABLE_LIB_INJECTION) {
                    try {
                        File loaderLib = new File(getFilesDir(), "loader/libbgmi.so");
                        if (loaderLib.exists()) {
                            System.load(loaderLib.getAbsolutePath());
                            Log.d(TAG, "libbgmi.so injected successfully into " + packageName);
                        } else {
                            Log.w(TAG, "libbgmi.so not found at: " + loaderLib.getAbsolutePath());
                        }
                    } catch (UnsatisfiedLinkError e) {
                        Log.e(TAG, "Failed to load libbgmi.so: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Error injecting lib: " + e.getMessage());
                    }
                } else {
                    Log.d(TAG, "Lib injection disabled for testing");
                }

                // Fix 2: Pre-create virtual data directories so BGMI doesn't re-download
                if (BGMI_PACKAGE.equals(packageName)) {
                    ensureVirtualDataDirs(packageName, userId);
                }
            }

            @Override
            public void beforeApplicationOnCreate(String packageName, String processName, Application application, int userId) {
            }

            @Override
            public void afterApplicationOnCreate(String packageName, String processName, Application application, int userId) {
            }

        });
    }

    /**
     * Pre-creates all virtual data directories for the package so the game
     * finds its expected storage paths and doesn't re-download resources.
     */
    private void ensureVirtualDataDirs(String packageName, int userId) {
        try {
            File externalDataDir = BEnvironment.getExternalDataDir(packageName, userId);
            if (!externalDataDir.exists()) externalDataDir.mkdirs();

            File externalFilesDir = BEnvironment.getExternalDataFilesDir(packageName, userId);
            if (!externalFilesDir.exists()) externalFilesDir.mkdirs();

            File externalCacheDir = BEnvironment.getExternalDataCacheDir(packageName, userId);
            if (!externalCacheDir.exists()) externalCacheDir.mkdirs();

            File obbDir = BEnvironment.getExternalObbDir(packageName, userId);
            if (!obbDir.exists()) obbDir.mkdirs();

            File dataFilesDir = BEnvironment.getDataFilesDir(packageName, userId);
            if (!dataFilesDir.exists()) dataFilesDir.mkdirs();

            File dataCacheDir = BEnvironment.getDataCacheDir(packageName, userId);
            if (!dataCacheDir.exists()) dataCacheDir.mkdirs();

            Log.d(TAG, "Virtual data directories created for " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error creating virtual data dirs: " + e.getMessage());
        }
    }

}
