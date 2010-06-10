package org.droidstack;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.droidstack.stackapi.Question;
import org.droidstack.stackapi.QuestionsQuery;
import org.droidstack.stackapi.StackAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class Questions extends Activity {
	
	private QuestionsQuery mQuery;
	private StackAPI mAPI;
	private SitesDatabase mSitesDatabase;
	private Uri mQueryURI;
	private String mQueryType;
	private String mDomain;
	private String mEndpoint;
	private String mSiteName;
	private int mPage = 1;
	private int mPageSize;
	private long mUserID = 0;
	private boolean mNoMoreQuestions = false;
	private boolean mIsRequestOngoing = true;
	
	private Resources mResources;
	private Context mContext;
	
	private List<Question> mQuestions;
	private ArrayAdapter<Question> mAdapter;
	private ListView mListView;
	private ArrayAdapter<CharSequence> mSortAdapter;
	private ArrayAdapter<CharSequence> mOrderAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.questions);
		
		mResources = getResources();
		mContext = (Context) this;
		mPageSize = getPreferences(Context.MODE_PRIVATE).getInt(Const.PREF_PAGESIZE, Const.DEF_PAGESIZE);
		
		mQueryURI = getIntent().getData();
		mDomain = mQueryURI.getHost();
		List<String> path = mQueryURI.getPathSegments();
		String title = null;
		if (path.get(0).equals("questions")) {
			if (path.size() == 1) {
				mQueryType = QuestionsQuery.QUERY_ALL;
				title = "All Questions";
				mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_all, android.R.layout.simple_spinner_item);
			}
			else if (path.get(1).equals("unanswered")) {
				mQueryType = QuestionsQuery.QUERY_UNANSWERED;
				title = "Unanswered Questions";
				mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_unanswered, android.R.layout.simple_spinner_item);
			}
		}
		else if (path.get(0).equals("users")) {
			if (path.size() == 3) {
				mUserID = Long.parseLong(path.get(1));
				if (path.get(2).equals("questions")) {
					mQueryType = QuestionsQuery.QUERY_USER;
					title = "User #" + String.valueOf(mUserID) + "'s questions";
					mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_user, android.R.layout.simple_spinner_item);
				}
				else if (path.get(2).equals("favorites")) {
					mQueryType = QuestionsQuery.QUERY_FAVORITES;
					title = "User #" + String.valueOf(mUserID) + "'s favorites";
					mSortAdapter = ArrayAdapter.createFromResource(this, R.array.q_sort_favorites, android.R.layout.simple_spinner_item);
				}
			}
		}
		
		if (mQueryType == null) {
			Log.e(Const.TAG, "Invalid data URI");
			finish();
		}
		
		mSitesDatabase = new SitesDatabase(mContext);
		mEndpoint = mSitesDatabase.getEndpoint(mDomain);
		mSiteName = mSitesDatabase.getName(mDomain);
		mSitesDatabase.dispose();
		mAPI = new StackAPI(mEndpoint, Const.APIKEY);
		mQuery = new QuestionsQuery(mQueryType).setUser(mUserID);
		mSortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mOrderAdapter = ArrayAdapter.createFromResource(this, R.array.q_order, android.R.layout.simple_spinner_item);
		mOrderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mQuery.setPageSize(mPageSize);
		mQuestions = new ArrayList<Question>();
		mAdapter = new QuestionsListAdapter<Question>(getApplicationContext(), 0, mQuestions);
		mListView = (ListView)findViewById(R.id.questions);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(onQuestionClicked);
		mListView.setOnScrollListener(onQuestionsScrolled);
		
		if (title != null) title = mSiteName + ": " + title;
		else title = mSiteName;
		setTitle(title);
		
		getQuestions();
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		if (mIsRequestOngoing == false) {
	    	getMenuInflater().inflate(R.menu.questions, menu);
	    	return true;
		}
		return false;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menu_sort:
    		AlertDialog.Builder b = new AlertDialog.Builder(this);
    		b.setTitle(R.string.menu_sort);
    		View v = getLayoutInflater().inflate(R.layout.questions_menu_sort, null);
    		final Spinner sort = (Spinner)v.findViewById(R.id.spinner_sort); 
    		sort.setAdapter(mSortAdapter);
    		String[] validSortFields = QuestionsQuery.validSortFields.get(mQueryType);
    		for (int i=0; i < validSortFields.length; i++) {
    			if (validSortFields[i].equals(mQuery.getSort())) {
    				sort.setSelection(i);
    				break;
    			}
    		}
    		final Spinner order = (Spinner)v.findViewById(R.id.spinner_order);
    		order.setAdapter(mOrderAdapter);
    		if (mQuery.getOrder().equals(QuestionsQuery.ORDER_DESCENDING)) order.setSelection(0);
    		if (mQuery.getOrder().equals(QuestionsQuery.ORDER_ASCENDING)) order.setSelection(1);
    		b.setView(v);
    		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// UGLY!! But it's Java, what you gonna do
					mQuery.setSort(QuestionsQuery.validSortFields.get(mQueryType)[sort.getSelectedItemPosition()]);
					// a bit hard-coded, but fuck it
					if (order.getSelectedItemPosition() == 0) {
						mQuery.setOrder(QuestionsQuery.ORDER_DESCENDING);
					}
					else {
						mQuery.setOrder(QuestionsQuery.ORDER_ASCENDING);
					}
					mQuestions.clear();
					mPage = 1;
					mQuery.setPage(1);
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
		new GetQuestionsAsync().execute(mQuery);
	}
	
	private class GetQuestionsAsync extends AsyncTask<QuestionsQuery, Void, List<Question>> {
		private Exception mException;
		
		@Override
		protected List<Question> doInBackground(QuestionsQuery... queries) {
			try {
				return mAPI.getQuestions(queries[0]);
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
				Log.e(Const.TAG, mException.getMessage());
			}
			else {
				if (result.size() < mPageSize) mNoMoreQuestions = true;
				mQuestions.addAll(result);
				mAdapter.notifyDataSetChanged();
			}
		}
	}
	
	private class QuestionsListAdapter<E> extends ArrayAdapter<E> {
		
		LayoutInflater inflater;
		private LinearLayout.LayoutParams tagLayout;
		
		private class ViewHolder {
			public TextView title;
			public TextView score;
			public TextView answers;
			public TextView answerLabel;
			public TextView views;
			public TextView bounty;
			public LinearLayout tags;
		}
		
		public QuestionsListAdapter(Context context, int textViewResourceId,
				List<E> objects) {
			super(context, textViewResourceId, objects);
			inflater = getLayoutInflater();
			tagLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			tagLayout.setMargins(0, 0, 5, 0);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Question q = (Question) getItem(position);
			View v;
			TextView tagView;
			ViewHolder h;
			
			if (convertView == null) {
				v = inflater.inflate(R.layout.question_item, null);
				h = new ViewHolder();
				h.title = (TextView) v.findViewById(R.id.title);
				h.score = (TextView) v.findViewById(R.id.votesN);
				h.answers = (TextView) v.findViewById(R.id.answersN);
				h.answerLabel = (TextView) v.findViewById(R.id.answersL);
				h.views = (TextView) v.findViewById(R.id.viewsN);
				h.bounty = (TextView) v.findViewById(R.id.bounty);
				h.tags = (LinearLayout) v.findViewById(R.id.tags);
				v.setTag(h);
			}
			else {
				v = convertView;
				h = (ViewHolder) convertView.getTag();
			}
			
			h.title.setText(q.title);
			h.score.setText(String.valueOf(q.score));
			h.answers.setText(String.valueOf(q.answerCount));
			h.views.setText(String.valueOf(q.viewCount));
			
			h.bounty.setVisibility(View.GONE);
			if (q.bounty != 0 && q.bountyEnd.before(new Date())) {
				h.bounty.setText("+" + String.valueOf(q.bounty));
				h.bounty.setVisibility(View.VISIBLE);
			}
			
			h.tags.removeAllViews();
			for (String tag: q.tags){
				tagView = (TextView) inflater.inflate(R.layout.tag, null);
				tagView.setText(tag);
				tagView.setOnClickListener(onTagClicked);
				h.tags.addView(tagView, tagLayout);
			}
			
			if (q.answerCount == 0) {
				h.answers.setBackgroundResource(R.color.no_answers_bg);
				h.answerLabel.setBackgroundResource(R.color.no_answers_bg);
				h.answers.setTextColor(mResources.getColor(R.color.no_answers_text));
				h.answerLabel.setTextColor(mResources.getColor(R.color.no_answers_text));
			}
			else {
				h.answers.setBackgroundResource(R.color.some_answers_bg);
				h.answerLabel.setBackgroundResource(R.color.some_answers_bg);
				if (q.acceptedAnswerID != 0) {
					h.answers.setTextColor(mResources.getColor(R.color.answer_accepted_text));
					h.answerLabel.setTextColor(mResources.getColor(R.color.answer_accepted_text));
				}
				else {
					h.answers.setTextColor(mResources.getColor(R.color.some_answers_text));
					h.answerLabel.setTextColor(mResources.getColor(R.color.some_answers_text));
				}
			}
			
			return v;
		}
		
	}
	
	private OnClickListener onTagClicked = new OnClickListener() {
		@Override
		public void onClick(View v) {
			String tag = ((TextView)v).getText().toString();
			Log.d(Const.TAG, "Tag clicked: " + tag);
		}
	};
	
	private OnItemClickListener onQuestionClicked = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.d(Const.TAG, "Question clicked: " + mQuestions.get(position).title);
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
				mQuery.setPage(++mPage);
				getQuestions();
			}
		}
	};
	
}
