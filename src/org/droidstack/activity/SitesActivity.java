package org.droidstack.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Stats;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.stackauth.StackAuth;
import net.sf.stackwrap4j.stackauth.entities.Site;

import org.droidstack.R;
import org.droidstack.adapter.SitesAdapter;
import org.droidstack.service.NotificationsService;
import org.droidstack.util.Const;
import org.droidstack.util.SitesDatabase;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SitesActivity extends ListActivity {
	
	private final static int CODE_PICK_USER = 1;
	
	private SitesDatabase mSitesDatabase;
	private Cursor mSites;
	private SitesAdapter mAdapter;
	private File mIcons;
	
	private String mResultEndpoint;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sites);
        
        HttpClient.setTimeout(Const.NET_TIMEOUT);
        mSitesDatabase = new SitesDatabase(this);
        mSites = mSitesDatabase.getSites();
        startManagingCursor(mSites);
        
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	externalMediaError();
        }
        
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

        mAdapter = new SitesAdapter(this, mSites, mIcons);
        setListAdapter(mAdapter);
        getListView().setOnItemClickListener(onSiteClicked);
        registerForContextMenu(getListView());
        
        ArrayList<String> missing = new ArrayList<String>();
        mSites.moveToFirst();
        while (!mSites.isAfterLast()) {
        	String endpoint = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
        	File icon = new File(mIcons, Uri.parse(endpoint).getHost());
        	if (!icon.exists()) {
        		missing.add(endpoint);
        	}
        	mSites.moveToNext();
        }
        if (missing.size() != 0) {
        	new FetchMissingIconsTask().execute(missing);
        }
        
        // start notification service on app update
        if (Const.getOldVersion(this) != Const.getNewVersion(this)) {
        	startService(new Intent(this, NotificationsService.class));
        	PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(Const.PREF_VERSION, Const.getNewVersion(this)).commit();
        }
        
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
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		mSites.close();
		mSitesDatabase.dispose();
	}

	private OnItemClickListener onSiteClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mSites.moveToPosition(position);
			String endpoint = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			String name = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_NAME));
			int uid = mSites.getInt(mSites.getColumnIndex(SitesDatabase.KEY_UID));
			String uname = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_UNAME));
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
    		mSites.moveToPosition(info.position);
    		String name = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_NAME));
    		menu.setHeaderTitle(name);
    		getMenuInflater().inflate(R.menu.sites_context, menu);
    	}
	}
    
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mSites.moveToPosition(info.position);
		final String endpoint = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
		switch(item.getItemId()) {
		case R.id.menu_set_user:
			Intent i = new Intent(this, UsersActivity.class);
			i.setAction(Intent.ACTION_PICK);
			String uri = "droidstack://users?endpoint=" + endpoint;
			i.setData(Uri.parse(uri));
			mResultEndpoint = endpoint;
			startActivityForResult(i, CODE_PICK_USER);
			return true;
		case R.id.menu_remove:
			mSitesDatabase.removeSite(endpoint);
			mSites.requery();
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
				mSites.requery();
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
    	case R.id.menu_add_site:
    		new SitePickerTask().execute();
    		break;
    	case R.id.menu_settings:
    		Intent i = new Intent(this, PreferencesActivity.class);
    		startActivity(i);
    	}
    	return false;
    }
    
    private class FetchMissingIconsTask extends AsyncTask<List<String>, Void, Void> {
    	
    	private Exception mException;
    	private ProgressDialog progressDialog;
    	
    	@Override
    	protected void onPreExecute() {
			progressDialog = ProgressDialog.show(SitesActivity.this, "", getString(R.string.loading), true, false);
    	}

		@Override
		protected Void doInBackground(List<String>... params) {
			List<String> endpoints = params[0];
			try {
				for (String endpoint: endpoints) {
					StackWrapper api = new StackWrapper(endpoint, Const.APIKEY);
					Stats stats = api.getStats();
					InputStream in = new URL(stats.getSite().getIconUrl()).openStream();
					OutputStream out = new FileOutputStream(new File(mIcons, Uri.parse(endpoint).getHost()));
					byte[] buf = new byte[1024];
					int len;
					while ((len = in.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
					in.close();
					out.close();
				}
			}
			catch(Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void params) {
			progressDialog.dismiss();
			if (mException != null) {
				Log.e(Const.TAG, "Error refreshing site icons", mException);
				new AlertDialog.Builder(SitesActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.icons_refresh_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					})
					.create().show();
			}
			else {
				mSites.requery();
				mAdapter.notifyDataSetChanged();
			}
		}
    }
    
    private class SetUserIDTask extends AsyncTask<Void, Void, User> {
    	
    	private final int mUserID;
    	private final String mEndpoint;
    	private Exception mException;
    	private ProgressDialog progressDialog;
    	
    	public SetUserIDTask(String endpoint, int userID) {
    		super();
    		mEndpoint = endpoint;
    		mUserID = userID;
    	}
    	
    	@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(SitesActivity.this, "", getString(R.string.loading), true, false);
		}
    	
		@Override
		protected User doInBackground(Void... params) {
			StackWrapper api = new StackWrapper(mEndpoint, Const.APIKEY);
			User result = null;
			try {
				result = api.getUserById(mUserID);
			}
			catch(Exception e) {
				mException = e;
			}
			return result;
		}

		@Override
		protected void onPostExecute(User result) {
			progressDialog.dismiss();
			if (mException != null) {
				Log.e(Const.TAG, "Error retrieving user info", mException);
				new AlertDialog.Builder(SitesActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.fetch_user_error)
					.setNeutralButton(android.R.string.ok, null)
					.create().show();
			}
			else {
				mSitesDatabase.setUser(mEndpoint, mUserID, result.getDisplayName());
				mSites.requery();
				mAdapter.notifyDataSetChanged();
				Toast.makeText(SitesActivity.this, "User " + result.getDisplayName() + " loaded", Toast.LENGTH_SHORT).show();
			}
		}
    }
    
    private class SitePickerTask extends AsyncTask<Void, Void, Void> {
    	
    	private ProgressDialog progressDialog;
    	private SitePickerTask mInstance;
    	private List<Site> sites;
    	private Exception mException;
    	
		@Override
		protected void onPreExecute() {
			mInstance = this;
			progressDialog = ProgressDialog.show(SitesActivity.this, "", getString(R.string.loading), true, true,
				new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mInstance.cancel(true);
					}
				});
		}
    	
		@Override
		protected void onCancelled() {
			progressDialog.dismiss();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				sites = StackAuth.getAllSites();
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			if (mException != null) {
				Log.e(Const.TAG, "Error retrieving sites", mException);
				new AlertDialog.Builder(SitesActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.fetch_sites_error)
					.setNeutralButton(android.R.string.ok, null)
					.create().show();
			}
			else {
				final CharSequence[] items = new CharSequence[sites.size()];
				int i=0;
				for (Site s: sites) {
					items[i++] = s.getName();
				}
				new AlertDialog.Builder(SitesActivity.this)
					.setTitle(R.string.menu_add_site)
					.setItems(items, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Site site = sites.get(which);
							new AddSiteTask().execute(site);
						}
					})
					.create().show();
			}
		}

    }
    
    
    private class AddSiteTask extends AsyncTask<Site, Void, Void> {
    	private Exception mException;
    	private Site site;
    	
    	@Override
		protected void onPreExecute() {
			mAdapter.setLoading(true);
		}

		@Override
		protected Void doInBackground(Site... params) {
			site = params[0];
			try {
				InputStream in = new URL(site.getIconUrl()).openStream();
				OutputStream out = new FileOutputStream(new File(mIcons, Uri.parse(site.getApiEndpoint()).getHost()));
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			mAdapter.setLoading(false);
			if (mException != null) {
				new AlertDialog.Builder(SitesActivity.this)
				.setTitle(R.string.title_error)
				.setMessage(R.string.site_add_error)
				.setNeutralButton(android.R.string.ok, null)
				.create().show();
				Log.e(Const.TAG, "Unable to add site", mException);
			}
			else {
				mSitesDatabase.addSite(site.getApiEndpoint(), site.getName(), 0, null);
				mSites.requery();
				mAdapter.notifyDataSetChanged();
			}
		}
    	
    }
    
    
}