package org.droidstack.receiver;

import org.droidstack.service.NotificationsService;
import org.droidstack.util.Const;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	int minutes = 0;
    	try {
    		minutes = Integer.parseInt(prefs.getString(Const.PREF_NOTIF_INTERVAL, Const.DEF_NOTIF_INTERVAL));
    	}
    	catch (NumberFormatException e) { }
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, NotificationsService.class);
		PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		am.cancel(pi);
    	if (minutes > 0) {
    		am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
    				SystemClock.elapsedRealtime() + minutes*60*1000,
    				minutes*60*1000, pi);
    		Log.d(Const.TAG, "AlarmManager set");
    	}
	}

}
