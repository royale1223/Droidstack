package org.droidstack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class Sites extends Activity {
	
	private SitesDatabase mSitesDatabase;
	private Cursor mSites;
	private ListView mListView;
	private SimpleCursorAdapter mAdapter;
	private View mAddSiteDialogView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sites);
        mSitesDatabase = new SitesDatabase(getApplicationContext());
        mSites = mSitesDatabase.getSites();
        startManagingCursor(mSites);
        
        mListView = (ListView) findViewById(R.id.SitesListView);
        mListView.setEmptyView(findViewById(R.id.NoSitesText));
        mAdapter = new SimpleCursorAdapter(
        	getApplicationContext(),
        	R.layout.site_row,
        	mSites,
        	new String[] { SitesDatabase.KEY_SITE },
        	new int[] { R.id.SiteTextView }
        );
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(onSiteClicked);
        registerForContextMenu(mListView);
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
			String site = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_SITE));
			Intent i = new Intent(getApplicationContext(), Site.class);
			i.setAction(Intent.ACTION_VIEW);
			i.setData(Uri.parse(site));
			startActivity(i);
		}
	};
    
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
    	if (v.getId() == R.id.SitesListView) {
    		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
    		mSites.moveToPosition(info.position);
    		String site = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_SITE));
    		menu.setHeaderTitle(site);
    		getMenuInflater().inflate(R.menu.sites_context, menu);
    	}
	}
    
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		mSites.moveToPosition(info.position);
		String site = mSites.getString(mSites.getColumnIndex(SitesDatabase.KEY_SITE));
		long userID = mSites.getLong(mSites.getColumnIndex(SitesDatabase.KEY_UID));
		switch(item.getItemId()) {
		case R.id.menu_set_user:
			addOrEditSite(site, userID);
			return true;
		case R.id.menu_remove:
			mSitesDatabase.removeSite(site);
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
    		addOrEditSite(null, 0);
    		break;
    	}
    	return false;
    }
    
    private void addOrEditSite(String site, long userID) {
    	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    	if (site == null) {
    		dialog.setTitle(R.string.menu_add_site);
    	}
    	else {
    		dialog.setTitle(R.string.menu_edit_site);
    	}
		mAddSiteDialogView = getLayoutInflater().inflate(R.layout.add_site, null);
		EditText siteEdit = (EditText) mAddSiteDialogView.findViewById(R.id.site);
		EditText userEdit = (EditText) mAddSiteDialogView.findViewById(R.id.user);
		if (site != null) {
			siteEdit.setText(site);
			siteEdit.setEnabled(false);
			siteEdit.clearFocus();
			userEdit.requestFocus();
		}
		else {
			siteEdit.setText("");
			siteEdit.setEnabled(true);
		}
		if (userID != 0) {
			userEdit.setText(String.valueOf(userID));
		}
		else {
			userEdit.setText("");
		}
		dialog.setView(mAddSiteDialogView);
		dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String site = ((EditText)mAddSiteDialogView.findViewById(R.id.site)).getText().toString();
				long userID = 0;
				try {
					userID = Long.parseLong(((EditText)mAddSiteDialogView.findViewById(R.id.user)).getText().toString(), 10);
				}
				catch (Exception e) { }
				mSitesDatabase.addSite(site, userID);
				mSites.requery();
				mAdapter.notifyDataSetChanged();
			}
		});
		dialog.setNegativeButton(android.R.string.cancel, null);
		dialog.show();
    }
    
}