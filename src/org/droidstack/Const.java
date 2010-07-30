package org.droidstack;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

public final class Const {
	
	public final static String TAG = "Droidstack";
	public final static String APIKEY = "8eE4X4E2LEGgAjNRfVePkg";
	
	public final static int NET_TIMEOUT = 30000;
	
	public final static String PREF_FILE = "preferences";
	public final static String PREF_PAGESIZE = "pagesize";
	public final static String DEF_PAGESIZE = "10";
	
	public static int getPageSize(Context ctx) {
		String items = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_PAGESIZE, DEF_PAGESIZE);
		return Integer.parseInt(items);
	}
	
}
