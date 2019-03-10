package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class LoginActivity extends AppCompatActivity {

    private SharedPreferences mPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button loginConfirm = findViewById(R.id.loginConfirm);
        final EditText loginUsername = findViewById(R.id.loginUsername);
        final EditText loginPassword = findViewById(R.id.loginPassword);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        assert imm != null;
        imm.hideSoftInputFromWindow(loginUsername.getWindowToken(),0);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        loginConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username = loginUsername.getText().toString();
                String password = loginPassword.getText().toString();
                if (username.length() == 0 || password.length() == 0){
                    Toast.makeText(LoginActivity.this, "Ungültige Eingabe", Toast.LENGTH_SHORT).show();
                } else {
                    editor = mPreferences.edit();
                    editor.putString(Constants.usernameKey, username);
                    editor.putString(Constants.passwordKey, password);
                    editor.putBoolean(Constants.firstStartKey, false);
                    editor.apply();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        System.exit(0);
    }
}
