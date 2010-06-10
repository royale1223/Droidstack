package org.droidstack;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class Site extends Activity {
	
	private static final int POS_ALL = 0;
	private static final int POS_UNANSWERED = 1;
	private static final int POS_MY_QUESTIONS = 2;
	private static final int POS_FAVORITES = 3;
	private static final int POS_MY_ANSWERS = 4;
	
	private SitesDatabase mSitesDatabase;
	private String mDomain;
	private String mSiteName;
	private long mUserID;
	private ListView mSiteActionsList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.site);
		
		mDomain = getIntent().getData().getHost();
		
		mSitesDatabase = new SitesDatabase(getApplicationContext());
		mUserID = mSitesDatabase.getUserID(mDomain);
		mSiteName = mSitesDatabase.getName(mDomain);
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
				whatToLaunch.setData(Uri.parse("stack://" + mDomain + "/questions"));
				break;
			case POS_UNANSWERED:
				whatToLaunch = new Intent(getApplicationContext(), Questions.class);
				whatToLaunch.setAction(Intent.ACTION_VIEW);
				whatToLaunch.setData(Uri.parse("stack://" + mDomain + "/questions/unanswered"));
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
				whatToLaunch.setData(Uri.parse("stack://" + mDomain + "/users/" + mUserID + "/questions"));
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
				whatToLaunch.setData(Uri.parse("stack://" + mDomain + "/users/" + mUserID + "/favorites"));
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
			if (whatToLaunch != null) startActivity(whatToLaunch);
		}
	};
	
}
