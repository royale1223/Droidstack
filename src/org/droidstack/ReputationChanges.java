package org.droidstack;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Question;
import net.sf.stackwrap4j.entities.Reputation;
import net.sf.stackwrap4j.enums.Order;
import net.sf.stackwrap4j.query.ReputationQuery;

import org.droidstack.adapter.ReputationAdapter;
import org.droidstack.utils.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ReputationChanges extends ListActivity {
	
	private String mEndpoint;
	private String mSiteName;
	private int mUserID;
	private String mUserName;
	
	private Context mContext;
	private int mPageSize;
	private int mPage = 1;
	private ReputationAdapter mAdapter;
	
	private StackWrapper mAPI;
	private List<Reputation> mRepChanges;
	
	private boolean isRequestOngoing = false;
	private boolean noMoreChanges = false;
	
	@Override
	public void onCreate(Bundle inState) {
		super.onCreate(inState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		mContext = (Context) this;
		setContentView(R.layout.repchanges);
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		mSiteName = data.getQueryParameter("name");
		mUserName = data.getQueryParameter("uname");
		
		try {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
			if (mEndpoint == null) throw new NullPointerException();
		}
		catch (Exception e) {
			Log.e(Const.TAG, "ReputationChanges: could not parse launch parameters", e);
			finish();
			return;
		}
		
		String title = "";
		if (mSiteName != null) title += mSiteName + ": ";
		if (mUserName != null) title += mUserName;
		else title += "#" + mUserID;
		title += "'s rep changes";
		setTitle(title);
		
		mAPI = new StackWrapper(mEndpoint);
		mPageSize = Const.getPageSize(mContext);
		mRepChanges = new ArrayList<Reputation>();
		mAdapter = new ReputationAdapter(mContext, mRepChanges);
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(onScroll);
		getListView().setOnItemClickListener(onClick);
		
		if (inState == null) {
			new GetData().execute();
		}
		else {
			mRepChanges.addAll((List<Reputation>) inState.getSerializable("mRepChanges"));
			mPage = inState.getInt("mPage");
			mAdapter.notifyDataSetChanged();
			getListView().setSelection(inState.getInt("scroll"));
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mRepChanges", (ArrayList<Reputation>) mRepChanges);
		outState.putInt("mPage", mPage);
		outState.putInt("scroll", getListView().getFirstVisiblePosition());
	}
	
	private OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (isRequestOngoing == false && noMoreChanges == false && totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
				mPage++;
				new GetData().execute();
			}
		}
	};
	
	private OnItemClickListener onClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Reputation r = mRepChanges.get(position);
			Intent i = new Intent(mContext, ViewQuestion.class);
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
	};
	
	private class GetData extends AsyncTask<Void, Void, List<Reputation>> {
		
		private Exception mException;
		
		@Override
		protected void onPreExecute() {
			isRequestOngoing = true;
			setProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		protected List<Reputation> doInBackground(Void... params) {
			try {
				ReputationQuery query = new ReputationQuery();
				query.setPageSize(mPageSize).setPage(mPage).setIds(mUserID);
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
			isRequestOngoing = false;
			setProgressBarIndeterminateVisibility(false);
			if (mException != null) {
					new AlertDialog.Builder(mContext)
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
				if (result.size() < mPageSize) noMoreChanges = true;
				if (mRepChanges.size() == 0) {
					findViewById(R.id.empty).setVisibility(View.VISIBLE);
					getListView().setVisibility(View.GONE);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}
	
}
