package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class NotificationJobService extends JobService {

    private File file1, file2;
    private boolean errorOccurred, success;
    private String path, fileContentTemp = null;

    /**
     * This is called by the system once it determines it is time to run the job.
     *
     * @param jobParameters Contains the information about the job
     * @return Boolean indicating whether or not the job was offloaded to a separate thread.
     * In this case, it is true since the notification cannot be posted on the main thread.
     */
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        Log.i(TAG, "Job fired!");

        errorOccurred = false;

        final Thread thread;

        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (canPing()) {

                    path = "/storage/emulated/0/Android/data/" + getPackageName() + "/files/plans/";

                /* Wenn noch kein Vergleichs-VPlan im Ordner ist, wird dieser erstellt und
                ein VPlan heruntergeladen (normalerweise nur nach Erstinstallation der Fall)
                 */
                    if (isDirEmpty(path)) {
                        Log.i(TAG, "Directory is empty");
                        createDir(path);
                        Methods.downloadPlan(getApplicationContext(), path);
                    }

                    deleteInvalidFilesIfPresent();

                    File dir = new File(path);
                    File[] allFiles;

                    int amountOfFiles = dir.listFiles().length;
                    if (amountOfFiles == 0) {
                        Log.i(TAG, "The directory seems to be empty, no plan in sight!");
                        Methods.downloadPlan(getApplicationContext(), path);
                    } else if (amountOfFiles > 1) {
                        Log.i(TAG, "Directory contains " + amountOfFiles + " files and therefore the content will be reduced" +
                                " to one file");
                        allFiles = dir.listFiles();
                        allFiles = sortFileArrayLastModified(allFiles);
                        Methods.reduceDirContent(allFiles, 1);
                    }

                /*
                  Lädt den Vertretungsplan herunter
                  Speichert ihn im App-internen Ordner Documents
                  Benennt die .xml Datei nach der aktuellen Uhrzeit
                 */

                    Methods.downloadPlan(getApplicationContext(), path);

                    // 10s Wartezeit, um genügend Spielraum für Download des VPlans zu haben (ca. 5-11kb)

                    deleteInvalidFilesIfPresent();

                    amountOfFiles = dir.listFiles().length;
                    if (amountOfFiles <= 1) {
                        Log.i(TAG, "Directory does not contain enough plans to compare (only " + amountOfFiles + " file(s))");
                        Log.i(TAG, "Calling jobFinished(), reschedule will be executed...");
                        jobFinished(jobParameters, true);
                    } else {
                        Log.i(TAG, "Directory contains 2 valid files, we're good to go!");

                /*
                  Ermittelt die beiden Dateien im Ordner Documents
                  Lädt diese in einen String
                  Generiert daraus einen MD5-Hash
                 */

                        allFiles = dir.listFiles();
                        allFiles = sortFileArrayLastModified(allFiles);

                        String file1Name = allFiles[0].toString();
                        String file2Name = allFiles[1].toString();

                        file1 = new File(file1Name);
                        file2 = new File(file2Name);

                        Log.i(TAG, "The following files are being compared: " + file1Name + " and " + file2Name);

                        String file1MD5 = calculateMD5(file1);
                        String file2MD5 = calculateMD5(file2);

                        Log.i(TAG, "File 1 has MD5-Hash: " + file1MD5);
                        Log.i(TAG, "File 2 has MD5-Hash: " + file2MD5);

                    /*Vergleicht die Hashes der beiden Dateien
                      Wenn sie verschieden voneinander sind, wird jeweils die ältere Datei gelöscht
                      und eine Benachrichtigung an den Nutzer gesendet
                      Wenn die Hashes gleich sind, wird eine der beiden Dateien gelöscht
                     */

                        //Ein neuer VPlan ist verfügbar!
                        if (!Objects.equals(file1MD5, file2MD5)) {

                        /*
                          Extrahiert das Datum aus der .xml-Datei
                          und teilt es in zwei Strings auf nach dem Muster
                          fileDateLeft = "Montag"
                          fileDateRight = "16. April 2018"
                         */

                            fileContentTemp = getFileContentTemp(file2);

                            if (fileContentTemp != null) {
                                String file1String = getFileContentTemp(file1);
                                String file2String = getFileContentTemp(file2);

                                String file1Date = getLeftDateFromFile(file1String);
                                String file2Date = getLeftDateFromFile(file2String);

                                Log.i(TAG, "File 1 has date: " + file1Date);
                                Log.i(TAG, "File 2 has date: " + file2Date);

                                if (Objects.equals(file1Date, file2Date)) {
                                    Log.i(TAG, "A changed plan is available!");
                                    sendNotifyWhenPlanChanged();
                                } else {
                                    Log.i(TAG, "A new plan is available!");
                                    sendNotifyWhenNewPlan(fileContentTemp);
                                }
                            } else {
                                errorOccurred = true;
                                Log.e(TAG, "Error: fileContentTemp ist null, d.h. es gab einen Fehler" +
                                        "beim Einlesen der neueren Datei in einen String!");
                            }
                            //Lösche die ältere Datei

                            if (FileUtils.isFileNewer(file1, file2)) {
                                success = deleteFile(file2);
                            } else {
                                success = deleteFile(file1);
                            }
                        }
                        //Kein neuer Vertretungsplan verfügbar
                        else {
                            //sendNotifyWhenNoNewPlan();
                            Log.i(TAG, "No new plan!");
                            success = deleteFile(file2);
                        }
                    }
                    Log.i(TAG, "Calling jobFinished, no reschedule needed...");
                    jobFinished(jobParameters, false);
                } else {

                    BroadcastReceiver broadcastReceiver;
                    broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            Log.i(TAG, "Background data seems to be restricted, sending " +
                                    "notification to address the user!");
                            sendNotifyWhenBackgroundDataRestricted();
                        }
                    };

                    IntentFilter filter;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        filter = new IntentFilter(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED);
                        filter.addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED);
                        getApplicationContext().registerReceiver(broadcastReceiver, filter);
                    }

                    Log.i(TAG, "Calling jobFinished, reschedule will be executed...");
                    jobFinished(jobParameters, true);
                }
            }
        });

        thread.start();
        return true;
    }

    /**
     * Called by the system when the job is running but the conditions are no longer met.
     *
     * @param jobParameters Contains the information about the job
     * @return Boolean indicating whether the job needs rescheduling
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.i(TAG, "Deleted file has value" + success);
        Log.i(TAG, "Job stopped!");
        return errorOccurred;
    }

    private static String calculateMD5(File updateFile) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception while getting digest", e);
            return null;
        }

        InputStream is;
        try {
            is = new FileInputStream(updateFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "Exception while getting FileInputStream", e);
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception on closing MD5 input stream", e);
            }
        }
    }

    private static String getStringFromFile(File file) {
        try {
            return FileUtils.readFileToString(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Boolean isDirEmpty(String path) {
        File f = new File(path);
        return !f.exists() || f.listFiles() == null;
    }

    private void createDir(String filepath) {
        File f = new File(filepath);
        boolean successfullyCreatedDir = f.mkdir();
        Log.i(TAG, "Boolean succesfullyCreatedDir has value " + successfullyCreatedDir);
    }

    /*
        private void downloadVPlan() {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String url = sharedPreferences.getString("pref_url", getString(R.string.pref_url_default));
            String username = sharedPreferences.getString("pref_username", "Genscher");
            String password = sharedPreferences.getString("pref_password", "Schule");

            Uri vplan_uri = Uri.parse(url);

            String currentDateTime = Methods.getCurrentDateTime();
            String credentials = username + ":" + password;
            byte[] data = new byte[0];
            try {
                data = credentials.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String base64 = Base64.encodeToString(data, Base64.DEFAULT);

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(vplan_uri);

            request.setDestinationInExternalFilesDir(NotificationJobService.this,
                    Environment.DIRECTORY_DOCUMENTS, "vplan" + currentDateTime + ".xml");
            request.addRequestHeader("Authorization", "Basic " + base64);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            assert downloadManager != null;
            Log.i(TAG, "Started download for 'vplan" + currentDateTime + ".xml'");
            downloadManager.enqueue(request);
            //File file = new File(path + currentDateTime + ".xml");
            //waitForDownloadToFinish(file);
        }
    */
    private void deleteInvalidFilesIfPresent() {
        Boolean invalidFileFound = false;
        Log.i(TAG, "Checking for any invalid files");
        File allFilesArray[] = new File(path).listFiles();
        for (File anAllFilesArray : allFilesArray) {
            if (!isValidFile(anAllFilesArray)) {
                invalidFileFound = true;
                success = deleteFile(anAllFilesArray);
                Log.i(TAG, "Deleted file that is not a regular plan '" + anAllFilesArray + "'");
            }
        }
        if (!invalidFileFound) {
            Log.i(TAG, "No invalid files were found!");
        }
        startMediaScanner();
    }

    private void sendNotifyWhenNewPlan(String fileContentTemp) {
        String fileContent1[] = fileContentTemp.split("<titel>");
        String fileDateTemp = fileContent1[1];
        String fileContent2[] = fileDateTemp.split(" </titel>");
        String fileDate = fileContent2[0];

        String fileDateTempBoth[] = fileDate.split(", ");
        String fileDateLeft = fileDateTempBoth[0];

        String fileDateRight = fileDateTempBoth[1];

        //PendingIntent sorgt dafür, dass die App startet, wenn die Benachrichtigung angeklickt wird
        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "Channel_1";
        int id = 1;
        CharSequence name = "Vertretungsplan-Benachrichtigungen";
        String description = "Sendet eine Benachrichtigung bei neuem Vertretungsplan";
        int importance = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            importance = NotificationManager.IMPORTANCE_DEFAULT;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Erstellt den NotificationChannel für Geräte mit Android >= 8.0
            NotificationChannel mChannel = new NotificationChannel(channel_id, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(mChannel);
        }
        String textSnippet = "Der Vertretungsplan für " + fileDateLeft + ", den " + fileDateRight;
        String bigText = "Der Vertretungsplan für " + fileDateLeft + ", den " + fileDateRight + ", ist verfügbar!";
        //Erstellt die eigentliche Benachrichtigung
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                .setSmallIcon(R.drawable.ic_job_running)
                .setContentTitle("Vertretungsplan")
                .setContentText(textSnippet)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

        assert mNotificationManager != null;
        mNotificationManager.notify(id, mBuilder.build());
        Log.i(TAG, "Sent notification for new plan! '" + bigText + "'");
    }

    /**
     * private void sendNotifyWhenNoNewPlan() {
     * //Set up the notification content intent to launch the app when clicked
     * PendingIntent contentPendingIntent = PendingIntent.getActivity
     * (getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
     * <p>
     * NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
     * String channel_id = "Channel_2";
     * int id = 2;
     * CharSequence name = "No VPLAN";
     * String description = "Sendet eine Benachrichtigung, wenn kein neuer Vertretungsplan gefunden wurde";
     * int importance = 0;
     * if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
     * importance = NotificationManager.IMPORTANCE_LOW;
     * }
     * <p>
     * if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
     * //Erstellt den NotificationChannel für Geräte mit Android >= 8.0
     * NotificationChannel mChannel = new NotificationChannel(channel_id, name, importance);
     * mChannel.setDescription(description);
     * mChannel.enableLights(true);
     * mChannel.setLightColor(Color.RED);
     * assert mNotificationManager != null;
     * mNotificationManager.createNotificationChannel(mChannel);
     * }
     * //Erstellt die eigentliche Benachrichtigung
     * NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
     * .setSmallIcon(R.drawable.ic_job_running)
     * .setContentTitle("Vertretungsplan")
     * .setContentText("Keinen neuen VPlan gefunden!")
     * .setContentIntent(contentPendingIntent)
     * .setAutoCancel(true)
     * .setStyle(new NotificationCompat.BigTextStyle().bigText("Das ist ein sehr langer Text, darum wird er nur dann" +
     * " komplett angezeigt, wenn man ihn ausklappt"));
     * <p>
     * assert mNotificationManager != null;
     * mNotificationManager.notify(id, mBuilder.build());
     * }
     */
    private void sendNotifyWhenPlanChanged() {
        //PendingIntent sorgt dafür, dass die App startet, wenn die Benachrichtigung angeklickt wird
        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "Channel_1";
        int id = 1;
        CharSequence name = "Vertretungsplan-Benachrichtigungen";
        String description = "Sendet eine Benachrichtigung bei neuem Vertretungsplan";
        int importance = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            importance = NotificationManager.IMPORTANCE_DEFAULT;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Erstellt den NotificationChannel für Geräte mit Android >= 8.0
            NotificationChannel mChannel = new NotificationChannel(channel_id, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(mChannel);
        }
        String text = "Der Vertretungsplan wurde aktualisiert!";
        //Erstellt die eigentliche Benachrichtigung
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                .setSmallIcon(R.drawable.ic_job_running)
                .setContentTitle("Vertretungsplan")
                .setContentText(text)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true);

        assert mNotificationManager != null;
        mNotificationManager.notify(id, mBuilder.build());
        Log.i(TAG, "Sent notification for changed plan!");
    }

    private void sendNotifyWhenBackgroundDataRestricted() {
        //PendingIntent sorgt dafür, dass die App startet, wenn die Benachrichtigung angeklickt wird
        PendingIntent contentPendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            contentPendingIntent = PendingIntent.getActivity
                    (getApplicationContext(), 0, new Intent(Settings.ACTION_IGNORE_BACKGROUND_DATA_RESTRICTIONS_SETTINGS,
                            Uri.parse("package:de.reichelt.moritz.vertretungsplangenschergymnasium")), PendingIntent.FLAG_UPDATE_CURRENT);
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = "Channel_2";
        int id = 1;
        CharSequence name = "Hintergrunddaten";
        String description = "Sendet eine Benachrichtigung, wenn Hintergrunddaten eingeschränkt sind (nur ab Android 7.0)";
        int importance = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            importance = NotificationManager.IMPORTANCE_HIGH;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            //Erstellt den NotificationChannel für Geräte mit Android >= 8.0
            NotificationChannel mChannel = new NotificationChannel(channel_id, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(mChannel);
        }

        //Erstellt die eigentliche Benachrichtigung
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                .setSmallIcon(R.drawable.ic_job_running)
                .setContentTitle("Vertretungsplan")
                .setContentText("Du hast die Hintergrunddaten für die App deaktiviert, wodurch du nicht " +
                        "über neue Vertretungspläne informiert wirst.")
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Du hast die Hintergrunddaten" +
                        " für die App deaktiviert, wodurch du nicht über neue Vertretungspläne informiert wirst." +
                        "Klicke auf die Benachrichtigung, um das zu ändern."));

        assert mNotificationManager != null;
        mNotificationManager.notify(id, mBuilder.build());
    }

    private String getLeftDateFromFile(String fileContentTemp) {

        String fileContent1[] = fileContentTemp.split("<titel>");
        String fileDateTemp = fileContent1[1];
        String fileContent2[] = fileDateTemp.split(" </titel>");
        String fileDate = fileContent2[0];

        String fileDateTempBoth[] = fileDate.split(", ");

        return fileDateTempBoth[0];
    }

    private String getFileContentTemp(File file) {
        fileContentTemp = getStringFromFile(file);
        return fileContentTemp;
    }

    private boolean isValidFile(File file) {
        String fileString = getStringFromFile(file);
        //Log.i(TAG, fileString);
        boolean isValid = false;
        if (fileString != null) {
            isValid = fileString.contains("<kopf>") || fileString.contains("<schulname>") || fileString.contains("Montag")
                    || fileString.contains("Dienstag") || fileString.contains("Mittwoch") || fileString.contains("Donnerstag")
                    || fileString.contains("Freitag");
        }
        //Log.i(TAG, "File '" + String.valueOf(file) + "' is valid: " + isValid);
        return isValid;
    }

    private boolean canPing() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Log.i(TAG, "Trying to ping IP address " + "8.8.8.8");
            Process process = runtime.exec("/system/bin/ping -c 1 " + "8.8.8.8");
            int exitValue = process.waitFor();
            Log.i(TAG, "Exit value: " + exitValue);
            if (exitValue == 0) {
                Log.i(TAG, "Ping was successful");
                return true;
            } else {
                return false;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "Failed to ping IP address " + "8.8.8.8");
        return false;
    }

    private File[] sortFileArrayLastModified(File[] files) {
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        return files;
    }

    private void startMediaScanner() {
        final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(new File(path));
        scanIntent.setData(contentUri);
        sendBroadcast(scanIntent);
        Log.i(TAG, "Media Scanner fired up!");
    }

    private boolean deleteFile(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                success = deleteFile(child);
            }
        }
        Log.i(TAG, "Deleted file '" + file + "'");
        boolean success = file.delete();
        startMediaScanner();
        return success;
    }
}


