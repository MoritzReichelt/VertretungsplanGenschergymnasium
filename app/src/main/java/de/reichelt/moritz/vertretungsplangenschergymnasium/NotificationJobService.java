package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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

                    path = getFilesDir().toString() + "/plans/";

                /* Wenn noch kein Vergleichs-VPlan im Ordner ist, wird dieser erstellt und
                ein VPlan heruntergeladen (normalerweise nur nach Erstinstallation der Fall)
                 */
                    if (isDirEmpty(path)) {
                        Log.i(TAG, "Directory is empty");
                        createDir(path);
                        Methods.downloadPlan(getApplicationContext(), path);
                    }

                    deleteInvalidFilesIfPresent(path);

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

                    deleteInvalidFilesIfPresent(path);

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

                            fileContentTemp = Methods.getStringFromFile(file2);

                            if (fileContentTemp != null) {
                                String file1String = Methods.getStringFromFile(file1);
                                String file2String = Methods.getStringFromFile(file2);

                                String file1Date = getLeftDateFromFile(Objects.requireNonNull(file1String));
                                String file2Date = getLeftDateFromFile(Objects.requireNonNull(file2String));

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
                                Log.e(TAG, "Error: fileContentTemp is null, meaning there was an error" +
                                        "while reading the file into a string!");
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
                } // Google DNS konnte nicht angepingt werden
                else {
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


    /**
     * Calculates the MD5-Hash of a given file.
     *
     * @param updateFile The file of which the hash should be calculated
     * @return String containing the MD5-Hash
     */
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





    /**
     * Checks whether or not a directory is empty.
     *
     * @param path File path to the directory
     * @return Boolean indicating whether or not the directory is empty
     */
    private Boolean isDirEmpty(String path) {
        File f = new File(path);
        return !f.exists() || f.listFiles() == null;
    }


    /**
     * Creates a directory in a file path.
     *
     * @param filePath Path in which the new directory should be created
     */
    private void createDir(String filePath) {
        File f = new File(filePath);
        boolean successfullyCreatedDir = f.mkdir();
        Log.i(TAG, "Boolean succesfullyCreatedDir has value " + successfullyCreatedDir);
    }


    /**
     * Checks if there are any invalid files in the directory and - if yes - deletes them.
     *
     * @param path Path to the directory
     */
    private void deleteInvalidFilesIfPresent(String path) {
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


    /**
     * Sends a notification to the user informing him about a new vplan.
     *
     * @param fileContentTemp String containing the whole file. From this the date will be extracted
     *                        to be used in the notification
     */
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
        String channel_id = getString(R.string.notification_channel_id);
        int id = 1;
        CharSequence name = getString(R.string.changed_plan_name);
        String description = getString(R.string.new_plan_description);
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
                .setSmallIcon(R.drawable.ic_job_plan)
                .setContentTitle(getString(R.string.plan_notif_title))
                .setContentText(textSnippet)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

        assert mNotificationManager != null;
        mNotificationManager.notify(id, mBuilder.build());
        Log.i(TAG, "Sent notification for new plan! '" + bigText + "'");
    }


    /**
     * Sends a notification to the user informing him about an updated vplan.
     */
    private void sendNotifyWhenPlanChanged() {
        //PendingIntent sorgt dafür, dass die App startet, wenn die Benachrichtigung angeklickt wird
        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channel_id = getString(R.string.notification_channel_id);
        int id = 1;
        CharSequence name = getString(R.string.changed_plan_name);
        String description = getString(R.string.changed_plan_description);
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
        String text = getString(R.string.changed_plan_text);
        //Erstellt die eigentliche Benachrichtigung
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channel_id)
                .setSmallIcon(R.drawable.ic_job_plan)
                .setContentTitle(getString(R.string.plan_notif_title))
                .setContentText(text)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true);

        assert mNotificationManager != null;
        mNotificationManager.notify(id, mBuilder.build());
        Log.i(TAG, "Sent notification for changed plan!");
    }


    /**
     * Gets the left date from a vplan file, like 'Montag' or 'Dienstag'.
     *
     * @param fileContent String containing the file
     * @return String containing the left date from the file
     */
    private String getLeftDateFromFile(String fileContent) {

        String fileContent1[] = fileContent.split("<titel>");
        String fileDateTemp = fileContent1[1];
        String fileContent2[] = fileDateTemp.split(" </titel>");
        String fileDate = fileContent2[0];

        String fileDateBoth[] = fileDate.split(", ");

        return fileDateBoth[0];
    }


    /**
     * Checks whether or not a given file is a valid vplan. This is needed because when the user is
     * connected to a Fritz!Box and his internet access is restricted, the app would download an
     * .xml file containing the standard Fritz!Box message ('Kein Internetzugriff' etc.).
     * This extra step ensures that only valid vplans will be saved and used to compare.
     *
     * @param file The file to be checked
     * @return Boolean indicating whether or not the file is valid
     */
    private boolean isValidFile(File file) {
        String fileString = Methods.getStringFromFile(file);
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


    /**
     * Another safety method - checks whether or not the Google DNS can be pinged. This is needed
     * because sometimes Android thinks the device is connected to a network and runs the job, even
     * if it does not really have internet access.
     *
     * @return Boolean indicating whether or not Google DNS could be reached
     */
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


    /**
     * Sorts a given file array so that the oldest file is at the beginning and the newest at the end.
     *
     * @param files File array to be sorted
     * @return Sorted file array
     */
    private File[] sortFileArrayLastModified(File[] files) {
        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        return files;
    }


    /**
     * Starts the Media Scanner built into the Android system. This is needed because after deleting
     * a file, Android keeps a temporary placeholder of this file. The media scanner will delete these
     * placeholders.
     */
    private void startMediaScanner() {
        final Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(new File(path));
        scanIntent.setData(contentUri);
        sendBroadcast(scanIntent);
        Log.i(TAG, "Media Scanner fired up!");
    }


    /**
     * Deletes a file recursively.
     *
     * @param file File to be deleted
     * @return Boolean indicating whether or not the deletion was successful
     */
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


