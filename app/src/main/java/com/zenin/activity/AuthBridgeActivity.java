package com.zenin.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zenin.utils.AuthSessionBridge;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthBridgeActivity extends AppCompatActivity {
    private static final String TAG = "ZeninAuth";
    public static final String EXTRA_PROVIDER = "provider";
    private static final String PROVIDER_FACEBOOK = "facebook";
    private static final String PROVIDER_TWITTER = "twitter";

    private static final String CALLBACK_SCHEME = "zenin";
    private static final String CALLBACK_HOST = "auth";
    private static final String CALLBACK_PATH = "callback";
    private static final String OAUTH_PREF = "oauth_bridge_prefs";
    private static final String KEY_EXPECTED_STATE = "expected_state";
    private static final String KEY_CODE_VERIFIER = "code_verifier";
    private static final String KEY_PROVIDER = "provider";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // Replace with your real OAuth client ids and backend callback flow.
    private static final String FB_CLIENT_ID = "1847354332646976";
    private static final String X_CLIENT_ID = "MC1ySEtlNUk0SmhPZ2xJbm1ZZzE6MTpjaQ";
    private static final String BACKEND_EXCHANGE_URL = "https://haxxcoresrc.onrender.com/api/social/exchange";
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate: " + getIntent());
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        android.util.Log.d(TAG, "onNewIntent: " + intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent != null ? intent.getData() : null;
        android.util.Log.d(TAG, "handleIntent data=" + data);
        if (data != null && isCallbackUri(data)) {
            String provider = data.getQueryParameter("provider");
            if (TextUtils.isEmpty(provider)) {
                provider = getStoredProvider();
            }
            android.util.Log.d(TAG, "Callback received. provider=" + provider + " data=" + data);

            // Facebook native callback (fb{APP_ID}://authorize) does NOT carry a `state` param.
            boolean isFacebookNative = ("fb" + FB_CLIENT_ID).equals(data.getScheme())
                    && "authorize".equals(data.getHost());

            if (!isFacebookNative && !isStateValid(data.getQueryParameter("state"))) {
                android.util.Log.e(TAG, "Invalid OAuth state! expected=" + getSharedPreferences(OAUTH_PREF, MODE_PRIVATE).getString(KEY_EXPECTED_STATE, "<none>") + " got=" + data.getQueryParameter("state"));
                Toast.makeText(this, "Invalid OAuth state", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            exchangeCodeWithBackend(provider, data);
            return;
        }

        String provider = intent != null ? intent.getStringExtra(EXTRA_PROVIDER) : null;
        android.util.Log.d(TAG, "Starting provider auth: " + provider);
        if (TextUtils.isEmpty(provider)) {
            Toast.makeText(this, "Missing auth provider", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        startProviderAuth(provider);
    }

    private void startProviderAuth(String provider) {
        String state = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        persistOAuthSession(state, codeVerifier, provider);
        android.util.Log.d(TAG, "startProviderAuth: provider=" + provider + " state=" + state + " codeChallenge=" + codeChallenge);

        Uri callbackUri = buildRedirectUri(provider);

        Uri authUri;
        if (PROVIDER_FACEBOOK.equalsIgnoreCase(provider)) {
            authUri = new Uri.Builder()
                    .scheme("https")
                    .authority("www.facebook.com")
                    .appendPath("v19.0")
                    .appendPath("dialog")
                    .appendPath("oauth")
                    .appendQueryParameter("client_id", FB_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", callbackUri.toString())
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("state", state)
                    .build();
        } else if (PROVIDER_TWITTER.equalsIgnoreCase(provider)) {
            authUri = new Uri.Builder()
                    .scheme("https")
                    .authority("twitter.com")
                    .appendPath("i")
                    .appendPath("oauth2")
                    .appendPath("authorize")
                    .appendQueryParameter("client_id", X_CLIENT_ID)
                    .appendQueryParameter("redirect_uri", callbackUri.toString())
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("state", state)
                    .appendQueryParameter("scope", "tweet.read users.read offline.access")
                    .appendQueryParameter("code_challenge", codeChallenge)
                    .appendQueryParameter("code_challenge_method", "S256")
                    .build();
        } else {
            Toast.makeText(this, "Unsupported provider: " + provider, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, authUri);
        browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
        android.util.Log.d(TAG, "Opening browser for auth: " + authUri);
        startActivity(browserIntent);
        // DO NOT call finish() here!
        // With singleTask, keeping this activity alive ensures onNewIntent()
        // is called when the OAuth callback URI (zenin://auth/callback) arrives.
        // Calling finish() would destroy the stored state/codeVerifier and
        // force Android to create a NEW instance for the callback, breaking PKCE.
    }

    private boolean isCallbackUri(Uri uri) {
        if (uri == null) return false;
        // Our generic callback (Twitter + internal flow)
        if (CALLBACK_SCHEME.equals(uri.getScheme())
                && CALLBACK_HOST.equals(uri.getHost())
                && ("/" + CALLBACK_PATH).equals(uri.getPath())) {
            return true;
        }
        // Facebook native callback: fb{APP_ID}://authorize
        return ("fb" + FB_CLIENT_ID).equals(uri.getScheme())
                && "authorize".equals(uri.getHost());
    }

    private void persistOAuthSession(String state, String codeVerifier, String provider) {
        SharedPreferences prefs = getSharedPreferences(OAUTH_PREF, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_EXPECTED_STATE, state)
                .putString(KEY_CODE_VERIFIER, codeVerifier)
                .putString(KEY_PROVIDER, provider)
                .apply();
    }

    private boolean isStateValid(String callbackState) {
        SharedPreferences prefs = getSharedPreferences(OAUTH_PREF, MODE_PRIVATE);
        String expected = prefs.getString(KEY_EXPECTED_STATE, "");
        return !TextUtils.isEmpty(expected) && expected.equals(callbackState);
    }

    private String getStoredCodeVerifier() {
        return getSharedPreferences(OAUTH_PREF, MODE_PRIVATE).getString(KEY_CODE_VERIFIER, "");
    }

    private String getStoredProvider() {
        return getSharedPreferences(OAUTH_PREF, MODE_PRIVATE).getString(KEY_PROVIDER, "");
    }

    private String generateCodeVerifier() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.encodeToString(random, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (Exception e) {
            return verifier;
        }
    }

    private void exchangeCodeWithBackend(String provider, Uri callbackUri) {
        final String code = callbackUri.getQueryParameter("code");
        final String state = callbackUri.getQueryParameter("state");
        final String error = callbackUri.getQueryParameter("error");
        final String verifier = getStoredCodeVerifier();
        android.util.Log.d(TAG, "exchangeCodeWithBackend: provider=" + provider + " code=" + (code != null ? code.substring(0, Math.min(8, code.length())) + "..." : "null") + " error=" + error + " verifier_len=" + (verifier != null ? verifier.length() : 0));

        if (!TextUtils.isEmpty(error) || TextUtils.isEmpty(code)) {
            android.util.Log.e(TAG, "OAuth error or missing code. error=" + error + " code=" + code);
            boolean saved = AuthSessionBridge.saveCallbackSession(this, provider, callbackUri);
            Toast.makeText(this, saved ? "OAuth error: " + error : "OAuth callback save failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(() -> {
            boolean success = false;
            try {
                JSONObject body = new JSONObject();
                body.put("provider", provider);
                body.put("code", code);
                body.put("state", state);
                body.put("code_verifier", verifier);
                body.put("redirect_uri", buildRedirectUri(provider).toString());

                Request request = new Request.Builder()
                        .url(BACKEND_EXCHANGE_URL)
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new IOException("Exchange failed: HTTP " + response.code());
                    }
                    String responseJson = response.body().string();
                    android.util.Log.d(TAG, "Backend response: " + responseJson);
                    JSONObject parsed = new JSONObject(responseJson);
                    String accessToken = parsed.optString("access_token", "");
                    String refreshToken = parsed.optString("refresh_token", "");
                    long expiresIn = parsed.optLong("expires_in", 0L);
                    android.util.Log.d(TAG, "Tokens: access=" + (accessToken.length() > 0 ? "OK" : "EMPTY") + " refresh=" + (refreshToken.length() > 0 ? "OK" : "EMPTY"));
                    success = AuthSessionBridge.saveBackendSession(
                            AuthBridgeActivity.this,
                            provider,
                            code,
                            state,
                            accessToken,
                            refreshToken,
                            expiresIn
                    );
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Backend exchange exception: " + e.getMessage(), e);
                success = AuthSessionBridge.saveCallbackSession(AuthBridgeActivity.this, provider, callbackUri);
            }

            final boolean finalSuccess = success;
            runOnUiThread(() -> {
                Toast.makeText(
                        AuthBridgeActivity.this,
                        finalSuccess ? "Social login session prepared" : "Failed to prepare social session",
                        Toast.LENGTH_SHORT
                ).show();
                finish();
            });
        }).start();
    }

    private Uri buildRedirectUri(String provider) {
        // Facebook works most reliably with fb{APP_ID}://authorize for native flows.
        if (PROVIDER_FACEBOOK.equalsIgnoreCase(provider)) {
            return new Uri.Builder()
                    .scheme("fb" + FB_CLIENT_ID)
                    .authority("authorize")
                    .build();
        }

        // Twitter stays on our custom scheme.
        return new Uri.Builder()
                .scheme(CALLBACK_SCHEME)
                .authority(CALLBACK_HOST)
                .appendPath(CALLBACK_PATH)
                .appendQueryParameter("provider", provider)
                .build();
    }
}
