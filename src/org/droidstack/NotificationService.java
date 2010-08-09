package org.droidstack;

import java.util.List;

import org.droidstack.utils.Const;
import org.droidstack.utils.SitesDatabase;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Reputation;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.ReputationQuery;
import net.sf.stackwrap4j.utils.StackUtils;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class NotificationService extends Service {
	
	private static final int REP_ID = 1;
	
	private int mInterval;
	private long mLastRun;
	private SitesDatabase mDB;
	private Cursor mSites;
	private Context mContext;
	private NotificationManager mNotifManager;
	private SharedPreferences mPreferences;
	
	@Override
	public void onCreate() {
		Log.d(Const.TAG, "NotificationService started");
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mContext = (Context) this;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
		mInterval = Integer.parseInt(mPreferences.getString(Const.PREF_NOTIF_INTERVAL, Const.DEF_NOTIF_INTERVAL));
		if (mInterval == 0) {
			stopSelf();
			return;
		}
		if (mPreferences.getLong(Const.PREF_NOTIF_LASTRUN, -1) == -1) {
			Log.d(Const.TAG, "NotificationService: no previous run");
			mPreferences.edit().putLong(Const.PREF_NOTIF_LASTRUN, System.currentTimeMillis()/1000).commit();
			setupNextRun();
			stopSelf();
			return;
		}
		mLastRun = mPreferences.getLong(Const.PREF_NOTIF_LASTRUN, -1);
		mPreferences.edit().putLong(Const.PREF_NOTIF_LASTRUN, System.currentTimeMillis()/1000).commit();
		mNotifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mDB = new SitesDatabase(mContext);
		mSites = mDB.getSites();
		new WorkerTask().execute();
	}
	
	private void setupNextRun() {
		if (mInterval == 0) return;
		AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent i = new Intent(mContext, NotificationService.class);
		PendingIntent pi = PendingIntent.getService(mContext, 0, i, 0);
		Log.d(Const.TAG, "NotificationService: starting again in " + mInterval + " minutes");
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + mInterval*60*1000, pi);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	private class WorkerTask extends AsyncTask<Void, Void, Void> {
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(Const.TAG, "NotificationService working");
			mSites.moveToFirst();
			while (!mSites.isAfterLast()) {
				String endpoint = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
				String name = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_NAME));
				int uid = mSites.getInt(mSites.getColumnIndex(SitesDatabase.KEY_UID));
				String uname = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_UNAME));
				if (uid > 0) {
					try {
						StackWrapper api = new StackWrapper(endpoint, Const.APIKEY);
						User user = api.getUserById(uid);
						ReputationQuery query = new ReputationQuery();
						query.setFromDate(mLastRun).setIds(uid);
						List<Reputation> repChanges = api.getReputationByUserId(query);
						if (repChanges.size() > 0) {
							Log.d(Const.TAG, "NotificationService: new rep changes on " + endpoint);
							int posRep = 0;
							int negRep = 0;
							for (Reputation rep: repChanges) {
								posRep += rep.getPositiveRep();
								negRep += rep.getNegativeRep();
							}
							Intent notifIntent = new Intent(mContext, SiteActions.class);
							String uri = "droidstack://site/" +
								"?endpoint=" + Uri.encode(endpoint) +
								"&name=" + Uri.encode(name) +
								"&uid=" + String.valueOf(uid) +
								"&uname=" + Uri.encode(uname);
							notifIntent.setData(Uri.parse(uri));
							PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notifIntent, 0);
							Notification notif = new Notification(R.drawable.ic_notif_rep, "New rep changes on " + name, repChanges.get(0).getOnDate()*1000);
							String contentText = "";
							if (posRep > 0 && negRep > 0) contentText = "+" + posRep + " / -" + negRep;
							else if (posRep > 0) contentText = "+" + posRep;
							else contentText = "-" + negRep;
							contentText += ", new total: " + Const.longFormatRep(user.getReputation());
							notif.setLatestEventInfo(mContext, name, contentText, contentIntent);
							notif.defaults |= Notification.DEFAULT_ALL;
							notif.flags |= Notification.FLAG_AUTO_CANCEL;
							mNotifManager.notify(endpoint, REP_ID, notif);
						}
					}
					catch (Exception e) {
						Log.e(Const.TAG, "NotificationService: exception on " + endpoint, e);
					}
				}
				mSites.moveToNext();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			Log.d(Const.TAG, "NotificationService finished");
			mSites.close();
			mDB.dispose();
			setupNextRun();
			stopSelf();
		}
	}

}
