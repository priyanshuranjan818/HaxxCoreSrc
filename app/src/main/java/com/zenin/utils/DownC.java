package com.zenin.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.zenin.R;
import com.zenin.activity.MainActivity;
import android.app.Activity;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownC extends AsyncTask<String, Integer, String> {
    private final Context context;
    private AlertDialog progressDialog;
    private TextView progressText;
    private LottieAnimationView lottieAnimationView;

    public DownC(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.progress_dialog, null);

        progressText = dialogView.findViewById(R.id.progressText);
        lottieAnimationView = dialogView.findViewById(R.id.lottieAnimationView);
        lottieAnimationView.playAnimation();
        lottieAnimationView.loop(true);

        builder.setView(dialogView);
        builder.setCancelable(false);
        builder.setTitle(R.string.checking_data);
        builder.setMessage(R.string.waiting_don_t_close_application);

        progressDialog = builder.create();
        progressDialog.show();
    }

    // YEH METHOD ADD KARO - ORIGINAL CODE SE
    @Override
    protected String doInBackground(String... urls) {
        String zipUrl = urls[0];

        try {
            // File paths
            File cacheDir = context.getCacheDir();
            File tempFile = new File(cacheDir, "ZENIN.zip");
            File finalFile = new File(cacheDir, "dynamic.zip");

            // Clean up any old files
            if (tempFile.exists()) tempFile.delete();
            if (finalFile.exists()) finalFile.delete();

            // Download ZIP file
            URL downloadUrl = new URL(zipUrl);
            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return "ERROR: HTTP response code " + responseCode;
            }

            int fileLength = connection.getContentLength();
            try (InputStream input = connection.getInputStream();
                 OutputStream output = new FileOutputStream(tempFile)) {
                byte[] data = new byte[8192];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) ((total * 100) / fileLength));
                    output.write(data, 0, count);
                }
            }

            // Verify ZIP integrity
            try {
                ZipFile zipCheck = new ZipFile(tempFile);
                if (zipCheck.isValidZipFile()) {
                    tempFile.renameTo(finalFile);
                    extractZipFile(finalFile, context.getFilesDir());
                    return "SUCCESS";
                } else {
                    tempFile.delete();
                    return "INVALID_ZIP";
                }
            } catch (Exception e) {
                tempFile.delete();
                return "VERIFY_FAILED";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    private void extractZipFile(File zipFile, File destinationDir) {
        try {
            // Extract to temporary directory
            File tempExtractDir = new File(destinationDir, "temp_extract");
            if (!tempExtractDir.exists()) tempExtractDir.mkdirs();

            new ZipFile(zipFile).extractAll(tempExtractDir.getPath());

            // Create loader dir
            File loaderDir = new File(destinationDir, "loader");
            if (!loaderDir.exists()) loaderDir.mkdirs();

            // Move libbgmi.so from temp to loader
            File extractedLib = new File(tempExtractDir, "libbgmi.so");
            File targetLib = new File(loaderDir, "libbgmi.so");

            if (extractedLib.exists()) {
                extractedLib.renameTo(targetLib);
            }

            // Delete temp extracted files
            deleteRecursive(tempExtractDir);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progressDialog != null && progressDialog.isShowing()) {
            int percent = progress[0];
            progressText.setText(percent + "%");
            
            // Agar 100% complete ho toh animation stop karo
            if (percent == 100) {
                lottieAnimationView.cancelAnimation();
                progressText.setText("Download Complete!");
            }
        }
    }

    @Override
    protected void onPostExecute(String result) {
        // Animation stop karo chahe success ho ya fail
        if (lottieAnimationView != null) {
            lottieAnimationView.cancelAnimation();
        }
        
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        switch (result) {
            case "SUCCESS":
                context.startActivity(new Intent(context, MainActivity.class));
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
                break;
            case "INVALID_ZIP":
                Toast.makeText(context, "Downloaded file is corrupted. Please try again.", Toast.LENGTH_LONG).show();
                break;
            case "VERIFY_FAILED":
                Toast.makeText(context, "File verification failed. Please try again.", Toast.LENGTH_LONG).show();
                break;
            default:
                if (result.startsWith("ERROR:")) {
                    Toast.makeText(context, "Error: " + result.substring(6), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}