package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.os.Environment;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Logging {
    public static void start(String packageName, String externalStorage){
        if ( isExternalStorageWritable() ) {

            File appDirectory = new File(externalStorage);
            File logFile = new File( appDirectory, "log" + Methods.getCurrentDateTime() + ".txt" );

            // create app folder
            if ( !appDirectory.exists() ) {
                appDirectory.mkdir();
            }

            //Reduces amount of logs to 50 if there are too many

            int amountOfLogs = logFile.listFiles().length;

            if (amountOfLogs > 100){
                File[] allLogs = logFile.listFiles();
                Arrays.sort(allLogs, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
                Methods.reduceDirContent(allLogs,50);
            }

            // clear the previous logcat and then write the new one to the file
            try {
                Process process = Runtime.getRuntime().exec("logcat -c");
                process = Runtime.getRuntime().exec("logcat -s ContentValues *:E -f  " + logFile);
            } catch ( IOException e ) {
                e.printStackTrace();
            }

        } else if ( isExternalStorageReadable() ) {
            // only readable
        } else {
            // not accessible
        }
    }

    /* Checks if external storage is available for read and write */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    private static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
