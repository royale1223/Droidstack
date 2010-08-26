package org.droidstack.activity;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.query.UserQuery;

import org.droidstack.R;
import org.droidstack.adapter.UsersAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class UsersActivity extends ListActivity {
	
	private String mEndpoint;
	private StackWrapper mAPI;
	
	private int mPage = 1;
	private int mPageSize;
	
	private UsersAdapter mAdapter;
	private ArrayList<User> mUsers;
	
	private boolean isRequestOngoing;
	private boolean noMoreUsers;
	private boolean isStartedForResult;
	
	private EditText mFilter;
	private Handler mHandler;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.users);
		
		mPageSize = Const.getPageSize(this);
		
		mFilter = (EditText) findViewById(R.id.filter);
		mHandler = new Handler();
		
		mUsers = new ArrayList<User>();
		mAdapter = new UsersAdapter(this, mUsers);
		
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(onScroll);
		getListView().setOnItemClickListener(onClick);
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) isStartedForResult = true;
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		
		if (inState != null) {
			mUsers.addAll((ArrayList<User>) inState.getSerializable("mUsers"));
			mFilter.setText(inState.getString("filter"));
			mPage = inState.getInt("mPage");
			mAdapter.notifyDataSetChanged();
			getListView().setSelection(inState.getInt("scroll"));
		}
		
		mFilter.addTextChangedListener(onTextChanged);
		
		if (inState == null) new GetUsers().execute();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mUsers", mUsers);
		outState.putString("filter", mFilter.getText().toString());
		outState.putInt("mPage", mPage);
		outState.putInt("scroll", getListView().getFirstVisiblePosition());
	}
	
	private TextWatcher onTextChanged = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// unused
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// unused
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			mHandler.removeCallbacks(applyNewFilter);
			mHandler.postDelayed(applyNewFilter, 1500);
		}
	};
	
	private Runnable applyNewFilter = new Runnable() {
		@Override
		public void run() {
			if (isRequestOngoing) return;
			mPage = 1;
			noMoreUsers = false;
			mUsers.clear();
			mAdapter.notifyDataSetChanged();
			new GetUsers().execute();
		}
	};
	
	private class GetUsers extends AsyncTask<Void, Void, List<User>> {
		
		private Exception e;
		
		@Override
		protected void onPreExecute() {
			mAdapter.setLoading(true);
			isRequestOngoing = true;
		}

		@Override
		protected List<User> doInBackground(Void... params) {
			UserQuery query = new UserQuery();
			query.setPage(mPage).setPageSize(mPageSize);
			query.setFilter(mFilter.getText().toString());
			try {
				return mAPI.listUsers(query);
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<User> result) {
			isRequestOngoing = false;
			if (e != null) {
				new AlertDialog.Builder(UsersActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.users_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get users", e);
			}
			else {
				mUsers.addAll(result);
				if (result.size() < mPageSize) {
					noMoreUsers = true;
					mAdapter.setLoading(false);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}
	
	private OnItemClickListener onClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			User u = mUsers.get(position);
			if (!isStartedForResult) {
				Intent i = new Intent(UsersActivity.this, UserActivity.class);
				String uri = "droidstack://user" +
					"?endpoint=" + Uri.encode(mEndpoint) +
					"&uid=" + u.getId();
				i.setData(Uri.parse(uri));
				startActivity(i);
			}
			else {
				Intent i = new Intent();
				i.putExtra("uid", u.getId());
				i.putExtra("name", u.getDisplayName());
				i.putExtra("rep", u.getReputation());
				i.putExtra("emailHash", u.getEmailHash());
				setResult(RESULT_OK, i);
				finish();
			}
		}
	};
	
	private OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (isRequestOngoing == false && noMoreUsers == false && totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
				mPage++;
				new GetUsers().execute();
			}
		}
	};
	
}
