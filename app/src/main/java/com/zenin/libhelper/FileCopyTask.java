package com.zenin.libhelper;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.app.Activity;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.haxxcore.engine.env.HaxxEnvironment;

public class FileCopyTask {

    private Activity activity;
    private ProgressDialog progressDialog;
    private String errorMessage;

    public FileCopyTask(Activity activity) {
        this.activity = activity;

        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Copying Files...");
        progressDialog.setMessage("Preparing...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
    }

    public interface CopyCallback {
        void onCopyCompleted(boolean success);
    }

    public boolean isObbCopied(String packageName) {
        File sourceDir = new File("/storage/emulated/0/Android/obb/", packageName);
        File destDir = HaxxEnvironment.getExternalObbDir(packageName);

        if (destDir.exists() && destDir.isDirectory()) {
            File[] destFiles = destDir.listFiles();
            File[] sourceFiles = sourceDir.exists() ? sourceDir.listFiles() : null;

            if (destFiles != null && destFiles.length > 0) {
                long sourceSize = 0;
                if (sourceFiles != null) {
                    for (File f : sourceFiles) {
                        sourceSize += f.length();
                    }
                }

                long destSize = 0;
                for (File f : destFiles) {
                    destSize += f.length();
                }

                // If source is readable, ensure the copy size roughly matches (>=)
                if (sourceSize > 0 && destSize >= sourceSize) {
                    return true;
                }
                
                // Android 13/11+ SAF might hide the source dir listFiles(),
                // so we fallback to a minimum realistic size constraint (~100MB)
                if (sourceSize == 0 && destSize > 100 * 1024 * 1024L) {
                    return true;
                }
            }
        }
        return false;
    }

    public void copyObbFolderAsync(final String packageName, final CopyCallback callback) {
        if (isObbCopied(packageName)) {
            if (callback != null) {
                callback.onCopyCompleted(true);
            }
            return;
        }

        new AsyncTask<Void, Integer, Boolean>() {

            @Override
            protected void onPreExecute() {
                progressDialog.setMessage("Copying OBB: " + packageName);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                File sourceDir = new File("/storage/emulated/0/Android/obb/", packageName);
                File destDir = HaxxEnvironment.getExternalObbDir(packageName);

                if (!sourceDir.exists() || !sourceDir.isDirectory()) {
                    errorMessage = "OBB not found!";
                    return false;
                }

                if (!destDir.exists() && !destDir.mkdirs()) {
                    errorMessage = "Destination directory creation failed!";
                    return false;
                }

                File[] files = sourceDir.listFiles();
                if (files == null || files.length == 0) {
                    errorMessage = "No files found to copy!";
                    return false;
                }

                long totalBytes = 0, copiedBytes = 0;
                for (File file : files) {
                    totalBytes += file.length();
                }

                try {
                    byte[] buffer = new byte[8192];
                    for (File file : files) {
                        InputStream inputStream = new FileInputStream(file);
                        FileOutputStream outputStream = new FileOutputStream(new File(destDir, file.getName()));

                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            copiedBytes += bytesRead;
                            publishProgress((int) ((copiedBytes * 100) / totalBytes));
                        }

                        inputStream.close();
                        outputStream.close();
                    }
                } catch (IOException e) {
                    errorMessage = "Error copying files: " + e.getMessage();
                    return false;
                }
                return true;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressDialog.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                progressDialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                if (result) {
                    builder.setTitle("Success").setMessage("Copy Successful!");
                } else {
                    builder.setTitle("Error").setMessage(errorMessage);
                }
                builder.setCancelable(false).setPositiveButton("OK", null).show();

                if (callback != null) {
                    callback.onCopyCompleted(result);
                }
            }
        }.execute();
    }

    // Delete OBB folder completely
    public static boolean deleteObbFolder(String packageName) {
        File obbDestDir = HaxxEnvironment.getExternalObbDir(packageName);
        return deleteDirectory(obbDestDir);
    }

    // Recursive delete method
    private static boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            for (File child : dir.listFiles()) {
                deleteDirectory(child);
            }
        }
        return dir.delete();
    }
}
