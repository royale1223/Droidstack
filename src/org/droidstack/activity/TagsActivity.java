package org.droidstack.activity;

import net.sf.stackwrap4j.entities.Tag;

import org.droidstack.R;
import org.droidstack.adapter.TagsAdapter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.AdapterView.OnItemClickListener;

public class TagsActivity extends Activity implements OnItemClickListener {

	private String mEndpoint;
	
	private TagsAdapter mAdapter;
	
	private AutoCompleteTextView mFilter;
	
	private boolean isStartedForResult = false;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.tags); 
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) isStartedForResult = true;
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		
		mAdapter = new TagsAdapter(this, mEndpoint);
		mFilter = (AutoCompleteTextView) findViewById(R.id.filter);
		mFilter.setAdapter(mAdapter);
		mFilter.setOnItemClickListener(this);
	}
	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
		}
		finish();
	};
	
}
