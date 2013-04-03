package net.zekjur.davsync;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class GeneralPreferenceFragment extends PreferenceFragment{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref_general);

		EditTextPreference webdav_url = (EditTextPreference) findPreference("webdav_url");
		webdav_url.setSummary(webdav_url.getText());
		webdav_url.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((CharSequence) newValue);
				return true;
			}
		});


		EditTextPreference webdav_user = (EditTextPreference) findPreference("webdav_user");
		webdav_user.setSummary(webdav_user.getText());
		webdav_user.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				preference.setSummary((CharSequence) newValue);
				return true;
			}
		});
	}
}
