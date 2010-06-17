package org.droidstack;

import org.droidstack.stackapi.QuestionsQuery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SiteActions extends Activity {
	
	private static final int POS_ALL = 0;
	private static final int POS_UNANSWERED = 1;
	private static final int POS_MY_QUESTIONS = 2;
	private static final int POS_FAVORITES = 3;
	private static final int POS_MY_ANSWERS = 4;
	
	private SitesDatabase mSitesDatabase;
	private int mSiteID;
	private String mSiteName;
	private String mUserName;
	private long mUserID;
	private ListView mSiteActionsList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.site);
		
		mSiteID = getIntent().getIntExtra(SitesDatabase.KEY_ID, -1);
		
		mSitesDatabase = new SitesDatabase(getApplicationContext());
		mUserID = mSitesDatabase.getUserID(mSiteID);
		mSiteName = mSitesDatabase.getName(mSiteID);
		mUserName = mSitesDatabase.getUserName(mSiteID);
		mSitesDatabase.dispose();
		
		setTitle(mSiteName);
		
		mSiteActionsList = (ListView) findViewById(R.id.SiteActionsList);
		
		mSiteActionsList.setOnItemClickListener(onItemClicked);
	}
	
	private OnItemClickListener onItemClicked = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Intent whatToLaunch = null;
			switch(position) {
			case POS_ALL:
				whatToLaunch = new Intent(getApplicationContext(), Questions.class);
				whatToLaunch.setAction(Intent.ACTION_VIEW);
				whatToLaunch.putExtra(Questions.INTENT_TYPE, QuestionsQuery.QUERY_ALL);
				break;
			case POS_UNANSWERED:
				whatToLaunch = new Intent(getApplicationContext(), Questions.class);
				whatToLaunch.setAction(Intent.ACTION_VIEW);
				whatToLaunch.putExtra(Questions.INTENT_TYPE, QuestionsQuery.QUERY_UNANSWERED);
				break;
			case POS_MY_QUESTIONS:
				if (mUserID == 0) {
					Toast.makeText(getApplicationContext(),
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				whatToLaunch = new Intent(getApplicationContext(), Questions.class);
				whatToLaunch.setAction(Intent.ACTION_VIEW);
				whatToLaunch.putExtra(Questions.INTENT_TYPE, QuestionsQuery.QUERY_USER);
				whatToLaunch.putExtra(SitesDatabase.KEY_UID, mUserID);
				whatToLaunch.putExtra(SitesDatabase.KEY_UNAME, mUserName);
				break;
			case POS_FAVORITES:
				if (mUserID == 0) {
					Toast.makeText(getApplicationContext(),
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				whatToLaunch = new Intent(getApplicationContext(), Questions.class);
				whatToLaunch.setAction(Intent.ACTION_VIEW);
				whatToLaunch.putExtra(Questions.INTENT_TYPE, QuestionsQuery.QUERY_FAVORITES);
				whatToLaunch.putExtra(SitesDatabase.KEY_UID, mUserID);
				whatToLaunch.putExtra(SitesDatabase.KEY_UNAME, mUserName);
				break;
			case POS_MY_ANSWERS:
				if (mUserID == 0) {
					Toast.makeText(getApplicationContext(),
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				// TODO: Implement
				break;
			}
			if (whatToLaunch != null) {
				whatToLaunch.putExtra(SitesDatabase.KEY_ID, mSiteID);
				startActivity(whatToLaunch);
			}
		}
	};
	
}
