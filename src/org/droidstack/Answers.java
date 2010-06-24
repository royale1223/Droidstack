package org.droidstack;

import java.util.ArrayList;
import java.util.List;

import org.droidstack.stackapi.Answer;
import org.droidstack.stackapi.AnswersQuery;
import org.droidstack.stackapi.QuestionsQuery;
import org.droidstack.stackapi.StackAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

public class Answers extends Activity {
	
	public final static String INTENT_TYPE = "type";
	
	private AnswersQuery mQuery;
	private StackAPI mAPI;
	private SitesDatabase mSitesDatabase;
	private int mSiteID;
	private String mQueryType;
	private String mEndpoint;
	private String mSiteName;
	private int mPage = 1;
	private int mPageSize;
	private long mUserID = 0;
	private String mUserName;
	private boolean mNoMoreAnswers = false;
	private boolean mIsRequestOngoing = true;
	
	private List<Answer> mAnswers;
	private ArrayAdapter<Answer> mAdapter;
	private ListView mListView;
	
	private Context mContext;
	private Resources mResources;

	private ArrayAdapter<CharSequence> mSortAdapter;
	private ArrayAdapter<CharSequence> mOrderAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.answers);
		
		mContext = this;
		mResources = getResources();
		mPageSize = getPreferences(Context.MODE_PRIVATE).getInt(Const.PREF_PAGESIZE, Const.DEF_PAGESIZE);
		
		Intent launchParams = getIntent();
		mQueryType = launchParams.getStringExtra(INTENT_TYPE);
		mSiteID = launchParams.getIntExtra(SitesDatabase.KEY_ID, -1);
		mUserID = launchParams.getLongExtra(SitesDatabase.KEY_UID, 0);
		mUserName = launchParams.getStringExtra(SitesDatabase.KEY_UNAME);
		mSitesDatabase = new SitesDatabase(mContext);
		mEndpoint = mSitesDatabase.getEndpoint(mSiteID);
		mSiteName = mSitesDatabase.getName(mSiteID);
		mSitesDatabase.dispose();
		
		try {
			mQuery = new AnswersQuery(mQueryType).setUser(mUserID).setPageSize(mPageSize);
		}
		catch (Exception e) {
			Log.e(Const.TAG, "Invalid invocation", e);
			finish();
		}
		
		if (mQueryType.equals(AnswersQuery.QUERY_USER)) {
			setTitle(mSiteName + ": " + getString(R.string.title_user_answers).replace("%s", mUserName));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.a_sort_user, android.R.layout.simple_spinner_item);
		}
		
		mAPI = new StackAPI(mEndpoint, Const.APIKEY);
		mAnswers = new ArrayList<Answer>();
		mAdapter = new AnswersListAdapter<Answer>(mContext, 0, mAnswers);
		mListView = (ListView) findViewById(R.id.answers);
		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(onAnswersScrolled);
		
		mSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOrderAdapter = ArrayAdapter.createFromResource(this, R.array.q_order, android.R.layout.simple_spinner_item);
		mOrderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		getAnswers();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		if (mIsRequestOngoing == false) {
	    	getMenuInflater().inflate(R.menu.answers, menu);
	    	return true;
		}
		return false;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_sort:
    		AlertDialog.Builder b = new AlertDialog.Builder(this);
    		b.setTitle(R.string.menu_sort);
    		View v = getLayoutInflater().inflate(R.layout.menu_sort, null);
    		final Spinner sort = (Spinner)v.findViewById(R.id.spinner_sort); 
    		sort.setAdapter(mSortAdapter);
    		String[] validSortFields = AnswersQuery.validSortFields.get(mQueryType);
    		for (int i=0; i < validSortFields.length; i++) {
    			if (validSortFields[i].equals(mQuery.getSort())) {
    				sort.setSelection(i);
    				break;
    			}
    		}
    		final Spinner order = (Spinner) v.findViewById(R.id.spinner_order);
    		order.setAdapter(mOrderAdapter);
    		if (mQuery.getOrder().equals(AnswersQuery.ORDER_DESCENDING)) order.setSelection(0);
    		if (mQuery.getOrder().equals(AnswersQuery.ORDER_ASCENDING)) order.setSelection(1);
    		b.setView(v);
    		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mQuery.setSort(AnswersQuery.validSortFields.get(mQueryType)[sort.getSelectedItemPosition()]);
					if (order.getSelectedItemPosition() == 0) {
						mQuery.setOrder(QuestionsQuery.ORDER_DESCENDING);
					}
					else {
						mQuery.setOrder(QuestionsQuery.ORDER_ASCENDING);
					}
					mNoMoreAnswers = false;
					mAnswers.clear();
					mPage = 1;
					mQuery.setPage(1);
					getAnswers();
				}
			});
    		b.setNegativeButton(android.R.string.cancel, null);
    		b.create().show();
    		break;
    	}
    	return false;
    }
	
	private void getAnswers() {
		new GetAnswersTask().execute(mQuery);
	}
	
	private class GetAnswersTask extends AsyncTask<AnswersQuery, Void, List<Answer>> {
		
		private Exception mException;
		
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			mIsRequestOngoing = true;
		}
		
		@Override
		protected List<Answer> doInBackground(AnswersQuery... params) {
			try {
				return mAPI.getAnswers(params[0]);
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		@Override
		protected void onPostExecute(List<Answer> result) {
			setProgressBarIndeterminateVisibility(false);
			mIsRequestOngoing = false;
			if (mException != null) {
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.answers_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get answers", mException);
			}
			else {
				if (result.size() < mPageSize) mNoMoreAnswers = true;
				mAnswers.addAll(result);
				mAdapter.notifyDataSetChanged();
				if (mAnswers.size() == 0) {
					findViewById(R.id.empty).setVisibility(View.VISIBLE);
					mListView.setVisibility(View.GONE);
				}
			}
		}
		
	}
	
	private class AnswersListAdapter<E> extends ArrayAdapter<E> {
		
		private LayoutInflater inflater;
		
		private class ViewHolder {
			public TextView score;
			public TextView title;
		}
		
		public AnswersListAdapter(Context context, int textViewResourceId,
				List<E> objects) {
			super(context, textViewResourceId, objects);
			inflater = getLayoutInflater();
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Answer a = (Answer) getItem(position);
			View v;
			ViewHolder h;
			
			if (convertView == null) {
				v = inflater.inflate(R.layout.answer_item, null);
				h = new ViewHolder();
				h.score = (TextView) v.findViewById(R.id.score);
				h.title = (TextView) v.findViewById(R.id.title);
				v.setTag(h);
			}
			else {
				v = convertView;
				h = (ViewHolder) v.getTag();
			}
			
			h.score.setText(String.valueOf(a.score));
			h.title.setText(a.title);
			
			if (a.accepted) {
				h.score.setBackgroundResource(R.color.score_max_bg);
				h.score.setTextColor(mResources.getColor(R.color.score_max_text));
			}
			else if (a.score == 0) {
				h.score.setBackgroundResource(R.color.score_neutral_bg);
				h.score.setTextColor(mResources.getColor(R.color.score_neutral_text));
			}
			else if (a.score > 0) {
				h.score.setBackgroundResource(R.color.score_high_bg);
				h.score.setTextColor(mResources.getColor(R.color.score_high_text));
			}
			else {
				h.score.setBackgroundResource(R.color.score_low_bg);
				h.score.setTextColor(mResources.getColor(R.color.score_low_text));
			}
			
			return v;
		}
		
	}
	
	private OnScrollListener onAnswersScrolled = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (mIsRequestOngoing == false && mNoMoreAnswers == false && firstVisibleItem + visibleItemCount == totalItemCount) {
				mQuery.setPage(++mPage);
				getAnswers();
			}
		}
	};
	
}
