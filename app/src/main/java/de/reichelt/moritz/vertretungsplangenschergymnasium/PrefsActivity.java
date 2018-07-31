package de.reichelt.moritz.vertretungsplangenschergymnasium;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.apache.commons.validator.routines.UrlValidator;

import static android.support.constraint.Constraints.TAG;

@SuppressWarnings("deprecation")
public class PrefsActivity extends AppCompatPreferenceActivity {

    private boolean preferenceChange;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Logging.start(Objects.requireNonNull(getExternalFilesDir(null)).toString());

        addPreferencesFromResource(R.xml.preferences);
        setupActionBar();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = preferences.edit();

        // show the current value in the settings screen
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            pickPreferenceObject(getPreferenceScreen().getPreference(i));
        }

        Preference preferenceStretch = findPreference("pref_stretch");
        preferenceStretch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                saveChangedValueToSharedPreferences(preference, newValue);
                preferenceChange = true;
                return true;
            }
        });

        Preference preferenceURL = findPreference("pref_url");
        preferenceURL.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.i(TAG, "URL has changed, calling isURLValid()...");

                UrlValidator urlValidator = new UrlValidator();

                if (urlValidator.isValid((String) newValue)) {
                    saveChangedValueToSharedPreferences(preference, newValue);
                    preferenceChange = true;
                    return true;
                } else {
                    editor.putString("pref_url", getString(R.string.pref_url_default));
                    editor.apply();
                    Toast.makeText(PrefsActivity.this, "Ungültige URL", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        Preference preferenceUsername = findPreference("pref_username");
        preferenceUsername.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.length() == 0) {
                    Toast.makeText(PrefsActivity.this, "Ungültiger Benutzername", Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    saveChangedValueToSharedPreferences(preference, newValue);
                    preference.setSummary(preferences.getString("pref_username", "Genscher"));
                    preferenceChange = true;
                    return true;
                }
            }
        });

        Preference preferencePassword = findPreference("pref_password");
        preferencePassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.length() == 0) {
                    Toast.makeText(PrefsActivity.this, "Ungültiges Passwort", Toast.LENGTH_SHORT).show();
                    return false;
                }
                saveChangedValueToSharedPreferences(preference, newValue);
                preferenceChange = true;
                return true;
            }
        });

        Preference preferenceNotif = findPreference("pref_notifications_enabled");
        preferenceNotif.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (boolean) newValue;
                saveChangedValueToSharedPreferences(preference, newValue);
                if (enabled) {
                    Job.schedule(getApplicationContext(), getPackageName());
                } else {
                    Job.schedule(getApplicationContext(), getPackageName());
                    Job.cancel();
                }
                return true;
            }
        });

        Preference preferenceInterval = findPreference("pref_sync_interval");
        preferenceInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.valueOf(newValue.toString());
                saveChangedValueToSharedPreferences(preference, newValue);
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(newValue.toString());

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

                Job.schedule(getApplicationContext(), getPackageName());

                float usedDataTemp = 8 * (8 / ((float) value / 60));
                int usedData = (int) usedDataTemp;

                Toast.makeText(PrefsActivity.this, "Dies wird ca. " + String.valueOf(usedData)
                        + " KByte an Daten pro Tag verbrauchen, wenn du dein Handy 8h benutzt.", Toast.LENGTH_LONG).show();
                return true;
            }
        });

        Preference preferenceNetworkType = findPreference("pref_network_type");
        preferenceNetworkType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                saveChangedValueToSharedPreferences(preference, newValue);
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(newValue.toString());

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

                Job.schedule(getApplicationContext(), getPackageName());

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pref_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.resetSettings:
                fireAlertDialog();
            case android.R.id.home:
                if (preferenceChange) {
                    restartActivity();
                } else {
                    onBackPressed();
                }
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Sets up the action bar that the user can see at the top of the screen.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    /**
     * Shows an Alert Dialog to the user in which he can reset the settings or cancel that action.
     */
    private void fireAlertDialog() {
        if (!isFinishing()) {
            AlertDialog.Builder builder;

            builder = new AlertDialog.Builder(this);

            builder.setTitle("Einstellungen zurücksetzen")
                    .setMessage("Bist du sicher, dass du die Einstellungen zurücksetzen möchtest?")
                    .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            resetSettings();
                            recreate();
                        }
                    })
                    .setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }


    /**
     * Resets the settings to the default values.
     */
    private void resetSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    //Wendet die vorgenommenen Einstellungen an, indem es die MainActivity bei verändertern Einstellungen neu startet
    //Bei unveränderten Einstellungen wird MainActivity ohne Neustart geladen
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (preferenceChange) {
                restartActivity();
            } else {
                onBackPressed();
            }
        }
        return true;
    }


    /**
     * Starts an intent to the MainActivity from PrefsActivity so that the settings affecting
     * the WebView will take effect.
     */
    private void restartActivity() {
        Intent intent = new Intent(PrefsActivity.this, MainActivity.class);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }


    /**
     * Recursively picks a preference object so that the current value can be seen in the summary.
     *
     * @param p The preference category
     */
    private void pickPreferenceObject(Preference p) {
        if (p instanceof PreferenceCategory) {
            PreferenceCategory cat = (PreferenceCategory) p;
            for (int i = 0; i < cat.getPreferenceCount(); i++) {
                pickPreferenceObject(cat.getPreference(i));
            }
        } else {
            initSummary(p);
        }
    }


    /**
     * Sets the summary of a preference so that it reflects its current value.
     *
     * @param p The preference of which the summary should be shown
     */
    private void initSummary(Preference p) {
        String key = p.getKey();
        if (key.equals("pref_username") || key.equals("pref_sync_interval") || key.equals("pref_network_type")) {
            if (p instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                p.setSummary(editTextPref.getText());
            } else if (p instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) p;
                int index = listPreference.findIndexOfValue(((ListPreference) p).getValue());
                p.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            }
        }
    }


    /**
     * Saves a changed value to the Shared Preferences
     *
     * @param p        Preference to be used
     * @param newValue Value to be saved
     */
    private void saveChangedValueToSharedPreferences(Preference p, Object newValue) {
        String key = p.getKey();
        if (p instanceof SwitchPreference) {
            editor.putBoolean(key, (boolean) newValue);
        } else if (p instanceof EditTextPreference) {
            String editText = String.valueOf(newValue);
            editor.putString(key, editText);
        } else if (p instanceof ListPreference) {
            int newInt = Integer.valueOf(newValue.toString());
            editor.putString(key, String.valueOf(newInt));
        }
        editor.apply();
    }
}
