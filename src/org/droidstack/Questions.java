package org.droidstack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class Questions extends Activity {
	
	private QuestionsQuery mQuery;
	private StackAPI mAPI;
	private Uri mQueryURI;
	private String mSite;
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.questions);
		
		mResources = getResources();
		mContext = (Context) this;
		mPageSize = getPreferences(Context.MODE_PRIVATE).getInt(Const.PREF_PAGESIZE, Const.DEF_PAGESIZE);
		
		mQueryURI = getIntent().getData();
		
		mSite = "api." + mQueryURI.getHost();
		List<String> path = mQueryURI.getPathSegments();
		if (path.get(0).equals("questions")) {
			if (path.size() == 1) {
				mQuery = new QuestionsQuery(QuestionsQuery.QUERY_ALL);
			}
			else if (path.get(1).equals("unanswered")) {
				mQuery = new QuestionsQuery(QuestionsQuery.QUERY_UNANSWERED);
			}
		}
		else if (path.get(0).equals("users")) {
			if (path.size() == 3) {
				mUserID = Long.parseLong(path.get(1));
				if (path.get(2).equals("questions")) {
					mQuery = new QuestionsQuery(QuestionsQuery.QUERY_USER).setUser(mUserID);
				}
				else if (path.get(2).equals("favorites")) {
					mQuery = new QuestionsQuery(QuestionsQuery.QUERY_FAVORITES).setUser(mUserID);
				}
			}
		}
		
		if (mQuery == null) {
			Log.e(Const.TAG, "Invalid data URI");
			finish();
		}
		
		mQuery.setPageSize(mPageSize);
		mAPI = new StackAPI(mSite, Const.APIKEY);
		mQuestions = new ArrayList<Question>();
		mAdapter = new QuestionsListAdapter<Question>(getApplicationContext(), 0, mQuestions);
		mListView = (ListView)findViewById(R.id.questions);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(onQuestionClicked);
		mListView.setOnScrollListener(onQuestionsScrolled);
		
		getQuestions();
	}
	
	private void getQuestions() {
		mIsRequestOngoing = true;
		setProgressBarIndeterminateVisibility(true);
		new GetQuestionsAsync().execute(mQuery);
	}
	
	private class GetQuestionsAsync extends AsyncTask<QuestionsQuery, Void, Set<Question>> {
		private Exception mException;
		
		@Override
		protected Set<Question> doInBackground(QuestionsQuery... queries) {
			try {
				return mAPI.getQuestions(queries[0]);
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Set<Question> result) {
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
			public LinearLayout answerLayout;
			public TextView answers;
			public TextView answerLabel;
			public TextView views;
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
				h.score = (TextView) v.findViewById(R.id.score).findViewById(R.id.number);
				h.answerLayout = (LinearLayout) v.findViewById(R.id.answers); 
				h.answers = (TextView) h.answerLayout.findViewById(R.id.number);
				h.answerLabel = (TextView) h.answerLayout.findViewById(R.id.label);
				h.views = (TextView) v.findViewById(R.id.views).findViewById(R.id.number);
				h.tags = (LinearLayout) v.findViewById(R.id.tags);
				((TextView) v.findViewById(R.id.score).findViewById(R.id.label)).setText(R.string.votes);
				h.answerLabel.setText(R.string.answers);
				((TextView) v.findViewById(R.id.views).findViewById(R.id.label)).setText(R.string.views);
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
			
			h.tags.removeAllViews();
			for (String tag: q.tags){
				tagView = (TextView) inflater.inflate(R.layout.tag, null);
				tagView.setText(tag);
				tagView.setOnClickListener(onTagClicked);
				h.tags.addView(tagView, tagLayout);
			}
			
			if (q.answerCount == 0) {
				h.answerLayout.setBackgroundResource(R.color.no_answers_bg);
				h.answers.setTextColor(mResources.getColor(R.color.no_answers_text));
				h.answerLabel.setTextColor(mResources.getColor(R.color.no_answers_text));
			}
			else {
				h.answerLayout.setBackgroundResource(R.color.some_answers_bg);
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
