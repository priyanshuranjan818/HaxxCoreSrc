package com.zenin.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.os.Process;
import android.util.Base64;
import com.zenin.R;
import com.zenin.utils.Prefs;
import com.zenin.utils.DownC;
import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// New imports for Lottie
import com.airbnb.lottie.LottieAnimationView;
import android.view.View;

@Obfuscate
public class LoginActivity extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("zenin");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;
    private static final String PREFS_NAME = "com.zenin.prefs";
    private static final String PREF_PERMISSIONS_GRANTED = "permissions_granted";
    
    

    public static String USERKEY;
    private CardView btnSignIn;
    private Dialog loadingDialog;
    private TextView errorTextView;
    public static native String ziplink();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Lottie Animation Code - YAHAN ADD KIA HAI
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

        checkAndRequestPermissions();
        initDesign();
    }

    private void checkAndRequestPermissions() {
    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    boolean permissionsGranted = prefs.getBoolean(PREF_PERMISSIONS_GRANTED, false);

    if (!isStoragePermissionGranted()) {
        requestStoragePermissionDirect();
    } else if (!canRequestPackageInstalls()) {
        requestUnknownAppPermissionsDirect();
    } else {
        prefs.edit().putBoolean(PREF_PERMISSIONS_GRANTED, true).apply();
    }
} // <-- YEH CLOSING BRACKET ADD KARNA HAI

private boolean isStoragePermissionGranted() {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
}

private void requestStoragePermissionDirect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
    } else {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_MANAGE_STORAGE_PERMISSION);
    }
}

    private boolean canRequestPackageInstalls() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls();
    }

    private void requestUnknownAppPermissionsDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
        }
    }

    private void initDesign() {
        Prefs prefs = new Prefs(this);
        TextView textUsername = findViewById(R.id.userkey);
        textUsername.setText(prefs.getSt("USER", ""));

        btnSignIn = findViewById(R.id.login);
        btnSignIn.setOnClickListener(v -> {
            String userKey = textUsername.getText().toString().trim();
            if (!userKey.isEmpty()) {
                prefs.setSt("USER", userKey);
                USERKEY = userKey;
                login(userKey);
            } else {
                textUsername.setError(("Please Enter Your License"));
            }
        });

        ImageView paste = findViewById(R.id.paste);
        paste.setOnClickListener(view -> {
            ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboardManager != null && clipboardManager.getPrimaryClip() != null && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                String pastedText = clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                if (pastedText.length() > 5) {
                    textUsername.setText(pastedText);
                } else {
                    Toast.makeText(this, "Invalid key in clipboard.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No text in clipboard.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    

    private void showLoadingDialog(String errorMessage) {
        if (loadingDialog == null) {
            loadingDialog = new Dialog(this);
            loadingDialog.setContentView(R.layout.ios_loading);
            loadingDialog.setCancelable(false);
            loadingDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            errorTextView = loadingDialog.findViewById(R.id.errorText);
        }

        ProgressBar progressBar = loadingDialog.findViewById(R.id.progressBar);
        ImageView errorIcon = loadingDialog.findViewById(R.id.errorIcon);
        Button okButton = loadingDialog.findViewById(R.id.okButton);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            progressBar.setVisibility(ProgressBar.GONE);
            errorIcon.setVisibility(ImageView.VISIBLE);
            errorTextView.setVisibility(TextView.VISIBLE);
            errorTextView.setText("" + errorMessage);
            okButton.setVisibility(Button.VISIBLE);
            okButton.setOnClickListener(v -> dismissLoadingDialog());
        } else {
            progressBar.setVisibility(ProgressBar.VISIBLE);
            errorIcon.setVisibility(ImageView.GONE);
            errorTextView.setVisibility(TextView.GONE);
            okButton.setVisibility(Button.GONE);
        }

        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void login(String userKey) {
        showLoadingDialog(null);

        Handler loginHandler = new Handler(msg -> {
            dismissLoadingDialog();
            if (msg.what == 0) {
                String zipUrl = ziplink() + "ZENIN.zip";
                new DownC(this).execute(zipUrl);
                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show();
                // Save key after successful login
                
            } else if (msg.what == 1) {
                String errorMessage = (String) msg.obj;
                showLoadingDialog(errorMessage);
            }
            return true;
        });

        new Thread(() -> {
            String result = native_Check(this, userKey);
            if ("OK".equals(result)) {
                loginHandler.sendEmptyMessage(0);
            } else {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = result;
                loginHandler.sendMessage(msg);
            }
        }).start();
    }

    private static native String native_Check(Context context, String userKey);

    private static native String activity();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            if (isStoragePermissionGranted()) {
                checkAndRequestPermissions();
            } else {
                checkAndRequestPermissions();
            }
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (canRequestPackageInstalls()) {
                Process.killProcess(Process.myPid());
            }
        }
    }
}