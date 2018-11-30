package de.blankedv.sx4control

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.preference.EditTextPreference
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

class Preferences : PreferenceActivity(), OnSharedPreferenceChangeListener {


    private var ipPref: EditTextPreference? = null
    private val portPref: EditTextPreference? = null
    //private ListPreference configFilenamePref, locosFilenamePref;


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.preferences)

        ipPref = preferenceScreen.findPreference(KEY_IP) as EditTextPreference
        //relFontSize = (ListPreference) getPreferenceScreen().findPreference(KEY_FONT_FACTOR);

    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        key: String
    ) {

        // Let's do something if a preference value changes
        if (key == KEY_IP) {
            ipPref!!.summary = "= " + sharedPreferences.getString(KEY_IP, "")!!
        }

    }

    override fun onResume() {
        super.onResume()

        val context = applicationContext
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        // Setup the initial values
        ipPref!!.summary = "= " + prefs.getString(KEY_IP, "")!!

        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()

        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }


}

