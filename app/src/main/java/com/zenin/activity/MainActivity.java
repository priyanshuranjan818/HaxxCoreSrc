package com.zenin.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.zenin.R;
import java.io.File;
import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.haxxcore.engine.HaxxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import com.zenin.libhelper.FileCopyTask;
import com.zenin.utils.AuthSessionBridge;

// New imports for Lottie
import com.airbnb.lottie.LottieAnimationView;
import android.view.View;

public class MainActivity extends Activity {

    private static final String BGMI_PACKAGE = "com.pubg.imobile";
    private static final int USER_ID = 0;

    private HaxxCore zCore;
    private Button starthack;
    private TextView Enc;
    private ImageView myIcon;
    private FileCopyTask fileCopyTask;
    private Button facebookLoginButton;
    private Button twitterLoginButton;
    public static native String exdate();

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Lottie Animation Code
        LottieAnimationView animationView = findViewById(R.id.animation_view);
        if (animationView != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    animationView.setVisibility(View.VISIBLE);
                    animationView.playAnimation();
                    animationView.loop(true);
                }
            }, 1000);
        }

        // Initialize HaxxCore
        zCore = HaxxCore.get();

        // Initialize UI elements
        starthack = findViewById(R.id.starthack);
        Enc = findViewById(R.id.Enc);
        myIcon = findViewById(R.id.myIcon);

        // Setup Progress Dialog / File copy helper
        fileCopyTask = new FileCopyTask(this);
        facebookLoginButton = findViewById(R.id.btn_facebook_login);
        twitterLoginButton = findViewById(R.id.btn_twitter_login);

        // Set button listeners
        if (starthack != null) {
            starthack.setOnClickListener(view -> handleStart());
            starthack.setOnLongClickListener(view -> {
                showAuthBridgeStatus();
                return true;
            });
        } else {
            Toast.makeText(this, "Start button not found!", Toast.LENGTH_SHORT).show();
        }
        if (facebookLoginButton != null) {
            facebookLoginButton.setOnClickListener(v -> openSocialAuth("facebook"));
        }
        if (twitterLoginButton != null) {
            twitterLoginButton.setOnClickListener(v -> openSocialAuth("twitter"));
        }

        countDownStart();
    }

    private void launchClonedApp(String packageName) {
        // Check if the app is already cloned inside HaxxCore
        if (!zCore.isInstalled(packageName, USER_ID)) {
            // Clone the app before launching
            Toast.makeText(this, "Cloning " + packageName + "...", Toast.LENGTH_SHORT).show();
            InstallResult installResult = zCore.installPackageAsUser(packageName, USER_ID);

            if (!installResult.success) {
                Toast.makeText(this, "Cloning failed: " + installResult.msg, Toast.LENGTH_SHORT).show();
                return;
            } else {
                Toast.makeText(this, packageName + " cloned successfully!", Toast.LENGTH_SHORT).show();
            }
        }

        // Launch the cloned app
        try {
            boolean success = zCore.launchApk(packageName, USER_ID);
            if (!success) {
                Toast.makeText(this, "Failed to launch cloned app: " + packageName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error launching cloned app: " + packageName, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyObbFilesAndLaunch() {
        fileCopyTask.copyObbFolderAsync(BGMI_PACKAGE, success -> {
            if (success) {
                Toast.makeText(this, "OBB copied successfully!", Toast.LENGTH_SHORT).show();
                launchGame();
            } else {
                Toast.makeText(this, "Failed to copy OBB!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleStart() {
        if (zCore.isInstalled(BGMI_PACKAGE, USER_ID)) {
            // If already installed, just copy OBB and launch the game
            copyObbFilesAndLaunch();
        } else {
            // If not installed, install it first
            installGame();
        }
    }

    private void installGame() {
        Toast.makeText(this, "Installing BGMI...", Toast.LENGTH_SHORT).show();
        InstallResult installResult = zCore.installPackageAsUser(BGMI_PACKAGE, USER_ID);

        if (installResult.success) {
            Toast.makeText(this, "BGMI Installed Successfully!", Toast.LENGTH_SHORT).show();
            copyObbFilesAndLaunch();
        } else {
            Toast.makeText(this, "Installation Failed: " + installResult.msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGame() {
        // Sync host OAuth callback payload into virtual app files before launch.
        boolean synced = AuthSessionBridge.syncToVirtualApp(BGMI_PACKAGE, USER_ID, this);
        if (!synced && AuthSessionBridge.hasHostSession(this)) {
            Toast.makeText(this, "Social session exists but sync failed", Toast.LENGTH_SHORT).show();
        }
        boolean success = zCore.launchApk(BGMI_PACKAGE, USER_ID);
        if (success) {
            Toast.makeText(this, "BGMI launched successfully!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to launch BGMI!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSocialAuth(String provider) {
        Intent intent = new Intent(this, AuthBridgeActivity.class);
        intent.putExtra(AuthBridgeActivity.EXTRA_PROVIDER, provider);
        startActivity(intent);
    }

    private void showAuthBridgeStatus() {
        String json = AuthSessionBridge.getHostSessionJson(this);
        if (TextUtils.isEmpty(json)) {
            Toast.makeText(this, "No social session found yet", Toast.LENGTH_LONG).show();
            return;
        }
        String shortJson = json.length() > 180 ? json.substring(0, 180) + "..." : json;
        Toast.makeText(this, "Auth session: " + shortJson, Toast.LENGTH_LONG).show();
    }

    private void countDownStart() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date expiryDate = dateFormat.parse(exdate());
                    long now = System.currentTimeMillis();
                    long distance = expiryDate.getTime() - now;
                    long days = distance / (24 * 60 * 60 * 1000);
                    long hours = distance / (60 * 60 * 1000) % 24;
                    long minutes = distance / (60 * 1000) % 60;
                    long seconds = distance / 1000 % 60;
                    if (distance < 0) {
                        // expired
                    } else {
                        TextView Hari = findViewById(R.id.tv_d);
                        TextView Jam = findViewById(R.id.tv_h);
                        TextView Menit = findViewById(R.id.tv_m);
                        TextView Detik = findViewById(R.id.tv_s);
                        if (days > 0) {
                            Hari.setText(String.format("%02d", days));
                        }
                        if (hours > 0) {
                            Jam.setText(String.format("%02d", hours));
                        }
                        if (minutes > 0) {
                            Menit.setText(String.format("%02d", minutes));
                        }
                        if (seconds > 0) {
                            Detik.setText(String.format("%02d", seconds));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
