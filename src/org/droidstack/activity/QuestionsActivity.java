package org.droidstack.activity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Question;
import net.sf.stackwrap4j.enums.Order;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.FavoriteQuery;
import net.sf.stackwrap4j.query.QuestionQuery;
import net.sf.stackwrap4j.query.SearchQuery;
import net.sf.stackwrap4j.query.UnansweredQuery;
import net.sf.stackwrap4j.query.UserQuestionQuery;

import org.droidstack.R;
import org.droidstack.adapter.QuestionsAdapter;
import org.droidstack.util.Const;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class QuestionsActivity extends Activity {
	
	public final static String TYPE_ALL = "all";
	public final static String TYPE_UNANSWERED = "unanswered";
	public final static String TYPE_USER = "user";
	public final static String TYPE_FAVORITES = "favorites";
	public final static String TYPE_SEARCH = "search";
	
	private StackWrapper mAPI;
	private String mQueryType;
	private String mEndpoint;
	private String mSiteName;
	private int mPage = 1;
	private int mPageSize;
	private int mUserID = 0;
	private String mUserName;
	private String mInTitle;
	private ArrayList<String> mTagged;
	private String mNotTagged;
	private boolean mNoMoreQuestions = false;
	private boolean mIsRequestOngoing = true;
	private int mSort = -1;
	private Order mOrder = Order.DESC;
	private boolean mIsStartedForResult = false;
	
	private Context mContext;
	
	private List<Question> mQuestions;
	private QuestionsAdapter mAdapter;
	private ListView mListView;
	private ArrayAdapter<CharSequence> mSortAdapter;
	private ArrayAdapter<CharSequence> mOrderAdapter;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.questions);
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mContext = (Context) this;
		mPageSize = Const.getPageSize(mContext);
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) mIsStartedForResult = true;
		
		Uri data = getIntent().getData();
		mQueryType = data.getPathSegments().get(0);
		mEndpoint = data.getQueryParameter("endpoint");
		mSiteName = data.getQueryParameter("name");
		
		String titlePrefix = "";
		if (mSiteName != null) titlePrefix = mSiteName + ": ";
		if (mQueryType.equals(TYPE_ALL)) {
			setTitle(titlePrefix + getString(R.string.title_all_questions));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_all, android.R.layout.simple_spinner_item);
		}
		else if (mQueryType.equals(TYPE_UNANSWERED)) {
			setTitle(titlePrefix + getString(R.string.title_all_unanswered));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_unanswered, android.R.layout.simple_spinner_item);
		}
		else if (mQueryType.equals(TYPE_USER)) {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
			mUserName = data.getQueryParameter("uname");
			if (mUserName == null) mUserName = "#" + String.valueOf(mUserID);
			setTitle(titlePrefix + getString(R.string.title_user_questions).replace("%s", mUserName));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_user, android.R.layout.simple_spinner_item);
		}
		else if (mQueryType.equals(TYPE_FAVORITES)) {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
			mUserName = data.getQueryParameter("uname");
			if (mUserName == null) mUserName = "#" + String.valueOf(mUserID);
			setTitle(titlePrefix + getString(R.string.title_user_favorites).replace("%s", mUserName));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_favorites, android.R.layout.simple_spinner_item);
		}
		else if (mQueryType.equals(TYPE_SEARCH)) {
			mInTitle = data.getQueryParameter("intitle");
			mNotTagged = data.getQueryParameter("nottagged");
			setTitle(titlePrefix + getString(R.string.title_search_results));
			mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_search, android.R.layout.simple_spinner_item);
		}
		
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		mSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOrderAdapter = ArrayAdapter.createFromResource(this, R.array.q_order, android.R.layout.simple_spinner_item);
		mOrderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		if (inState == null) {
			mQuestions = new ArrayList<Question>();
			if (data.getQueryParameter("tagged") != null) {
				mTagged = new ArrayList<String>(Arrays.asList(data.getQueryParameter("tagged").split(" ")));
			}
		}
		else {
			mQuestions = (ArrayList<Question>) inState.getSerializable("mQuestions");
			if (inState.get("mTagged") != null) mTagged = (ArrayList<String>) inState.get("mTagged");
			mPage = inState.getInt("mPage");
			mSort = inState.getInt("mSort");
			if (inState.getBoolean("isAsc")) mOrder = Order.ASC;
			mIsRequestOngoing = false;
		}
		mAdapter = new QuestionsAdapter(mContext, mQuestions, onTagClicked);
		mListView = (ListView)findViewById(R.id.questions);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(onQuestionClicked);
		mListView.setOnScrollListener(onQuestionsScrolled);
		
		if (inState == null) getQuestions();
		else mListView.setSelection(inState.getInt("scroll"));
		
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mQuestions", (ArrayList<Question>) mQuestions);
		if (mTagged != null && mTagged.size() != 0) outState.putSerializable("mTagged", mTagged);
		outState.putInt("mPage", mPage);
		outState.putInt("mSort", mSort);
		outState.putBoolean("isAsc", mOrder.equals(Order.ASC));
		outState.putInt("scroll", mListView.getFirstVisiblePosition());
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		if (mIsRequestOngoing == false) {
	    	getMenuInflater().inflate(R.menu.questions, menu);
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
    		final Spinner order = (Spinner)v.findViewById(R.id.spinner_order);
    		order.setAdapter(mOrderAdapter);
    		b.setView(v);
    		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mSort = sort.getSelectedItemPosition();
					if (order.getSelectedItemPosition() == 0) mOrder = Order.DESC;
					else mOrder = Order.ASC;
					mNoMoreQuestions = false;
					mPage = 1;
					getQuestions();
				}
			});
    		b.setNegativeButton(android.R.string.cancel, null);
    		b.create().show();
    		break;
    	}
    	return false;
    }
	
	private void getQuestions() {
		mIsRequestOngoing = true;
		setProgressBarIndeterminateVisibility(true);
		new GetQuestionsAsync().execute();
	}
	
	private class GetQuestionsAsync extends AsyncTask<Void, Void, List<Question>> {
		private Exception mException;
		
		@Override
		protected List<Question> doInBackground(Void... queries) {
			try {
				if (mQueryType.equals(TYPE_ALL)) {
					QuestionQuery query = new QuestionQuery();
					query.setBody(false).setAnswers(false).setPageSize(mPageSize).setPage(mPage);
					if (mTagged != null) query.setTags(mTagged);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(QuestionQuery.Sort.activity()); break;
						case 1: query.setSort(QuestionQuery.Sort.votes()); break;
						case 2: query.setSort(QuestionQuery.Sort.creation()); break;
						case 3: query.setSort(QuestionQuery.Sort.featured()); break;
						case 4: query.setSort(QuestionQuery.Sort.hot()); break;
						case 5: query.setSort(QuestionQuery.Sort.week()); break;
						case 6: query.setSort(QuestionQuery.Sort.month()); break;
						}
					}
					return mAPI.listQuestions(query);
				}
				else if (mQueryType.equals(TYPE_UNANSWERED)) {
					UnansweredQuery query = new UnansweredQuery();
					query.setBody(false).setAnswers(false).setPageSize(mPageSize).setPage(mPage);
					if (mTagged != null) query.setTags(mTagged);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(UnansweredQuery.Sort.creation()); break;
						case 1: query.setSort(UnansweredQuery.Sort.votes()); break;
						}
					}
					return mAPI.listUnansweredQuestions(query);
				}
				else if (mQueryType.equals(TYPE_USER)) {
					UserQuestionQuery query = new UserQuestionQuery();
					query.setBody(false).setAnswers(false).setPageSize(mPageSize).setPage(mPage);
					query.setIds(mUserID);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(UserQuestionQuery.Sort.activity()); break;
						case 1: query.setSort(UserQuestionQuery.Sort.views()); break;
						case 2: query.setSort(UserQuestionQuery.Sort.creation()); break;
						case 3: query.setSort(UserQuestionQuery.Sort.votes()); break;
						}
					}
					return mAPI.getQuestionsByUserId(query);
				}
				else if (mQueryType.equals(TYPE_FAVORITES)) {
					FavoriteQuery query = new FavoriteQuery();
					query.setBody(false).setAnswers(false).setPageSize(mPageSize).setPage(mPage);
					query.setIds(mUserID);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(FavoriteQuery.Sort.activity()); break;
						case 1: query.setSort(FavoriteQuery.Sort.views()); break;
						case 2: query.setSort(FavoriteQuery.Sort.creation()); break;
						case 3: query.setSort(FavoriteQuery.Sort.added()); break;
						case 4: query.setSort(FavoriteQuery.Sort.votes()); break;
						}
					}
					return mAPI.getFavoriteQuestionsByUserId(query);
				}
				else if (mQueryType.equals(TYPE_SEARCH)) {
					SearchQuery query = new SearchQuery();
					query.setPageSize(mPageSize).setPage(mPage);
					if (mInTitle != null) query.setInTitle(mInTitle);
					if (mTagged != null) query.setTags(mTagged);
					if (mNotTagged != null) query.setNotTagged(mNotTagged);
					query.setOrder(mOrder);
					if (mSort > -1) {
						switch(mSort) {
						case 0: query.setSort(SearchQuery.Sort.activity()); break;
						case 1: query.setSort(SearchQuery.Sort.views()); break;
						case 2: query.setSort(SearchQuery.Sort.creation()); break;
						case 3: query.setSort(SearchQuery.Sort.votes()); break;
						}
					}
					return mAPI.search(query);
				}
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Question> result) {
			setProgressBarIndeterminateVisibility(false);
			mIsRequestOngoing = false;
			if (mException != null) {
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.questions_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get questions", mException);
			}
			else {
				if (mPage == 1) mQuestions.clear();
				if (result.size() < mPageSize) mNoMoreQuestions = true;
				mQuestions.addAll(result);
				mAdapter.notifyDataSetChanged();
				if (mQuestions.size() == 0) {
					findViewById(R.id.empty).setVisibility(View.VISIBLE);
					mListView.setVisibility(View.GONE);
				}
			}
		}
	}
	
	private OnClickListener onTagClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// these query types do not support tags
			if (mQueryType.equals(TYPE_USER) || mQueryType.equals(TYPE_FAVORITES)) {
				Log.i(Const.TAG, "Tags not supported by API for this query type");
				return;
			}
			final String tag = ((TextView) v).getText().toString();
			String[] items;
			if (mTagged == null || mTagged.size() == 0) {
				items = new String[1];
				items[0] = tag;
			}
			else {
				items = new String[2];
				items[0] = tag;
				StringBuffer buf = new StringBuffer(tag);
				for (String t: mTagged) {
					buf.append(", ").append(t);
				}
				items[1] = buf.toString();
			}
			AlertDialog.Builder b = new AlertDialog.Builder(mContext);
			b.setTitle(R.string.title_menu_tag);
			b.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which) {
					case 0:
						if (mTagged == null) mTagged = new ArrayList<String>();
						mTagged.clear();
						mTagged.add(tag);
						break;
					case 1:
						mTagged.add(tag);
						break;
					}
					mPage = 1;
					getQuestions();
				}
			});
			b.create().show();
		}
	};
	
	private OnItemClickListener onQuestionClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (!mIsStartedForResult) {
				Intent i = new Intent(mContext, QuestionActivity.class);
				String uri = "droidstack://question" +
					"?endpoint=" + Uri.encode(mEndpoint) +
					"&qid=" + id;
				i.setData(Uri.parse(uri));
				startActivity(i);
			}
			else {
				Question q = mQuestions.get(position);
				Intent i = new Intent();
				i.putExtra("id", q.getPostId());
				i.putExtra("title", q.getTitle());
				i.putExtra("tags", q.getTags().toArray());
				i.putExtra("score", q.getScore());
				i.putExtra("answers", q.getAnswerCount());
				i.putExtra("views", q.getViewCount());
				i.putExtra("accepted", q.getAcceptedAnswerId());
				setResult(RESULT_OK, i);
				finish();
			}
		}
	};
	
	private OnScrollListener onQuestionsScrolled = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (mIsRequestOngoing == false && mNoMoreQuestions == false && firstVisibleItem + visibleItemCount == totalItemCount) {
				mPage++;
				getQuestions();
			}
		}
	};
	
}
