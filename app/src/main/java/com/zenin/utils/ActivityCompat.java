package com.zenin.utils;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import com.zenin.utils.UiKit;
import java.io.IOException;
import android.os.Handler;
import android.app.ProgressDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import android.content.Context;
import org.jdeferred.android.AndroidDeferredManager;

import com.blankj.molihuan.utilcode.util.FileUtils;
import com.blankj.molihuan.utilcode.util.ToastUtils;
import com.zenin.activity.MainActivity;
import com.zenin.utils.FPrefs;
import com.zenin.R;


public class ActivityCompat extends AppCompatActivity {
    private static ActivityCompat activityCompat;
    public static int REQUEST_OVERLAY_PERMISSION = 5469;
    public static int PERMISSION_REQUEST_STORAGE = 100;
    public static int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;
    public boolean isLogin = false;
    public FPrefs prefs;
    
    
    public static ActivityCompat getActivityCompat() {
        return activityCompat;
    }
    
    public FPrefs getPref() {
        return FPrefs.with(this);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activityCompat = this;
        super.onCreate(savedInstanceState);
        setNavBar(R.color.background);
        prefs = getPref();
        ManageFiles();
    }
    
   public void setNavBar(int color){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(ContextCompat.getColor(this,color));
    }
    
    public void restartApp(String clazz) {
        Intent lauchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        lauchIntent.addFlags(335577088);
        lauchIntent.putExtra("restartApp", clazz);
        startActivity(lauchIntent);
        Runtime.getRuntime().exit(0);
    }
    
    public void toast(CharSequence msg) {
        ToastUtils _toast = ToastUtils.make();
        _toast.setBgColor(android.R.color.white);
        _toast.setLeftIcon(R.drawable.ic_launcher);
        //_toast.setGravity(Gravity.BOTTOM, Gravity.CENTER, Gravity.CENTER);
        _toast.setTextColor(android.R.color.black);
        _toast.setNotUseSystemToast();
        //_toast.setBgResource(R.drawable.button_coming);
        _toast.show(msg);
        //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    public void toastImage(int id, CharSequence msg) {
        ToastUtils _toast = ToastUtils.make();
        _toast.setBgColor(android.R.color.white);
        _toast.setLeftIcon(id);
        _toast.setTextColor(android.R.color.black);
        _toast.setNotUseSystemToast();
        _toast.show(msg);
    }
    
    public void RestartApp() {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
    
    public void takeFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE},PERMISSION_REQUEST_STORAGE);
        }
    }
    
    public boolean isPermissionGaranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          return Environment.isExternalStorageManager();
        } else {
          return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    public void InstllUnknownApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
            } else {
              if (!isPermissionGaranted()) {
                 takeFilePermissions();
              }
            }
        }
    }
    
    public void OverlayPermision() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            } else {
                InstllUnknownApp();
            }
        }
    }
    
    public void ManageFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_STORAGE);
            } else {
                OverlayPermision();
            }
        }
    }
    
    protected AndroidDeferredManager defer() {
        return UiKit.defer();
    }
    
    private long backPressedTime = 0; 
    
    @Override
    public void onBackPressed() {
        if (isLogin) {
            long t = System.currentTimeMillis();
            if (t - backPressedTime > 2000) {    // 2 secs
                backPressedTime = t;
                toast("Press back again to exit");
            } else {
                super.onBackPressed();
            }
        }
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
    }
    
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_IMMERSIVE
        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
    
}
