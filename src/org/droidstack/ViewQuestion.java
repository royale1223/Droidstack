package org.droidstack;

import java.io.InputStream;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Question;
import net.sf.stackwrap4j.query.QuestionQuery;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;

public class ViewQuestion extends Activity {
	
	public static final String KEY_QID = "question_id";
	
	private int mSiteID;
	private int mQuestionID;
	private String mEndpoint;
	private String mSiteName;
	private String mTemplate;
	
	private Context mContext;
	private SitesDatabase mSitesDatabase;
	private StackWrapper mAPI;
	private Question mQuestion;
	private WebView mWebView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.question);
		
		mContext = this;
		mWebView = (WebView) findViewById(R.id.content);
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
		Intent launchParams = getIntent();
		
		mSiteID = launchParams.getIntExtra(SitesDatabase.KEY_ID, -1);
		mQuestionID = launchParams.getIntExtra(KEY_QID, -1);
		
		mSitesDatabase = new SitesDatabase(mContext);
		mEndpoint = mSitesDatabase.getEndpoint(mSiteID);
		mSiteName = mSitesDatabase.getName(mSiteID);
		mSitesDatabase.dispose();
		
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		setTitle(R.string.loading);
		new FetchQuestionTask().execute();
	}
	
	private void updateView() {
		setTitle(mQuestion.getTitle());
		String html = mTemplate;
		html = html.replace("<%QuestionBody%>", mQuestion.getBody());
		
		mWebView.loadDataWithBaseURL("about:blank", html, "text/html", "utf-8", null);
	}
	
	private class FetchQuestionTask extends AsyncTask<Void, Void, Question> {
		
		private Exception mException;
		
		@Override
		protected void onPreExecute() {
			setProgressBarIndeterminateVisibility(true);
		}
		
		@Override
		protected Question doInBackground(Void... params) {
			try {
				QuestionQuery query = new QuestionQuery();
				query.setBody(true).setIds(mQuestionID);
				return mAPI.getQuestions(query).get(0);
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Question result) {
			setProgressBarIndeterminateVisibility(false);
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
				Log.e(Const.TAG, "Failed to get questions", mException);
			}
			else {
				mQuestion = result;
				updateView();
			}
		}
		
	}
}
