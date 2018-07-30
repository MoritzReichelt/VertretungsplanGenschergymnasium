package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import static android.content.ContentValues.TAG;

class Methods {

    private static String getCurrentDateTime() {
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
        currentDateTime = currentDateTime.replaceAll("\\s+", "_");
        currentDateTime = currentDateTime.replaceAll(":", "_");
        return currentDateTime;
    }

    public static void reduceDirContent(File[] files, int filesToBeLeft) {
        Log.i(TAG, "The following files are inside the array(" + files.length + "): " + Arrays.toString(files));

        int filesToDelete = files.length - filesToBeLeft;
        if (filesToDelete > 0) {
            for (int i = 0; i < filesToDelete; i++) {
                File file = files[i];
                boolean success = deleteFile(file);
                Log.i(TAG,"Deleted file has value " + success);
            }
        }
    }

    private static boolean deleteFile(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteFile(child);
            }
        }
        Log.i(TAG, "Deleted file '" + file + "'");
        return file.delete();
    }

    // SharedPrefsAuthentication Methoden

    public static String getSharedPrefsURL(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_url", "http://www.genscher-halle.bildung-lsa.de/plan/vplan.xml");
    }

    public static String getSharedPrefsUsername(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_username", "Genscher");
    }

    public static String getSharedPrefsPassword(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_password", "Schule");
    }

    public static void downloadPlan(Context context, String dirPath) {
        Log.i(TAG, "Attempting to download a vplan...");

        String urlString = getSharedPrefsURL(context);
        String username = getSharedPrefsUsername(context);
        String password = getSharedPrefsPassword(context);

        String credentials = username + ":" + password;
        byte[] data = new byte[0];
        try {
            data = credentials.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String base64 = Base64.encodeToString(data, Base64.NO_WRAP);

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.setRequestProperty("Authorization", "Basic " + base64);

            if (connection.getResponseCode() != 401) {
                Log.i(TAG, "Response code: " + String.valueOf(connection.getResponseCode()));

                InputStream is = connection.getInputStream();
                Log.i(TAG, "Getting input stream from server...");
                String currentDateTime = getCurrentDateTime();

                File file = new File(dirPath + "Plan_" + currentDateTime + ".xml");
                Log.i(TAG, "Writing input stream to file " + file.toString());
                FileUtils.copyInputStreamToFile(is, file);
            } else {
                Log.i(TAG, "Response code was 401, meaning the client was unauthorized to access" +
                        " the server");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

