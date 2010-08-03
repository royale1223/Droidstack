package org.droidstack;

import android.content.Context;
import android.preference.PreferenceManager;

public final class Const {
	
	public final static String TAG = "Droidstack";
	public final static String APIKEY = "8eE4X4E2LEGgAjNRfVePkg";
	
	public final static int NET_TIMEOUT = 30000;
	
	public final static String PREF_FILE = "preferences";
	public final static String PREF_PAGESIZE = "pagesize";
	public final static String DEF_PAGESIZE = "10";
	
	public final static String PREF_NOTIF_INTERVAL = "notif_interval";
	public final static String DEF_NOTIF_INTERVAL = "0";
	
	public final static String PREF_NOTIF_REP = "notif_rep";
	public final static boolean DEF_NOTIF_REP = true;
	
	public final static String PREF_NOTIF_LASTRUN = "notif_lastrun";
	
	public final static String PREF_FONTSIZE = "fontsize";
	public final static String DEF_FONTSIZE = "1em";
	
	public static int getPageSize(Context ctx) {
		String items = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_PAGESIZE, DEF_PAGESIZE);
		return Integer.parseInt(items);
	}
	
}
