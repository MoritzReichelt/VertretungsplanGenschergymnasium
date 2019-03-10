package de.reichelt.moritz.vertretungsplangenschergymnasium;

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
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import org.apache.commons.validator.routines.UrlValidator;

@SuppressWarnings("deprecation")
public class PrefsActivity extends AppCompatPreferenceActivity {

    private boolean preferenceChanged;

    private SharedPreferences mPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        setupActionBar();

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // show the current value in the settings screen
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            pickPreferenceObject(getPreferenceScreen().getPreference(i));
        }

        Preference preferenceStretch = findPreference(Constants.stretchScreenKey);
        preferenceStretch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                saveChangedValueToSharedPreferences(preference, newValue);
                preferenceChanged = true;
                return true;
            }
        });

        Preference preferenceCache = findPreference(Constants.cacheKey);
        preferenceCache.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                saveChangedValueToSharedPreferences(preference, newValue);
                preferenceChanged = true;
                return true;
            }
        });


        Preference preferenceURL = findPreference(Constants.urlKey);
        preferenceURL.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                UrlValidator urlValidator = new UrlValidator();

                if (urlValidator.isValid((String) newValue)) {
                    saveChangedValueToSharedPreferences(preference, newValue);
                    if (Methods.areNotificationsEnabled(getApplicationContext()))
                        Job.schedule(getApplicationContext(), getPackageName());
                    preferenceChanged = true;
                    return true;
                } else {
                    Toast.makeText(PrefsActivity.this, "Ungültige URL", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        Preference preferenceUsername = findPreference(Constants.usernameKey);
        preferenceUsername.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.length() == 0) {
                    Toast.makeText(PrefsActivity.this, "Ungültiger Benutzername", Toast.LENGTH_SHORT).show();
                    return false;
                } else {
                    saveChangedValueToSharedPreferences(preference, newValue);
                    if (Methods.areNotificationsEnabled(getApplicationContext()))
                        Job.schedule(getApplicationContext(), getPackageName());
                    preference.setSummary(mPreferences.getString("pref_username", "Genscher"));
                    preferenceChanged = true;
                    return true;
                }
            }
        });

        Preference preferencePassword = findPreference(Constants.passwordKey);
        preferencePassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String value = (String) newValue;
                if (value.length() == 0) {
                    Toast.makeText(PrefsActivity.this, "Ungültiges Passwort", Toast.LENGTH_SHORT).show();
                    return false;
                }
                saveChangedValueToSharedPreferences(preference, newValue);
                if (Methods.areNotificationsEnabled(getApplicationContext()))
                    Job.schedule(getApplicationContext(), getPackageName());
                preferenceChanged = true;
                return true;
            }
        });

        Preference preferenceNotif = findPreference(Constants.notificationsKey);
        preferenceNotif.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean enabled = (boolean) newValue;
                saveChangedValueToSharedPreferences(preference, newValue);
                if (enabled) {
                    Job.schedule(getApplicationContext(), getPackageName());
            } else

            {
                Job.cancel();
            }
                return true;
        }
    });

    Preference preferenceInterval = findPreference(Constants.syncIntervalKey);
        preferenceInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()

    {
        @Override
        public boolean onPreferenceChange (Preference preference, Object newValue){
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

    Preference preferenceNetworkType = findPreference(Constants.networkTypeKey);
        preferenceNetworkType.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()

    {
        @Override
        public boolean onPreferenceChange (Preference preference, Object newValue){
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


    /**
     * Called by the system when the user selects an item on the action bar.
     *
     * @param item Selected item
     * @return Boolean whether or not the click was handled by the user
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                if (preferenceChanged) {
                    reloadToMainActivity();
                } else {
                    onBackPressed();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Called by the system when a button or other key was pressed.
     * Overrides the default onKeyDown event so that the MainActivity can be reloaded when a preference
     * affecting the WebView changed.
     *
     * @param keyCode Provided by the system
     * @param event   Provided by the system, we just need it to detect if the back button was pressed
     *                (KeyEvent.KEYCODE_BACK)
     * @return In this case true because we have handled the onKeyDown event our self
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (preferenceChanged) {
                reloadToMainActivity();
            } else {
                onBackPressed();
            }
        }
        return true;
    }


    /**
     * Overrides the default back event for custom handling.
     */
    @Override
    public void onBackPressed() {
        if (preferenceChanged) {
            reloadToMainActivity();
        } else {
            super.onBackPressed();
        }
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
     * Starts an intent to the MainActivity from PrefsActivity so that the settings affecting
     * the WebView will take effect.
     */
    private void reloadToMainActivity() {
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
        if (key.equals(Constants.usernameKey) || key.equals(Constants.syncIntervalKey) || key.equals(Constants.networkTypeKey)) {
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
     * Saves a changed value to the SharedPreferences
     *
     * @param p        Preference to be used
     * @param newValue Value to be saved
     */
    private void saveChangedValueToSharedPreferences(Preference p, Object newValue) {
        SharedPreferences.Editor editor = mPreferences.edit();
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
