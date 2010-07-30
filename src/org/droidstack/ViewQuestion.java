package org.droidstack;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.jtpl.Template;
import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Answer;
import net.sf.stackwrap4j.entities.Comment;
import net.sf.stackwrap4j.entities.Question;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.AnswerQuery;
import net.sf.stackwrap4j.query.QuestionQuery;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class ViewQuestion extends Activity {
	
	public static final String KEY_QID = "question_id";
	
	private int mQuestionID;
	private String mEndpoint;
	private String mTemplate;
	private int mAnswerCount;
	private int mCurAnswer = -1;
	private int mPage = 0;
	private int mPageSize;
	
	private Context mContext;
	private StackWrapper mAPI;
	private Question mQuestion;
	private List<Answer> mAnswers;
	
	private WebView mWebView;
	private TextView mAnswerCountView;
	private Button mNextButton;
	private Button mPreviousButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.question);
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		mContext = this;
		try {
			InputStream is = getAssets().open("question.html", AssetManager.ACCESS_BUFFER);
			StringBuilder builder = new StringBuilder();
			int b;
			while((b = is.read()) != -1) {
				builder.append((char)b);
			}
			mTemplate = builder.toString();
		}
		catch (Exception e) {
			Log.e(Const.TAG, "wtf asset load fail", e);
			finish();
		}
		Uri data = getIntent().getData();
		try {
			mQuestionID = Integer.parseInt(data.getQueryParameter("qid"));
		}
		catch (Exception e) {
			Log.e(Const.TAG, "ViewQuestion: qid could not be parsed into an integer: " + data.getQueryParameter("qid"));
			finish();
		}
		mEndpoint = data.getQueryParameter("endpoint");
		
		mWebView = (WebView) findViewById(R.id.content);
		try {
			// try to enable caching, useful for avatars
			WebSettings.class.getMethod("setAppCacheEnabled", new Class[] { boolean.class }).invoke(mWebView.getSettings(), true);
			WebSettings.class.getMethod("setCacheMode", new Class[] { int.class }).invoke(mWebView.getSettings(), WebSettings.LOAD_CACHE_ELSE_NETWORK);
		}
		catch(Exception e) {
			// app cache not supported, must be < 2.1
			Log.i(Const.TAG, "Unable to enable WebView caching", e);
		}
		mAnswerCountView = (TextView) findViewById(R.id.answer_count);
		mNextButton = (Button) findViewById(R.id.next);
		mPreviousButton = (Button) findViewById(R.id.previous);
		mPageSize = getPreferences(Context.MODE_PRIVATE).getInt(Const.PREF_PAGESIZE, Const.DEF_PAGESIZE);
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		if (savedInstanceState == null) {
			mAnswers = new ArrayList<Answer>();
			setTitle(R.string.loading);
			new FetchQuestionTask().execute();
		}
		else {
			mQuestion = (Question) savedInstanceState.getSerializable("mQuestion");
			mAnswers = (ArrayList<Answer>) savedInstanceState.getSerializable("mAnswers");
			mCurAnswer = savedInstanceState.getInt("mCurAnswer");
			mPage = savedInstanceState.getInt("mPage");
			mAnswerCount = savedInstanceState.getInt("mAnswerCount");
			updateView();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mQuestion", mQuestion);
		outState.putSerializable("mAnswers", (ArrayList<Answer>) mAnswers);
		outState.putInt("mCurAnswer", mCurAnswer);
		outState.putInt("mPage", mPage);
		outState.putInt("mAnswerCount", mAnswerCount);
	}
	
	private void updateView() {
		setTitle(mQuestion.getTitle());
		
		if (mCurAnswer == -1) {
			Template tpl = new Template(mTemplate);
			try {
				for (Comment c: mQuestion.getComments()) {
					tpl.assign("CBODY", c.getBody());
					User owner = c.getOwner();
					if (owner == null) tpl.assign("CAUTHOR", "?");
					else tpl.assign("CAUTHOR", owner.getDisplayName());
					tpl.assign("CSCORE", String.valueOf(c.getScore()));
					if (c.getScore() > 0) tpl.parse("main.post.comment.score");
					tpl.parse("main.post.comment");
				}
			}
			catch (Exception e) {
				Log.e(Const.TAG, "wtf Question.getComments() error", e);
				finish();
			}
			for (String tag: mQuestion.getTags()) {
				tpl.assign("TAG", tag);
				tpl.parse("main.post.tags.tag");
			}
			tpl.parse("main.post.tags");
			User owner = mQuestion.getOwner();
			tpl.assign("QBODY", mQuestion.getBody());
			tpl.assign("QSCORE", String.valueOf(mQuestion.getScore()));
			tpl.assign("QAHASH", owner.getEmailHash());
			tpl.assign("QANAME", owner.getDisplayName());
			tpl.assign("QAREP", String.valueOf(owner.getReputation()));
			tpl.parse("main.post");
			tpl.parse("main");
			mWebView.loadDataWithBaseURL("about:blank", tpl.out(), "text/html", "utf-8", null);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mAnswerCountView.setText(String.valueOf(mAnswerCount));
			}
			else {
				mAnswerCountView.setText(getString(R.string.number_of_answers).replace("%s", String.valueOf(mAnswerCount)));
			}
			
			mPreviousButton.setEnabled(false);
			mNextButton.setEnabled(false);
			if (mAnswerCount > 0) mNextButton.setEnabled(true);
		}
		else {
			Template tpl = new Template(mTemplate);
			Answer answer = mAnswers.get(mCurAnswer);
			try {
				for (Comment c: answer.getComments()) {
					tpl.assign("CBODY", c.getBody());
					User owner = c.getOwner();
					if (owner == null) tpl.assign("CAUTHOR", "?");
					else tpl.assign("CAUTHOR", owner.getDisplayName());
					tpl.assign("CSCORE", String.valueOf(c.getScore()));
					if (c.getScore() > 0) tpl.parse("main.post.comment.score");
					tpl.parse("main.post.comment");
				}
			}
			catch (Exception e) {
				Log.e(Const.TAG, "wtf Answer.getComments() error", e);
			}
			User owner = answer.getOwner();
			tpl.assign("QBODY", answer.getBody());
			tpl.assign("QSCORE", String.valueOf(answer.getScore()));
			// String.valueOf is needed because getEmailHash() returns null sometimes 
			tpl.assign("QAHASH", String.valueOf(owner.getEmailHash()));
			tpl.assign("QANAME", owner.getDisplayName());
			tpl.assign("QAREP", String.valueOf(owner.getReputation()));
			if (answer.isAccepted()) tpl.parse("main.post.accepted");
			tpl.parse("main.post");
			tpl.parse("main");
			mWebView.loadDataWithBaseURL("about:blank", tpl.out(), "text/html", "utf-8", null);
			
			mPreviousButton.setEnabled(false);
			mNextButton.setEnabled(false);
			if (mCurAnswer > -1) mPreviousButton.setEnabled(true);
			if (mCurAnswer < mAnswerCount-1) mNextButton.setEnabled(true);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mAnswerCountView.setText((mCurAnswer+1) + "\nof\n" + mAnswerCount);
			}
			else {
				mAnswerCountView.setText((mCurAnswer+1) + "/" + mAnswerCount);
			}
		}
	}
	
	public void changePost(View target) {
		switch(target.getId()) {
		case R.id.next:
			mCurAnswer++;
			if (mCurAnswer >= mAnswers.size()) {
				new FetchMoreAnswers().execute();
			}
			else {
				updateView();
			}
			break;
		case R.id.previous:
			mCurAnswer--;
			updateView();
			break;
		}
	}
	
	private class FetchQuestionTask extends AsyncTask<Void, Void, Void> {
		
		private Exception mException;
		private ProgressDialog progressDialog;
		
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, false);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				QuestionQuery query = new QuestionQuery();
				query.setBody(true).setComments(true).setAnswers(false).setIds(mQuestionID);
				mQuestion = mAPI.getQuestions(query).get(0);
				mAnswerCount = mQuestion.getAnswerCount();
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			setProgressBarIndeterminateVisibility(false);
			progressDialog.dismiss();
			if (mException != null) {
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.question_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get question", mException);
			}
			else {
				updateView();
			}
		}
		
	}
	
	private class FetchMoreAnswers extends AsyncTask<Void, Void, List<Answer>> {
		
		private Exception mException;
		private ProgressDialog progressDialog;
		
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, false);
		}
		
		@Override
		protected List<Answer> doInBackground(Void... params) {
			try {
				AnswerQuery query = new AnswerQuery();
				query.setBody(true).setComments(true).setIds(mQuestionID);
				query.setPageSize(mPageSize).setPage(++mPage);
				query.setSort(AnswerQuery.Sort.votes());
				return mAPI.getAnswersByQuestionId(query);
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(List<Answer> result) {
			setProgressBarIndeterminateVisibility(false);
			progressDialog.dismiss();
			if (mException != null) {
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.more_answers_error)
					.setNeutralButton(android.R.string.ok, null)
					.create().show();
				Log.e(Const.TAG, "Failed to get answers", mException);
				mPage--;
				mCurAnswer--;
			}
			else {
				mAnswers.addAll(result);
				updateView();
			}
		}
		
	}
}
