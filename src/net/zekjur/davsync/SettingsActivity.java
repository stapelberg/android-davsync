package net.zekjur.davsync;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;

// XXX: I am aware of the fact that the EditTextPreferences don’t display a
// meaningful summary. This seems to be possible, see
// http://stackoverflow.com/questions/531427/how-do-i-display-the-current-value-of-an-android-preference-in-the-preference-su
// However, I think it is overly complicated and I’m not motivated to test
// this app on a tablet. Patches welcome.

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = false;
	
	private EditTextPreference webdavUrlPreference;
	private EditTextPreference webdavUserPreference;
	private EditTextPreference webdavPwdPreference;
	
	private CheckBoxPreference autoSyncCameraPictures;
	private CheckBoxPreference autoSyncCameraVideos;
	private CheckBoxPreference autoSyncOnWifiOnly;

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
		
		webdavUrlPreference = (EditTextPreference) getPreferenceScreen().findPreference("webdav_url");
		webdavUserPreference = (EditTextPreference) getPreferenceScreen().findPreference("webdav_user");
		webdavPwdPreference = (EditTextPreference) getPreferenceScreen().findPreference("webdav_password");
		
		autoSyncCameraPictures = (CheckBoxPreference) getPreferenceScreen().findPreference("auto_sync_camera_pictures");
		autoSyncCameraVideos = (CheckBoxPreference) getPreferenceScreen().findPreference("auto_sync_camera_videos");
		autoSyncOnWifiOnly = (CheckBoxPreference) getPreferenceScreen().findPreference("auto_sync_on_wifi_only");
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
		
		webdavUrlPreference.setSummary(sharedPreferences.getString("webdav_url", ""));
		if (sharedPreferences.getString("webdav_url", "").equals(""))
		{
			setEnabledAutoSyncOptions(false);
			
			autoSyncCameraPictures.setChecked(false);
			autoSyncCameraVideos.setChecked(false);
			autoSyncOnWifiOnly.setChecked(false);
		}
		else
		{
			setEnabledAutoSyncOptions(true);
		}
		webdavUserPreference.setSummary(sharedPreferences.getString("webdav_user", ""));
		if (sharedPreferences.getString("webdav_password", "").equals("")) {
			webdavPwdPreference.setSummary("");
		} else {
			webdavPwdPreference.setSummary("****");
		}
		
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);	
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("webdav_url")) {
			if (sharedPreferences.getString("webdav_url", "").equals(""))
			{
				setEnabledAutoSyncOptions(false);
				
				autoSyncCameraPictures.setChecked(false);
				autoSyncCameraVideos.setChecked(false);
				autoSyncOnWifiOnly.setChecked(false);
			}
			else
			{
				setEnabledAutoSyncOptions(true);
			}
			webdavUrlPreference.setSummary(sharedPreferences.getString("webdav_url", ""));
		} 
		else if (key.equals("webdav_user")) {
			webdavUserPreference.setSummary(sharedPreferences.getString("webdav_user", ""));
		}
		else if (key.equals("webdav_password")) {
			if (sharedPreferences.getString("webdav_password", "").equals("")) {
				webdavPwdPreference.setSummary("");
			} else {
				webdavPwdPreference.setSummary("****");
			}
		}
	}
	
	public void setEnabledWebdavOptions(boolean enabled)
	{
		webdavUrlPreference.setEnabled(enabled);
		webdavUserPreference.setEnabled(enabled);
		webdavPwdPreference.setEnabled(enabled);
	}
	
	public void setEnabledAutoSyncOptions(boolean enabled)
	{
		autoSyncCameraPictures.setEnabled(enabled);
		autoSyncCameraVideos.setEnabled(enabled);
		autoSyncOnWifiOnly.setEnabled(enabled);
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	@SuppressWarnings("deprecation")
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		addPreferencesFromResource(R.xml.pref_general);
		// Add 'data and sync' preferences, and a corresponding header.
		PreferenceCategory fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_autosync);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_autosync);
	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * This fragment shows notification preferences only. It is used when the
	 * activity is showing a two-pane settings UI.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class AutoSyncPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.pref_autosync);
		}
	}

}
