package org.droidstack.activity;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Answer;
import net.sf.stackwrap4j.enums.Order;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.AnswerQuery;

import org.droidstack.R;
import org.droidstack.adapter.AnswersAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class AnswersActivity extends ListActivity {
	
	public final static String TYPE_USER = "user";
	
	private StackWrapper mAPI;
	private String mQueryType;
	private String mEndpoint;
	private int mPage = 1;
	private int mPageSize;
	private int mUserID = 0;
	private String mUserName;
	private boolean mNoMoreAnswers = false;
	private boolean mIsRequestOngoing = true;
	private int mSort = -1;
	private Order mOrder = Order.DESC;
	private boolean mIsStartedForResult = false;
	
	private List<Answer> mAnswers;
	private AnswersAdapter mAdapter;

	private ArrayAdapter<CharSequence> mSortAdapter;
	private ArrayAdapter<CharSequence> mOrderAdapter;
	
	private TextView mTitleView;
	private View mLoadingView;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.answers);
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mPageSize = Const.getPageSize(this);
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) mIsStartedForResult = true; 
		
		Uri data = getIntent().getData();
		mQueryType = data.getPathSegments().get(0);
		mEndpoint = data.getQueryParameter("endpoint");
		mUserName = data.getQueryParameter("uname");
		
		if (mQueryType.equals(TYPE_USER)) {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.a_sort_user, android.R.layout.simple_spinner_item);
		}
		
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		if (inState == null) {
			mAnswers = new ArrayList<Answer>();
		}
		else {
			mAnswers = (ArrayList<Answer>) inState.getSerializable("mAnswers");
			mPage = inState.getInt("mPage");
			mSort = inState.getInt("mSort");
			if (inState.getBoolean("isAsc")) mOrder = Order.ASC;
			mIsRequestOngoing = false;
		}
		mAdapter = new AnswersAdapter(this, mAnswers);
		mTitleView = (TextView) View.inflate(this, R.layout.item_header, null);
		mLoadingView = View.inflate(this, R.layout.item_loading, null);
		getListView().addHeaderView(mTitleView, null, false);
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(onAnswersScrolled);
		getListView().setOnItemClickListener(onAnswerClicked);
		
		mSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOrderAdapter = ArrayAdapter.createFromResource(this, R.array.q_order, android.R.layout.simple_spinner_item);
		mOrderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		if (inState == null) new GetAnswersTask().execute();
		else getListView().setSelection(inState.getInt("scroll"));
		
		setNiceTitle();
	}
	
	@Override
	public void setTitle(CharSequence title) {
		mTitleView.setText(title);
	}
	
	private void setNiceTitle() {
		StringBuilder b = new StringBuilder();
		if (mQueryType.equals(TYPE_USER)) {
			if (mUserName != null) {
				if (mUserName.endsWith("s")) b.append(mUserName).append("' answers");
				else b.append(mUserName).append("'s answers");
			}
			else b.append("User answers");
		}
		
		setTitle(b);
	}
	
	private void setLoading(boolean loading) {
		getListView().removeFooterView(mLoadingView);
		if (loading) getListView().addFooterView(mLoadingView, null, false);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mAnswers", (ArrayList<Answer>) mAnswers);
		outState.putInt("mPage", mPage);
		outState.putInt("mSort", mSort);
		outState.putBoolean("isAsc", mOrder.equals(Order.ASC));
		outState.putInt("scroll", getListView().getFirstVisiblePosition());
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
    		if (mSort > -1) {
    			sort.setSelection(mSort);
    		}
    		final Spinner order = (Spinner) v.findViewById(R.id.spinner_order);
    		order.setAdapter(mOrderAdapter);
    		if (mOrder.equals(Order.DESC)) order.setSelection(0);
    		else order.setSelection(1);
    		b.setView(v);
    		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mSort = sort.getSelectedItemPosition();
					if (order.getSelectedItemPosition() == 0) mOrder = Order.DESC;
					else mOrder = Order.ASC;
					mAnswers.clear();
					mAdapter.notifyDataSetChanged();
					mNoMoreAnswers = false;
					mPage = 1;
					new GetAnswersTask().execute();
				}
			});
    		b.setNegativeButton(android.R.string.cancel, null);
    		b.create().show();
    		break;
    	}
    	return false;
    }
	
	private class GetAnswersTask extends AsyncTask<Void, Void, List<Answer>> {
		
		private Exception mException;
		
		@Override
		protected void onPreExecute() {
			setLoading(true);
			mIsRequestOngoing = true;
		}
		
		@Override
		protected List<Answer> doInBackground(Void... params) {
			try {
				if (mQueryType.equals(TYPE_USER)) {
					AnswerQuery query = new AnswerQuery();
					query.setComments(false).setBody(false).setPageSize(mPageSize).setPage(mPage);
					query.setIds(mUserID);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(AnswerQuery.Sort.activity()); break;
						case 1: query.setSort(AnswerQuery.Sort.views()); break;
						case 2: query.setSort(AnswerQuery.Sort.creation()); break;
						case 3: query.setSort(AnswerQuery.Sort.votes()); break;
						}
					}
					return mAPI.getAnswersByUserId(query);
				}
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		@Override
		protected void onPostExecute(List<Answer> result) {
			if (isFinishing()) return;
			mIsRequestOngoing = false;
			if (mException != null) {
				new AlertDialog.Builder(AnswersActivity.this)
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
				if (mPage == 1) mAnswers.clear();
				if (result.size() < mPageSize) {
					mNoMoreAnswers = true;
					setLoading(false);
				}
				mAnswers.addAll(result);
				mAdapter.notifyDataSetChanged();
				if (mAnswers.size() == 0) {
					findViewById(R.id.empty).setVisibility(View.VISIBLE);
					getListView().setVisibility(View.GONE);
				}
			}
		}
		
	}
	
	private OnItemClickListener onAnswerClicked = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// position-1 because we currently always have a title set on the adapter
			Answer a = mAnswers.get(position-1);
			if (!mIsStartedForResult) {
				Intent i = new Intent(AnswersActivity.this, QuestionActivity.class);
				String uri = "droidstack://question" +
					"?endpoint=" + Uri.encode(mEndpoint) +
					"&qid=" + Uri.encode(String.valueOf(a.getQuestionId()));
				i.setData(Uri.parse(uri));
				startActivity(i);
			}
			else {
				Intent i = new Intent();
				i.putExtra("id", a.getPostId());
				i.putExtra("qid", a.getQuestionId());
				i.putExtra("title", a.getTitle());
				i.putExtra("score", a.getScore());
				i.putExtra("accepted", a.isAccepted());
				setResult(RESULT_OK, i);
				finish();
			}
		}
		
	};
	
	private OnScrollListener onAnswersScrolled = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (mIsRequestOngoing == false && mNoMoreAnswers == false && firstVisibleItem + visibleItemCount == totalItemCount) {
				mPage++;
				new GetAnswersTask().execute();
			}
		}
	};
	
}
