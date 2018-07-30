package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

public class AboutActivity extends AppCompatActivity {
    String logPath;
    String zipPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        logPath = "/storage/emulated/0/Android/data/" + getPackageName() + "/Logs";
        zipPath = "storage/emulated/0/Android/data/" + getPackageName() + "/Zips";

        File logDir = new File(logPath);
        final File[] logFiles = logDir.listFiles();
        final String[] filesString = new String[0];
/*
        if (logFiles.length > 0) {

            for (int i = 0; i < logFiles.length; i++) {
                filesString[i] = logFiles[i].toString();
            }

            Button crashButton = findViewById(R.id.buttonSendLog);
            crashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    File zipDir = new File(zipPath);
                    if (!zipDir.exists()) {
                        zipDir.mkdir();
                    }
                    String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
                    currentDateTime = currentDateTime.replaceAll("\\s+", "_");
                    currentDateTime = currentDateTime.replaceAll(":", "_");

                    Zipper.zip(filesString, zipPath + currentDateTime);
                }
            });

        }
        */
    }
}