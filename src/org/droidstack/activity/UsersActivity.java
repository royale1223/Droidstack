package org.droidstack.activity;

import net.sf.stackwrap4j.entities.User;

import org.droidstack.R;
import org.droidstack.adapter.UsersAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class UsersActivity extends ListActivity implements TextWatcher {
	
	private String mEndpoint;
	
	private UsersAdapter mAdapter;
	private boolean isStartedForResult;
	
	private EditText mFilter;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.users);

		if (Intent.ACTION_PICK.equals(getIntent().getAction())) isStartedForResult = true;
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		
		mFilter = (EditText) findViewById(R.id.filter);
		mFilter.addTextChangedListener(this);
		
		mAdapter = new UsersAdapter(this, mEndpoint);
		
		setListAdapter(mAdapter);
		
		mAdapter.getFilter().filter(mFilter.getText());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		User u = mAdapter.getUser(position);
		if (!isStartedForResult) {
			Intent i = new Intent(UsersActivity.this, UserActivity.class);
			String uri = "droidstack://user" +
				"?endpoint=" + Uri.encode(mEndpoint) +
				"&uid=" + u.getId();
			i.setData(Uri.parse(uri));
			startActivity(i);
		}
		else {
			Intent i = new Intent();
			i.putExtra("endpoint", mEndpoint);
			i.putExtra("uid", u.getId());
			i.putExtra("name", u.getDisplayName());
			i.putExtra("rep", u.getReputation());
			i.putExtra("emailHash", u.getEmailHash());
			setResult(RESULT_OK, i);
			finish();
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		mAdapter.getFilter().filter(s);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// not used
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// not used
	}
	
}
