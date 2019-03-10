package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AboutActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        Button viewLicense = findViewById(R.id.viewLicense);
        viewLicense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewLicense = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.txt"));
                startActivity(viewLicense);
            }
        });

        Button viewCode = findViewById(R.id.viewCode);
        viewCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewCode = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/StardOva/VertretungsplanGenschergymnasium/blob/master/README.md"));
                startActivity(viewCode);
            }
        });

    }
}