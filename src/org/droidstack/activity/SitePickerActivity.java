package org.droidstack.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.stackauth.StackAuth;
import net.sf.stackwrap4j.stackauth.entities.Site;

import org.droidstack.R;
import org.droidstack.adapter.SitesArrayAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class SitePickerActivity extends ListActivity implements TextWatcher {
	
	private List<Site> mSites;
	private final ArrayList<Site> mFiltered = new ArrayList<Site>();
	private final ArrayList<Site> mSelected = new ArrayList<Site>();
	
	private final ArrayList<String> mEndpoints = new ArrayList<String>();
	
	private SitesArrayAdapter mAdapter;
	private EditText mFilter;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.sites_picker);
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		Bundle extras = getIntent().getExtras();
		mEndpoints.addAll(Arrays.asList((String[]) extras.getSerializable("checked")));
		
		mSites = new ArrayList<Site>();
		mAdapter = new SitesArrayAdapter(this, mFiltered);
		setListAdapter(mAdapter);
		
		mFilter = (EditText) findViewById(R.id.filter);
		mFilter.addTextChangedListener(this);
		
		new GetSites().execute();
	}
	
	private class GetSites extends AsyncTask<Void, Void, List<Site>> {
		private Exception e;
		
		@Override
		protected void onPreExecute() {
			
		}
		@Override
		protected List<Site> doInBackground(Void... params) {
			try {
				return StackAuth.getAllSites();
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}
		@Override
		protected void onPostExecute(List<Site> result) {
			if (isFinishing()) return;
			if (e != null) {
				new AlertDialog.Builder(SitePickerActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.sites_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get sites", e);
			}
			else {
				mSites.clear();
				mSites.addAll(result);
				for (Site s: mSites) {
					if (mEndpoints.contains(s.getApiEndpoint())) mSelected.add(s);
				}
				afterTextChanged(null);
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			if (mSites.size() > 0) {
				String[] endpoints = new String[mSelected.size()];
				String[] names = new String[mSelected.size()];
				String[] icons = new String[mSelected.size()];
				for (int i=0; i < mSelected.size(); i++) {
					Site site = mSelected.get(i);
					endpoints[i] = site.getApiEndpoint();
					names[i] = site.getName();
					icons[i] = site.getIconUrl();
				}
				Intent result = new Intent();
				result.putExtra("endpoints", endpoints);
				result.putExtra("names", names);
				result.putExtra("icons", icons);
				setResult(RESULT_OK, result);
			}
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Site site = mFiltered.get(position);
		if (mSelected.contains(site)) {
			mSelected.remove(site);
			getListView().setItemChecked(position, false);
		}
		else {
			mSelected.add(site);
			getListView().setItemChecked(position, true);
		}
	}
	
	@Override
	public void afterTextChanged(Editable constr) {
		mFiltered.clear();
		if (constr == null || constr.equals("")) mFiltered.addAll(mSites);
		else {
			Pattern pat = Pattern.compile(Pattern.quote(constr.toString()), Pattern.CASE_INSENSITIVE);
			for (Site s: mSites) {
				if (pat.matcher(s.getName()).find()) {
					mFiltered.add(s);
				}
			}
		}
		getListView().clearChoices();
		for (int i=0; i < mFiltered.size(); i++) {
			if (mSelected.contains(mFiltered.get(i)))
				getListView().setItemChecked(i, true);
		}
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// nada
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// nope
	}
	
}
