package org.droidstack.service;

import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Reputation;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.ReputationQuery;

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
	
	private long mLastRun;
	private SitesDatabase mDB;
	private Cursor mSites;
	private NotificationManager mNotifManager;
	private SharedPreferences mPreferences;
	
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
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (mPreferences.getLong(Const.PREF_NOTIF_LASTRUN, -1) == -1) {
			Log.d(Const.TAG, "No previous run");
			mPreferences.edit().putLong(Const.PREF_NOTIF_LASTRUN, System.currentTimeMillis()/1000).commit();
			stopSelf();
			return;
		}
		mLastRun = mPreferences.getLong(Const.PREF_NOTIF_LASTRUN, -1);
		mPreferences.edit().putLong(Const.PREF_NOTIF_LASTRUN, System.currentTimeMillis()/1000).commit();
		mNotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mDB = new SitesDatabase(this);
		mSites = mDB.getSites();
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
			mSites.moveToFirst();
			while (!mSites.isAfterLast()) {
				String endpoint = SitesDatabase.getEndpoint(mSites);
				String name = SitesDatabase.getName(mSites);
				int uid = (int) SitesDatabase.getUserID(mSites);
				String uname = SitesDatabase.getUserName(mSites);
				if (uid > 0) {
					try {
						StackWrapper api = new StackWrapper(endpoint, Const.APIKEY);
						User user = api.getUserById(uid);
						ReputationQuery query = new ReputationQuery();
						query.setFromDate(mLastRun).setIds(uid);
						List<Reputation> repChanges = api.getReputationByUserId(query);
						if (repChanges.size() > 0) {
							int posRep = 0;
							int negRep = 0;
							for (Reputation rep: repChanges) {
								posRep += rep.getPositiveRep();
								negRep += rep.getNegativeRep();
							}
							Intent notifIntent = new Intent(NotificationsService.this, ReputationActivity.class);
							String uri = "droidstack://reputation" +
								"?endpoint=" + Uri.encode(endpoint) +
								"&name=" + Uri.encode(name) +
								"&uid=" + String.valueOf(uid) +
								"&uname=" + Uri.encode(uname);
							notifIntent.setData(Uri.parse(uri));
							PendingIntent contentIntent = PendingIntent.getActivity(NotificationsService.this, 0, notifIntent, 0);
							Notification notif = new Notification(R.drawable.ic_notif_rep, "New rep changes on " + name, repChanges.get(0).getOnDate()*1000);
							String contentText = "";
							if (posRep > 0 && negRep > 0) contentText = "+" + posRep + " / -" + negRep;
							else if (posRep > 0) contentText = "+" + posRep;
							else contentText = "-" + negRep;
							contentText += ", new total: " + Const.longFormatRep(user.getReputation());
							notif.setLatestEventInfo(NotificationsService.this, name, contentText, contentIntent);
							notif.defaults |= Notification.DEFAULT_ALL;
							notif.flags |= Notification.FLAG_AUTO_CANCEL;
							mNotifManager.notify(endpoint, REP_ID, notif);
						}
					}
					catch (Exception e) {
						Log.e(Const.TAG, "Exception on " + endpoint, e);
					}
				}
				mSites.moveToNext();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mSites != null) mSites.close();
		if (mDB != null) mDB.close();
		Log.d(Const.TAG, "Releasing wakelock");
		mWakeLock.release();
	}

}
