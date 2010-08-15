package org.droidstack.activity;

import org.droidstack.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SiteActivity extends ListActivity {
	
	private static final int POS_ALL = 0;
	private static final int POS_UNANSWERED = 1;
	private static final int POS_SEARCH = 2;
	private static final int POS_MY_QUESTIONS = 3;
	private static final int POS_FAVORITES = 4;
	private static final int POS_MY_ANSWERS = 5;
	private static final int POS_MY_PROFILE = 6;
	
	private Context mContext;
	private String mEndpoint;
	private String mSiteName;
	private String mUserName;
	private int mUserID;
	private ArrayAdapter<String> mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.site);
		
		mContext = this;
		
		Uri data = getIntent().getData();
		
		mEndpoint = data.getQueryParameter("endpoint");
		mSiteName = data.getQueryParameter("name");
		mUserName = data.getQueryParameter("uname");
		try {
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
		}
		catch (Exception e) { }
		
		setTitle(mSiteName);
		
		String[] items = getResources().getStringArray(R.array.site_actions);
		mAdapter = new ArrayAdapter<String>(mContext, R.layout.item_siteaction, R.id.title, items);
		setListAdapter(mAdapter);
		
		getListView().setOnItemClickListener(onItemClicked);
	}
	
	@Override
	public boolean onSearchRequested() {
		View dialogView = getLayoutInflater().inflate(R.layout.search_dialog, null);
		final EditText intitleEdit = (EditText) dialogView.findViewById(R.id.intitle);
		final EditText taggedEdit = (EditText) dialogView.findViewById(R.id.tagged);
		final EditText nottaggedEdit = (EditText) dialogView.findViewById(R.id.nottagged);
		new AlertDialog.Builder(mContext)
			.setTitle(R.string.search_title)
			.setView(dialogView)
			.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String intitle = intitleEdit.getText().toString();
					String tagged = taggedEdit.getText().toString();
					String nottagged = nottaggedEdit.getText().toString();
					if (intitle.length() > 0 || tagged.length() > 0 || nottagged.length() > 0) {
						Intent i = new Intent(mContext, QuestionsActivity.class);
						String uri = "droidstack://questions/search" +
							"?endpoint=" + Uri.encode(mEndpoint) +
							"&name=" + Uri.encode(mSiteName);
						if (intitle.length() > 0) uri += "&intitle=" + Uri.encode(intitle);
						if (tagged.length() > 0) uri += "&tagged=" + Uri.encode(tagged);
						if (nottagged.length() > 0) uri += "&nottagged=" + Uri.encode(nottagged);
						i.setData(Uri.parse(uri));
						startActivity(i);
					}
				}
			}).create().show();
		return false;
	}

	private OnItemClickListener onItemClicked = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Class activity = null;
			String uri = null;
			switch(position) {
			case POS_ALL:
				activity = QuestionsActivity.class;
				uri = "droidstack://questions/all?";
				break;
			case POS_UNANSWERED:
				activity = QuestionsActivity.class;
				uri = "droidstack://questions/unanswered?";
				break;
			case POS_SEARCH:
				onSearchRequested();
				break;
			case POS_MY_QUESTIONS:
				if (mUserID == 0) {
					Toast.makeText(mContext,
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				activity = QuestionsActivity.class;
				uri = "droidstack://questions/user" +
					"?uid=" + mUserID +
					"&uname=" + Uri.encode(mUserName) + "&";
				break;
			case POS_FAVORITES:
				if (mUserID == 0) {
					Toast.makeText(mContext,
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				activity = QuestionsActivity.class;
				uri = "droidstack://questions/favorites" +
					"?uid=" + mUserID +
					"&uname=" + Uri.encode(mUserName) + "&";
				break;
			case POS_MY_ANSWERS:
				if (mUserID == 0) {
					Toast.makeText(mContext,
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				activity = AnswersActivity.class;
				uri = "droidstack://answers/user" +
					"?uid=" + mUserID +
					"&uname=" + Uri.encode(mUserName) + "&";
				break;
			case POS_MY_PROFILE:
				if (mUserID == 0) {
					Toast.makeText(mContext,
							R.string.no_userid,
							Toast.LENGTH_LONG).show();
					break;
				}
				activity = UserActivity.class;
				uri = "droidstack://user?uid=" + mUserID + "&";
				break;
			}
			if (activity != null && uri != null) {
				uri += "endpoint=" + Uri.encode(mEndpoint) + "&name=" + Uri.encode(mSiteName);
				Intent i = new Intent(mContext, activity);
				i.setData(Uri.parse(uri));
				startActivity(i);
			}
		}
	};
	
}
