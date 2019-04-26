package com.ov3rk1ll.kinocast.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.Toast;

import com.ov3rk1ll.kinocast.BuildConfig;
import com.ov3rk1ll.kinocast.R;
import com.ov3rk1ll.kinocast.api.KinoxParser;
import com.ov3rk1ll.kinocast.api.Parser;
import com.ov3rk1ll.kinocast.utils.Utils;
import com.winsontan520.wversionmanager.library.WVersionManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.ov3rk1ll.kinocast.api.Parser.PARSER_LIST;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        private EditTextPreference url_pref = null;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setupSimplePreferencesScreen();
        }

        private void setupSimplePreferencesScreen() {
            // In the simplified UI, fragments are not used at all and we instead
            // use the older PreferenceActivity APIs.

            // Add 'general' preferences.
            addPreferencesFromResource(R.xml.pref_general);


            findPreference("order_hostlist").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), OrderHostlistActivity.class));
                    return true;
                }
            });
            findPreference("order_parser").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), OrderParserActivity.class));
                    return true;
                }
            });

            findPreference("version_information").setSummary("v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
            findPreference("version_information").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(getActivity(), R.string.update_checking, Toast.LENGTH_SHORT).show();
                    WVersionManager versionManager = new WVersionManager(getActivity());
                    versionManager.setVersionContentUrl(getString(R.string.update_check));
                    versionManager.setShowToastIfUpToDate(true);
                    versionManager.checkVersion(true);
                    return true;
                }
            });
            Preference donate = findPreference("donate");
            if (Utils.isStringEmpty(getString(R.string.paypal_donate))) {
                PreferenceScreen pscreen = getPreferenceScreen();
                pscreen.removePreference(donate);
            } else {
                donate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.paypal_donate)));
                        startActivity(intent);
                        return true;
                    }
                });
            }
            url_pref = (EditTextPreference) findPreference("url");
            url_pref.setKey("url_" + String.valueOf(Parser.getInstance().getParserId()));

            ListPreference parser = (ListPreference) findPreference("parser");
            List<CharSequence> eT = new ArrayList<>();
            List<CharSequence> eV = new ArrayList<>();
            for (Class<?> h : PARSER_LIST) {
                try {
                    Parser pi = (Parser) h.getConstructor().newInstance();
                    eT.add(pi.getParserName());
                    eV.add(Integer.toString(pi.getParserId()));
                } catch (java.lang.InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            parser.setEntries(eT.toArray(new CharSequence[0]));
            parser.setEntryValues(eV.toArray(new CharSequence[0]));
            bindPreferenceSummaryToValue(parser);

            parser.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    url_pref.setKey("url_" + o.toString());
                    String prefurl = preferences.getString(url_pref.getKey(), "");
                    Parser.selectParser(getActivity(), Integer.parseInt(o.toString()), prefurl);
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, o);
                    url_pref.setText(prefurl);
                    url_pref.setSummary(prefurl);
                    return true;
                }
            });

            bindPreferenceSummaryToValue(url_pref);
            url_pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {

                    //prevent app from crashing with empty url
                    if (!o.toString().equalsIgnoreCase("") && !Patterns.WEB_URL.matcher(o.toString()).matches()) {
                        Toast.makeText(getActivity(), "Invalid URL", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    if(!o.toString().equalsIgnoreCase("")){
                        o = Parser.getInstance().PreSaveParserUrl(o.toString());
                    }
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, o);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    Parser.selectParser(getActivity(), Integer.parseInt(preferences.getString("parser", Integer.toString(KinoxParser.PARSER_ID))), o.toString());
                    return true;
                }
            });

            findPreference("allow_invalid_ssl").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Utils.DisableSSLCheck = (boolean) o;
                    return true;
                }
            });

            findPreference("chromecast_prefs").setEnabled(Utils.getCastContext(getActivity()) != null);

            // Add 'notifications' preferences, and a corresponding header.
        /*PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_notification);*/
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);

                } else if (preference instanceof RingtonePreference) {
                    // For ringtone preferences, look up the correct display value
                    // using RingtoneManager.
                    if (TextUtils.isEmpty(stringValue)) {
                        // Empty values correspond to 'silent' (no ringtone).
                        preference.setSummary(R.string.pref_ringtone_silent);

                    } else {
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                preference.getContext(), Uri.parse(stringValue));

                        if (ringtone == null) {
                            // Clear the summary if there was a lookup error.
                            preference.setSummary(null);
                        } else {
                            // Set the summary to reflect the new ringtone display
                            // name.
                            String name = ringtone.getTitle(preference.getContext());
                            preference.setSummary(name);
                        }
                    }

                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see #sBindPreferenceSummaryToValueListener
         */
        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }
}
