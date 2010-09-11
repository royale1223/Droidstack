package org.droidstack.activity;

import java.io.File;

import net.sf.stackwrap4j.http.HttpClient;

import org.droidstack.R;
import org.droidstack.adapter.SitesAdapter;
import org.droidstack.service.NotificationsService;
import org.droidstack.service.SitesService;
import org.droidstack.util.Const;
import org.droidstack.util.SitesDatabase;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SitesActivity extends ListActivity {
	
	private final static int CODE_PICK_USER = 1;
	
	private SitesDatabase mSitesDatabase;
	private Cursor mBookmarked;
	private Cursor mOthers;
	private SitesAdapter mAdapter;
	private File mIcons;
	private Messenger mService;
	
	private String mResultEndpoint;
	
	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case SitesService.MSG_UPDATE:
				mBookmarked.requery();
				mOthers.requery();
				mAdapter.notifyDataSetChanged();
			case SitesService.MSG_LOADING:
				mAdapter.setLoading(true);
				break;
			case SitesService.MSG_FINISHED:
				mAdapter.setLoading(false);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	}
	
	private Messenger mMessenger = new Messenger(new IncomingHandler());
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
			mAdapter.setLoading(false);
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = new Messenger(service);
			Message msg = Message.obtain(null, SitesService.MSG_REGISTER);
			msg.replyTo = mMessenger;
			try {
				mService.send(msg);
			}
			catch (RemoteException e) {
				Log.e(Const.TAG, "Could not send REGISTER message", e);
			}
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sites);

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	externalMediaError();
        }
        
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null || info.getState() != State.CONNECTED) {
        	networkError();
        }
        
        HttpClient.setTimeout(Const.NET_TIMEOUT);
        mSitesDatabase = new SitesDatabase(this);
        mBookmarked = mSitesDatabase.getBookmarkedSites();
        mOthers = mSitesDatabase.getOtherSites();
        startManagingCursor(mBookmarked);
        startManagingCursor(mOthers);
        
        mIcons = new File(Environment.getExternalStorageDirectory(), "/Android/data/org.droidstack/icons/");
        if (mIcons.mkdirs()) {
        	File noMedia = new File(mIcons, ".nomedia");
        	try {
        		noMedia.createNewFile();
        	}
        	catch (Exception e) {
        		Log.e(Const.TAG, ".nomedia creation error", e);
        		externalMediaError();
        	}
        }

        mAdapter = new SitesAdapter(this, mBookmarked, mOthers);
        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(onSiteClicked);
        registerForContextMenu(getListView());
        
        // start notification service on app update
        if (Const.getOldVersion(this) != Const.getNewVersion(this)) {
        	startService(new Intent(this, NotificationsService.class));
        	PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(Const.PREF_VERSION, Const.getNewVersion(this)).commit();
        }

        // start sites fetcher service
        bindService(new Intent(this, SitesService.class), mConnection, BIND_AUTO_CREATE);
    }
    
    private void externalMediaError() {
    	new AlertDialog.Builder(this)
		.setTitle(R.string.title_error)
		.setCancelable(false)
		.setMessage(R.string.no_sd_error)
		.setNeutralButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		})
		.create().show();
    }
    
    private void networkError() {
    	new AlertDialog.Builder(this)
		.setTitle(R.string.title_error)
		.setCancelable(false)
		.setMessage(R.string.no_network_error)
		.setNeutralButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		})
		.create().show();
    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		mSitesDatabase.dispose();
		
		if (mService != null) {
			try {
				Message msg = Message.obtain(null, SitesService.MSG_UNREGISTER);
				msg.replyTo = mMessenger;
				mService.send(msg);
			}
			catch (RemoteException e) {
				
			}
		}
		unbindService(mConnection);
	}

	private OnItemClickListener onSiteClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Object item = mAdapter.getItem(position);
			if (item instanceof Integer) return;
			Cursor site = (Cursor) item;
			String endpoint = site.getString(site.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			String name = site.getString(site.getColumnIndex(SitesDatabase.KEY_NAME));
			int uid = site.getInt(site.getColumnIndex(SitesDatabase.KEY_UID));
			String uname = site.getString(site.getColumnIndex(SitesDatabase.KEY_UNAME));
			Intent i = new Intent(SitesActivity.this, SiteActivity.class);
			String uri = "droidstack://site/" +
				"?endpoint=" + Uri.encode(endpoint) +
				"&name=" + Uri.encode(name) +
				"&uid=" + String.valueOf(uid) +
				"&uname=" + Uri.encode(uname);
			i.setData(Uri.parse(uri));
			startActivity(i);
		}
	};
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
    	if (v.getId() == android.R.id.list) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    		Object item = mAdapter.getItem(info.position);
    		if (item instanceof Integer) return;
    		Cursor site = (Cursor) item;
    		int resource;
    		if (item == mBookmarked) resource = R.menu.site_bookmarked;
    		else resource = R.menu.site_other;
    		String name = site.getString(site.getColumnIndex(SitesDatabase.KEY_NAME));
    		menu.setHeaderTitle(name);
    		getMenuInflater().inflate(resource, menu);
    	}
	}
    
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Object obj = mAdapter.getItem(info.position);
		if (obj instanceof Integer) return super.onContextItemSelected(item);
		Cursor site = (Cursor) obj;
		final String endpoint = site.getString(site.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
		switch(item.getItemId()) {
		case R.id.menu_set_user:
			Intent i = new Intent(this, UsersActivity.class);
			i.setAction(Intent.ACTION_PICK);
			String uri = "droidstack://users?endpoint=" + endpoint;
			i.setData(Uri.parse(uri));
			mResultEndpoint = endpoint;
			startActivityForResult(i, CODE_PICK_USER);
			return true;
		case R.id.menu_add:
			mSitesDatabase.bookmarkSite(endpoint);
			mBookmarked.requery();
			mOthers.requery();
			mAdapter.notifyDataSetChanged();
			return true;
		case R.id.menu_remove:
			mSitesDatabase.removeBookmark(endpoint);
			mBookmarked.requery();
			mOthers.requery();
			mAdapter.notifyDataSetChanged();
			return true;
		}
		return super.onContextItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode) {
		case CODE_PICK_USER:
			if (resultCode == RESULT_OK) {
				int uid = data.getIntExtra("uid", 0);
				String name = data.getStringExtra("name");
				mSitesDatabase.setUser(mResultEndpoint, uid, name);
				mBookmarked.requery();
				mOthers.requery();
				mAdapter.notifyDataSetChanged();
			}
			break;
		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.sites, menu);
    	return true;
    }
	
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_settings:
    		Intent i = new Intent(this, PreferencesActivity.class);
    		startActivity(i);
    	}
    	return false;
    }
    
}