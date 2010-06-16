package org.droidstack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.droidstack.stackapi.Site;
import org.droidstack.stackapi.StackAPI;
import org.droidstack.stackapi.User;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class Sites extends Activity {
	
	private SitesDatabase mSitesDatabase;
	private Cursor mSites;
	private GridView mGridView;
	private SimpleCursorAdapter mAdapter;
	private Context mContext;
	private File mIcons;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.sites);
        mContext = (Context) this;
        mSitesDatabase = new SitesDatabase(mContext);
        mSites = mSitesDatabase.getSites();
        startManagingCursor(mSites);
        
        mGridView = (GridView) findViewById(R.id.sites);
        mGridView.setEmptyView(findViewById(R.id.NoSitesText));
        mAdapter = new SitesAdapter(
        	this,
        	android.R.layout.simple_list_item_1,
        	mSites,
        	new String[] { SitesDatabase.KEY_NAME },
        	new int[] { android.R.id.text1 }
        );
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(onSiteClicked);
        registerForContextMenu(mGridView);
        
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
        		Log.e(Const.TAG, "Exception: " + e.getMessage());
        		externalMediaError();
        	}
        }
        
    }
    
    private void externalMediaError() {
    	new AlertDialog.Builder(mContext)
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
    
    private class SitesAdapter extends SimpleCursorAdapter {
    	
    	private final LayoutInflater inflater;
    	
    	private class ViewHolder {
    		public TextView label;
    		public ImageView icon;
    	}
    	
		public SitesAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to);
			inflater = getLayoutInflater();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			mSites.moveToPosition(position);
			String name = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_NAME));
			String endpoint = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			View v;
			ViewHolder h;
			if (convertView == null) {
				v = inflater.inflate(R.layout.site_item, null);
				h = new ViewHolder();
				h.label = (TextView) v.findViewById(R.id.label);
				h.icon = (ImageView) v.findViewById(R.id.icon);
				v.setTag(h);
			}
			else {
				v = convertView;
				h = (ViewHolder) convertView.getTag();
			}
			h.label.setText(name);
			
			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.reset();
				md5.update(endpoint.getBytes());
				byte[] hash = md5.digest();
				StringBuilder hexHash = new StringBuilder();
				for (int i=0; i < hash.length; i++) {
					hexHash.append(Integer.toHexString(0xFF & hash[i]));
				}
				Drawable icon = Drawable.createFromPath(new File(mIcons, hexHash.toString()).getAbsolutePath());
				h.icon.setImageDrawable(icon);
			}
			catch (NoSuchAlgorithmException e) {
				// wtf?
			}
			return v;
		}
    	
    }

	private OnItemClickListener onSiteClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			mSites.moveToPosition(position);
			int siteID = mSites.getInt(mSites.getColumnIndex(SitesDatabase.KEY_ID));
			Intent i = new Intent(mContext, SiteActions.class);
			i.setAction(Intent.ACTION_VIEW);
			i.putExtra(SitesDatabase.KEY_ID, siteID);
			startActivity(i);
		}
	};
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
    	if (v.getId() == R.id.sites) {
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
		final int id = mSites.getInt(mSites.getColumnIndex(SitesDatabase.KEY_ID));
		long userID = mSites.getLong(mSites.getColumnIndex(SitesDatabase.KEY_UID));
		String name = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_NAME));
		switch(item.getItemId()) {
		case R.id.menu_set_user:
			View dialogView = getLayoutInflater().inflate(R.layout.set_user_dialog, null);
			final EditText userEntry = (EditText) dialogView.findViewById(R.id.user);
			if (userID > 0) {
				userEntry.setText(String.valueOf(userID));
			}
			new AlertDialog.Builder(mContext)
				.setTitle(name)
				.setView(dialogView)
				.setNegativeButton(android.R.string.cancel, null)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						long userID = Long.parseLong(userEntry.getText().toString());
						new SetUserIDTask(id, userID).execute();
					}
				}).create().show();
			return true;
		case R.id.menu_remove:
			mSitesDatabase.removeSite(id);
			mSites.requery();
			mAdapter.notifyDataSetChanged();
			return true;
		}
		return super.onContextItemSelected(item);
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
    	}
    	return false;
    }
    
    private class SetUserIDTask extends AsyncTask<Void, Void, User> {
    	
    	private final int mSiteID;
    	private final long mUserID;
    	private final String mEndpoint;
    	private Exception mException;
    	private ProgressDialog progressDialog;
    	
    	public SetUserIDTask(int id, long userID) {
    		super();
    		mSiteID = id;
    		mUserID = userID;
    		mEndpoint = mSitesDatabase.getEndpoint(id);
    	}
    	
    	@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, false);
		}
    	
		@Override
		protected User doInBackground(Void... params) {
			StackAPI api = new StackAPI(mEndpoint);
			User result = null;
			try {
				result = api.getUser(mUserID);
			}
			catch(Exception e) {
				mException = e;
			}
			return result;
		}

		@Override
		protected void onPostExecute(User result) {
			setProgressBarIndeterminateVisibility(false);
			progressDialog.dismiss();
			if (mException != null) {
				Log.e(Const.TAG, "Error retrieving user info", mException);
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.fetch_user_error)
					.setNeutralButton(android.R.string.ok, null)
					.create().show();
			}
			else {
				mSitesDatabase.setUser(mSiteID, mUserID, result.name);
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
			setProgressBarIndeterminateVisibility(true);
			mInstance = this;
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, true,
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
			setProgressBarIndeterminateVisibility(false);
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				sites = StackAPI.getSites();
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			setProgressBarIndeterminateVisibility(false);
			progressDialog.dismiss();
			if (mException != null) {
				Log.e(Const.TAG, "Error retrieving sites", mException);
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.fetch_sites_error)
					.setNeutralButton(android.R.string.ok, null)
					.create().show();
			}
			else {
				final CharSequence[] items = new CharSequence[sites.size()];
				int i=0;
				for (Site s: sites) {
					items[i++] = s.name;
				}
				new AlertDialog.Builder(mContext)
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
    	
    	private ProgressDialog progressDialog;
    	private Exception mException;
    	private Site site;
    	
    	@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, false);
		}

		@Override
		protected Void doInBackground(Site... params) {
			site = params[0];
			try {
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				md5.reset();
				md5.update(site.api_endpoint.getBytes());
				byte[] hash = md5.digest();
				StringBuilder hexHash = new StringBuilder();
				for (int i=0; i < hash.length; i++) {
					hexHash.append(Integer.toHexString(0xFF & hash[i]));
				}
				
				File icon = new File(mIcons, hexHash.toString());
				InputStream in = new URL(site.icon_url).openStream();
				OutputStream out = new FileOutputStream(icon);
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
			setProgressBarIndeterminateVisibility(false);
			progressDialog.dismiss();
			if (mException != null) {
				new AlertDialog.Builder(mContext)
				.setTitle(R.string.title_error)
				.setMessage(R.string.site_add_error)
				.setNeutralButton(android.R.string.ok, null)
				.create().show();
				Log.e(Const.TAG, "Unable to add site", mException);
			}
			else {
				mSitesDatabase.addSite(site.api_endpoint, site.name, 0, null);
				mSites.requery();
				mAdapter.notifyDataSetChanged();
			}
		}
    	
    }
    
    
}