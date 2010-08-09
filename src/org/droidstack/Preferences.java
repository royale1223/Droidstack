package org.droidstack;

import org.droidstack.utils.Const;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Preferences extends PreferenceActivity {
	
	private Context mContext;
	private SharedPreferences mPreferences;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		mContext = (Context) this;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		ListPreference interval = (ListPreference) findPreference(Const.PREF_NOTIF_INTERVAL);
		interval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				mPreferences.edit().putString(Const.PREF_NOTIF_INTERVAL, (String)newValue).commit();
				int minutes = Integer.parseInt((String)newValue);
				AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
				Intent i = new Intent(mContext, NotificationService.class);
				PendingIntent pi = PendingIntent.getService(mContext, 0, i, 0);
				am.cancel(pi);
				if (minutes > 0) {
					startService(i);
				}
				return false;
			}
		});
	}
	
}
