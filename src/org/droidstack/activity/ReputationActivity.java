package org.droidstack.activity;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Reputation;
import net.sf.stackwrap4j.query.ReputationQuery;

import org.droidstack.R;
import org.droidstack.adapter.ReputationAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class ReputationActivity extends ListActivity implements OnScrollListener {
	
	private String mEndpoint;
	private int mUserID;
	private String mUserName;
	
	private int mPageSize;
	private int mPage = 1;
	private ReputationAdapter mAdapter;
	
	private StackWrapper mAPI;
	private List<Reputation> mRepChanges;
	
	private boolean isRequestOngoing = false;
	private boolean noMoreChanges = false;
	
	private TextView mTitleView;
	private View mLoadingView;
	
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.repchanges);
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		try {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
			if (mEndpoint == null) throw new NullPointerException();
		}
		catch (Exception e) {
			Log.e(Const.TAG, "ReputationChanges: could not parse launch parameters", e);
			finish();
			return;
		}
		mUserName = data.getQueryParameter("uname");
		
		mAPI = new StackWrapper(mEndpoint);
		mPageSize = Const.getPageSize(this);
		mRepChanges = new ArrayList<Reputation>();
		mAdapter = new ReputationAdapter(this, mRepChanges);
		mTitleView = (TextView) View.inflate(this, R.layout.item_header, null);
		mLoadingView = View.inflate(this, R.layout.item_loading, null);
		getListView().addHeaderView(mTitleView, null, false);
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(this);
		
		if (inState == null) {
			new GetData().execute();
		}
		else {
			mRepChanges.addAll((List<Reputation>) inState.getSerializable("mRepChanges"));
			mPage = inState.getInt("mPage");
			mAdapter.notifyDataSetChanged();
			getListView().setSelection(inState.getInt("scroll"));
		}
		
		setNiceTitle();
	}
	
	@Override
	public void setTitle(CharSequence title) {
		mTitleView.setText(title);
	}
	
	private void setNiceTitle() {
		StringBuilder b = new StringBuilder();
		if (mUserName != null) {
			b.append(mUserName);
			if (mUserName.endsWith("s")) b.append(mUserName).append("' ");
			else b.append("'s ");
		}
		b.append("Reputation");
		setTitle(b);
	}
	
	private void setLoading(boolean loading) {
		getListView().removeFooterView(mLoadingView);
		if (loading) getListView().addFooterView(mLoadingView, null, false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mRepChanges", (ArrayList<Reputation>) mRepChanges);
		outState.putInt("mPage", mPage);
		outState.putInt("scroll", getListView().getFirstVisiblePosition());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Reputation r = mRepChanges.get(position);
		Intent i = new Intent(ReputationActivity.this, QuestionActivity.class);
		if (r.getPostType().equals("question")) {
			String uri = "droidstack://question" +
				"?endpoint=" + Uri.encode(mEndpoint) +
				"&qid=" + Uri.encode(String.valueOf(r.getPostId()));
			i.setData(Uri.parse(uri));
		}
		else {
			String uri = "droidstack://question" +
				"?endpoint=" + Uri.encode(mEndpoint) +
				"&aid=" + Uri.encode(String.valueOf(r.getPostId()));
			i.setData(Uri.parse(uri));
		}
		startActivity(i);
	}
		
	private class GetData extends AsyncTask<Void, Void, List<Reputation>> {
		
		private Exception mException;
		
		@Override
		protected void onPreExecute() {
			isRequestOngoing = true;
			setLoading(true);
		}
		
		@Override
		protected List<Reputation> doInBackground(Void... params) {
			try {
				ReputationQuery query = new ReputationQuery();
				query.setFromDate(0).setPageSize(mPageSize).setPage(mPage).setIds(mUserID);
				List<Reputation> changes = mAPI.getReputationByUserId(query);
				return changes;
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Reputation> result) {
			if (isFinishing()) return;
			isRequestOngoing = false;
			setProgressBarIndeterminateVisibility(false);
			if (mException != null) {
					new AlertDialog.Builder(ReputationActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.rep_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get rep changes", mException);
			}
			else {
				if (mPage == 1) mRepChanges.clear();
				mRepChanges.addAll(result);
				if (result.size() < mPageSize) {
					noMoreChanges = true;
					setLoading(false);
				}
				if (mRepChanges.size() == 0) {
					findViewById(R.id.empty).setVisibility(View.VISIBLE);
					getListView().setVisibility(View.GONE);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (isRequestOngoing == false && noMoreChanges == false && totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
			mPage++;
			new GetData().execute();
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// not used
	}
	
}
