package de.blankedv.sx4control;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import static de.blankedv.sx4control.Constants.DEBUG;
import static de.blankedv.sx4control.Constants.KEY_IP;


public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {


    private EditTextPreference ipPref, portPref;
    //private ListPreference configFilenamePref, locosFilenamePref;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        ipPref = (EditTextPreference) getPreferenceScreen().findPreference(KEY_IP);
        //relFontSize = (ListPreference) getPreferenceScreen().findPreference(KEY_FONT_FACTOR);

    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        // Let's do something if a preference value changes
        if (key.equals(KEY_IP)) {
            ipPref.setSummary("= " + sharedPreferences.getString(KEY_IP, ""));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        Context context = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Setup the initial values
        ipPref.setSummary("= " + prefs.getString(KEY_IP, ""));

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }





}

