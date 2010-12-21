package org.droidstack.service;

import java.util.ArrayList;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;

import org.droidstack.R;
import org.droidstack.activity.ReputationActivity;
import org.droidstack.util.Const;
import org.droidstack.util.SitesDatabase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationsService extends Service {
	
	private static final int REP_ID = 1;
	
	private WakeLock mWakeLock;
	
	private final ArrayList<NotificationWrapper> notifications = new ArrayList<NotificationWrapper>();
	private SharedPreferences mPreferences;
	private NotificationManager mNotifManager;
	
	private boolean rep;
	private String sound;
	private boolean light;
	private boolean vibrate;
	
	private static class NotificationWrapper {
		public final Notification n;
		public final String tag;
		public final int id;
		public NotificationWrapper(Notification n, String tag, int id) {
			this.n = n;
			this.tag = tag;
			this.id = id;
		}
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		handleIntent(intent);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);
		return START_NOT_STICKY;
	}
	
	private void handleIntent(Intent intent) {
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Const.TAG);
		mWakeLock.acquire();
		Log.d(Const.TAG, "Wakelock acquired");
		
		final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		if (!cm.getBackgroundDataSetting()) {
			Log.d(Const.TAG, "Background data setting is OFF");
			stopSelf();
			return;
		}
        final NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || info.getState() != State.CONNECTED) {
        	Log.d(Const.TAG, "No active network connection");
        	stopSelf();
        	return;
        }
		
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        rep = mPreferences.getBoolean(Const.PREF_NOTIF_REP, false);
        if (!rep) {
        	Log.i(Const.TAG, "No notifications activated");
        	stopSelf();
        	return;
        }
        sound = mPreferences.getString(Const.PREF_NOTIF_SOUND, Const.DEF_NOTIF_SOUND);
        light = mPreferences.getBoolean(Const.PREF_NOTIF_LIGHT, Const.DEF_NOTIF_LIGHT);
        vibrate = mPreferences.getBoolean(Const.PREF_NOTIF_VIBRATE, Const.DEF_NOTIF_VIBRATE);
        
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mNotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		new WorkerTask().execute();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private class WorkerTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(Const.TAG, "Working...");
			if (rep) {
				SitesDatabase db = new SitesDatabase(NotificationsService.this);
				Cursor sites = db.getSites();
				sites.moveToFirst();
				while (!sites.isAfterLast()) {
					String endpoint = SitesDatabase.getEndpoint(sites);
					String name = SitesDatabase.getName(sites);
					int uid = (int) SitesDatabase.getUserID(sites);
					String uname = SitesDatabase.getUserName(sites);
					if (uid > 0) {
						try {
							StackWrapper api = new StackWrapper(endpoint, Const.APIKEY);
							User user = api.getUserById(uid);
							int newRep = user.getReputation();
							int oldRep = SitesDatabase.getReputation(sites);
							int diffRep = newRep - oldRep;
							if (diffRep != 0) {
								Intent notifIntent = new Intent(NotificationsService.this, ReputationActivity.class);
								String uri = "droidstack://reputation" +
									"?endpoint=" + Uri.encode(endpoint) +
									"&name=" + Uri.encode(name) +
									"&uid=" + String.valueOf(uid) +
									"&uname=" + Uri.encode(uname);
								notifIntent.setData(Uri.parse(uri));
								PendingIntent contentIntent = PendingIntent.getActivity(NotificationsService.this, 0, notifIntent, 0);
								Notification notif = new Notification(R.drawable.ic_notif_rep, "Reputation on " + name + ": " + newRep, System.currentTimeMillis());
								String contentText = "New reputation: " + Const.longFormatRep(newRep);
								notif.setLatestEventInfo(NotificationsService.this, name, contentText, contentIntent);
								notif.flags |= Notification.FLAG_AUTO_CANCEL;
								if (sound == null || sound.equals(Const.DEF_NOTIF_SOUND)) {
									notif.defaults |= Notification.DEFAULT_SOUND;
								}
								else notif.sound = Uri.parse(sound);
								if (light) notif.defaults |= Notification.DEFAULT_LIGHTS;
								if (vibrate) notif.defaults |= Notification.DEFAULT_VIBRATE;
								notifications.add(new NotificationWrapper(notif, endpoint, REP_ID));
								db.setReputation(endpoint, newRep);
							}
						}
						catch (Exception e) {
							Log.e(Const.TAG, "Exception on " + endpoint, e);
						}
					}
					sites.moveToNext();
				}
				sites.close();
				db.close();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			for (NotificationWrapper notif: notifications) {
				mNotifManager.notify(notif.tag, notif.id, notif.n);
			}
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(Const.TAG, "Releasing wakelock");
		mWakeLock.release();
	}

}
