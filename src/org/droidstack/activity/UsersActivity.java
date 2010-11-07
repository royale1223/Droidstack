package org.droidstack.activity;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.query.UserQuery;

import org.droidstack.R;
import org.droidstack.adapter.UsersAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.WindowManager;
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
	private HashMap<String, Bitmap> mAvatars;
	
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
		mAvatars = new HashMap<String, Bitmap>();
		mAdapter = new UsersAdapter(this, mUsers, mAvatars);
		
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
			if (isFinishing()) return;
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
				ArrayList<String> hashes = new ArrayList<String>();
				for (User u: mUsers) {
					if (!mAvatars.containsKey(u.getEmailHash())) {
						hashes.add(u.getEmailHash());
					}
				}
				if (hashes.size() > 0) {
					new GetAvatars(hashes).execute();
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}
	
	private class GetAvatars extends AsyncTask<Void, Pair<String, Bitmap>, Void> {
		
		private int size = 64;
		private List<String> hashes;
		
		public GetAvatars(List<String> hashes) {
			DisplayMetrics metrics = new DisplayMetrics();
			((WindowManager)UsersActivity.this.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay().getMetrics(metrics);
			size *= metrics.density;
			this.hashes = hashes;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			for (String hash: hashes) {
				try {
					URL avatarURL = new URL("http://www.gravatar.com/avatar/" + hash + "?s=" + size + "&d=identicon&r=PG");
					Bitmap avatar = BitmapFactory.decodeStream(avatarURL.openStream());
					Pair<String, Bitmap> pair = new Pair<String, Bitmap>(hash, avatar);
					publishProgress(pair);
				}
				catch (Exception e) {
					Log.e(Const.TAG, "Could not fetch avatar", e);
				}
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Pair<String, Bitmap>... pairs) {
			if (isFinishing()) return;
			mAvatars.put(pairs[0].first, pairs[0].second);
			mAdapter.notifyDataSetChanged();
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
				i.putExtra("endpoint", mEndpoint);
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
