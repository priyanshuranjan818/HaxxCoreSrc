package com.zenin.utils;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import top.niunaijun.blackbox.core.env.BEnvironment;

public final class AuthSessionBridge {
    private static final String BRIDGE_DIR = "auth_bridge";
    private static final String BRIDGE_FILE = "session.json";

    private AuthSessionBridge() {
    }

    public static boolean saveCallbackSession(Context context, String provider, Uri callbackUri) {
        if (context == null || TextUtils.isEmpty(provider) || callbackUri == null) {
            return false;
        }
        String code = callbackUri.getQueryParameter("code");
        String state = callbackUri.getQueryParameter("state");
        String error = callbackUri.getQueryParameter("error");
        return saveRawSession(context, provider, code, state, error, callbackUri.toString());
    }

    public static boolean saveRawSession(
            Context context,
            String provider,
            String code,
            String state,
            String error,
            String rawCallback
    ) {
        try {
            File outDir = new File(context.getFilesDir(), BRIDGE_DIR);
            if (!outDir.exists() && !outDir.mkdirs()) {
                return false;
            }

            JSONObject payload = new JSONObject();
            payload.put("provider", provider);
            payload.put("code", code == null ? "" : code);
            payload.put("state", state == null ? "" : state);
            payload.put("error", error == null ? "" : error);
            payload.put("raw_callback", rawCallback == null ? "" : rawCallback);
            payload.put("created_at", System.currentTimeMillis());
            payload.put("status", TextUtils.isEmpty(error) ? "ok" : "error");

            return writeToFile(new File(outDir, BRIDGE_FILE), payload.toString());
        } catch (JSONException e) {
            return false;
        }
    }

    public static boolean saveBackendSession(
            Context context,
            String provider,
            String code,
            String state,
            String accessToken,
            String refreshToken,
            long expiresInSec
    ) {
        try {
            File outDir = new File(context.getFilesDir(), BRIDGE_DIR);
            if (!outDir.exists() && !outDir.mkdirs()) {
                return false;
            }

            JSONObject payload = new JSONObject();
            payload.put("provider", provider == null ? "" : provider);
            payload.put("code", code == null ? "" : code);
            payload.put("state", state == null ? "" : state);
            payload.put("access_token", accessToken == null ? "" : accessToken);
            payload.put("refresh_token", refreshToken == null ? "" : refreshToken);
            payload.put("expires_in", expiresInSec);
            payload.put("created_at", System.currentTimeMillis());
            payload.put("status", TextUtils.isEmpty(accessToken) ? "error" : "ok");

            return writeToFile(new File(outDir, BRIDGE_FILE), payload.toString());
        } catch (JSONException e) {
            return false;
        }
    }

    public static boolean syncToVirtualApp(String packageName, int userId, Context context) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        File hostSession = getHostSessionFile(context);
        if (!hostSession.exists()) {
            return false;
        }

        File virtualFilesDir = BEnvironment.getDataFilesDir(packageName, userId);
        File virtualBridgeDir = new File(virtualFilesDir, BRIDGE_DIR);
        if (!virtualBridgeDir.exists() && !virtualBridgeDir.mkdirs()) {
            return false;
        }

        File virtualSession = new File(virtualBridgeDir, BRIDGE_FILE);
        return writeToFile(virtualSession, readToString(hostSession));
    }

    public static File getHostSessionFile(Context context) {
        return new File(new File(context.getFilesDir(), BRIDGE_DIR), BRIDGE_FILE);
    }

    public static boolean hasHostSession(Context context) {
        return context != null && getHostSessionFile(context).exists();
    }

    public static String getHostSessionJson(Context context) {
        if (context == null) {
            return "";
        }
        File file = getHostSessionFile(context);
        if (!file.exists()) {
            return "";
        }
        return readToString(file);
    }

    private static boolean writeToFile(File file, String content) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            fos.flush();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String readToString(File file) {
        InputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] data = new byte[(int) file.length()];
            int read = inputStream.read(data);
            if (read <= 0) {
                return "";
            }
            return new String(data, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
