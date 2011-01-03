package org.droidstack.activity;

import net.sf.stackwrap4j.entities.Tag;

import org.droidstack.R;
import org.droidstack.adapter.TagsAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

public class TagsActivity extends ListActivity implements TextWatcher {

	private String mEndpoint;
	
	private TagsAdapter mAdapter;
	
	private EditText mFilter;
	
	private boolean isStartedForResult = false;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.tags); 
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) isStartedForResult = true;
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		
		mAdapter = new TagsAdapter(this, mEndpoint);
		setListAdapter(mAdapter);
		
		mFilter = (EditText) findViewById(R.id.filter);
		mFilter.addTextChangedListener(this);
		
		mAdapter.getFilter().filter(mFilter.getText());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Tag t = mAdapter.getTag(position);
		if (!isStartedForResult) {
			Intent i = new Intent(TagsActivity.this, QuestionsActivity.class);
			String uri = "droidstack://questions/all" +
				"?endpoint=" + Uri.encode(mEndpoint) +
				"&tagged=" + Uri.encode(t.getName());
			i.setData(Uri.parse(uri));
			startActivity(i);
		}
		else {
			Intent i = new Intent();
			i.putExtra("name", t.getName());
			i.putExtra("count", t.getCount());
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
		// unused
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// unused
	}
	
}
