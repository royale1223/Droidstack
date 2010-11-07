package org.droidstack.activity;

import org.droidstack.R;
import org.droidstack.adapter.TagAutoCompleteAdapter;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;
import android.widget.MultiAutoCompleteTextView.CommaTokenizer;

public class SiteActivity extends ListActivity {
	
	private static final int POS_ALL = 0;
	private static final int POS_UNANSWERED = 1;
	private static final int POS_SEARCH = 2;
	private static final int POS_TAGS = 3;
	private static final int POS_USERS = 4;
	private static final int POS_MY_QUESTIONS = 5;
	private static final int POS_FAVORITES = 6;
	private static final int POS_MY_ANSWERS = 7;
	private static final int POS_MY_PROFILE = 8;
	
	private String mEndpoint;
	private String mSiteName;
	private String mUserName;
	private int mUserID;
	private ArrayAdapter<String> mAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.site);
		
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
		mAdapter = new ArrayAdapter<String>(this, R.layout.item_siteaction, R.id.title, items);
		setListAdapter(mAdapter);
	}
	
	@Override
	public boolean onSearchRequested() {
		// bits stolen from the Android source code ;)
		Dialog diag = new Dialog(this, android.R.style.Theme_Panel);
		diag.setContentView(R.layout.dialog_search);
		
		WindowManager.LayoutParams lp = diag.getWindow().getAttributes();
		lp.width = ViewGroup.LayoutParams.FILL_PARENT;
		lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		lp.gravity = Gravity.FILL_VERTICAL | Gravity.FILL_HORIZONTAL;
		lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
		
		diag.getWindow().setAttributes(lp);
		
		diag.setCanceledOnTouchOutside(true);
		
		diag.show();

		TagAutoCompleteAdapter tagAdapter = new TagAutoCompleteAdapter(this, mEndpoint);
		final EditText intitleEdit = (EditText) diag.findViewById(R.id.intitle);
		final MultiAutoCompleteTextView taggedEdit = (MultiAutoCompleteTextView) diag.findViewById(R.id.tagged);
		taggedEdit.setTokenizer(new CommaTokenizer());
		taggedEdit.setAdapter(tagAdapter);
		final MultiAutoCompleteTextView nottaggedEdit = (MultiAutoCompleteTextView) diag.findViewById(R.id.nottagged);
		nottaggedEdit.setTokenizer(new CommaTokenizer());
		nottaggedEdit.setAdapter(tagAdapter);
		final ImageButton searchButton = (ImageButton) diag.findViewById(R.id.search);
		
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				StringBuilder buf;
				String intitle = intitleEdit.getText().toString();
				String tagged = taggedEdit.getText().toString();
				String nottagged = nottaggedEdit.getText().toString();
				
				buf = new StringBuilder();
				for (String tag: tagged.split(",")) {
					buf.append(tag.trim()).append(" ");
				}
				tagged = buf.toString().trim();
				
				buf = new StringBuilder();
				for (String tag: nottagged.split(",")) {
					buf.append(tag.trim()).append(" ");
				}
				nottagged = buf.toString().trim();
				
				if (intitle.length() > 0 || tagged.length() > 0 || nottagged.length() > 0) {
					Intent i = new Intent(SiteActivity.this, QuestionsActivity.class);
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
		});
		return false;
	}
	
	@Override
	protected void onListItemClick(ListView parent, View view, int position, long id) {
		Class<?> activity = null;
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
		case POS_TAGS:
			activity = TagsActivity.class;
			uri = "droidstack://tags?";
			break;
		case POS_USERS:
			activity = UsersActivity.class;
			uri = "droidstack://users?";
			break;
		case POS_MY_QUESTIONS:
			if (mUserID == 0) {
				Toast.makeText(SiteActivity.this,
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
				Toast.makeText(SiteActivity.this,
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
				Toast.makeText(SiteActivity.this,
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
				Toast.makeText(SiteActivity.this,
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
			Intent i = new Intent(SiteActivity.this, activity);
			i.setData(Uri.parse(uri));
			startActivity(i);
		}
	}
	
}
