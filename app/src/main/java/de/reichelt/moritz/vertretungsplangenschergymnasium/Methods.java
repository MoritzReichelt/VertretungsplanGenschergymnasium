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

    /**
     * @return The current system date and time as a string formatted as DD.MM.YYYY_HH_MM_SS
     */
    private static String getCurrentDateTime() {
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
        currentDateTime = currentDateTime.replaceAll("\\s+", "_");
        currentDateTime = currentDateTime.replaceAll(":", "_");
        return currentDateTime;
    }


    /**
     * Reduces a given file array to a given amount of files.
     *
     * @param files         Array of files that should be reduced
     * @param filesToBeLeft The amount of files that should be left
     */
    public static void reduceDirContent(File[] files, int filesToBeLeft) {
        Log.i(TAG, "The following files are inside the array(" + files.length + "): " + Arrays.toString(files));

        int filesToDelete = files.length - filesToBeLeft;
        if (filesToDelete > 0) {
            for (int i = 0; i < filesToDelete; i++) {
                File file = files[i];
                boolean success = deleteFile(file);
                Log.i(TAG, "Deleted file has value " + success);
            }
        }
    }


    /**
     * Deletes a file recursively.
     *
     * @param file The file that will be deleted
     * @return Boolean indicating whether or not the deletion was successful
     */
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


    /**
     * Retrieves the URL that is currently saved in SharedPreferences.
     *
     * @param context Application context
     * @return String containing the SharedPrefs URL
     */
    public static String getSharedPrefsURL(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_url", "http://www.genscher-halle.bildung-lsa.de/plan/vplan.xml");
    }


    /**
     * Retrieves the username that is currently saved in SharedPreferences.
     *
     * @param context Application context
     * @return String containing the SharedPrefs username
     */
    public static String getSharedPrefsUsername(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_username", "Genscher");
    }


    /**
     * Retrieves the password that is currently saved in SharedPreferences.
     *
     * @param context Application context
     * @return String containing the SharedPrefs password
     */
    public static String getSharedPrefsPassword(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("pref_password", "Schule");
    }


    /**
     * Downloads an instance of the vplan using the authentication values from SharedPreferences.
     *
     * @param context Application context
     * @param dirPath Path to where the file should be saved
     */
    public static void downloadPlan(Context context, String dirPath) {
        Log.i(TAG, "Attempting to download a vplan...");

        String urlString = getSharedPrefsURL(context);
        String username = getSharedPrefsUsername(context);
        String password = getSharedPrefsPassword(context);
        int responseCode = -1;

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
            responseCode = connection.getResponseCode();
            if (responseCode != 401) {
                Log.i(TAG, "Response code: " + String.valueOf(responseCode));

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
        Log.i(TAG,"Some error occurred, the vplan could not be downloaded! Response code: " + responseCode);
    }


    /**
     * Retrieves a string from a file.
     *
     * @param file The file to be used
     * @return String containing the contents of the file
     */
    public static String getStringFromFile(File file) {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

