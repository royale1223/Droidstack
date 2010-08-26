package org.droidstack.activity;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Tag;
import net.sf.stackwrap4j.query.TagQuery;

import org.droidstack.R;
import org.droidstack.adapter.TagsAdapter;
import org.droidstack.util.Const;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

public class TagsActivity extends ListActivity {

	private String mEndpoint;
	private int mPage;
	private int mPageSize;
	private boolean noMoreTags = false;
	private boolean isRequestOngoing = false;
	private StackWrapper mAPI;
	
	private TagsAdapter mAdapter;
	private ArrayList<Tag> mTags;
	private Handler mHandler;
	
	private EditText mFilter;
	
	private boolean isStartedForResult = false;
	
	@Override
	protected void onCreate(Bundle inState) {
		super.onCreate(inState);
		setContentView(R.layout.tags); 
		
		if (Intent.ACTION_PICK.equals(getIntent().getAction())) isStartedForResult = true;
		
		Uri data = getIntent().getData();
		mEndpoint = data.getQueryParameter("endpoint");
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);

		mFilter = (EditText) findViewById(R.id.filter);
		mHandler = new Handler();
		
		mPageSize = Const.getPageSize(this);
		mPage = 1;
		
		mTags = new ArrayList<Tag>();
		mAdapter = new TagsAdapter(this, mTags);
		setListAdapter(mAdapter);
		getListView().setOnScrollListener(onScroll);
		getListView().setOnItemClickListener(onClick);
		
		if (inState != null) {
			mTags.addAll((ArrayList<Tag>) inState.getSerializable("mTags"));
			mFilter.setText(inState.getString("filter"));
			mPage = inState.getInt("mPage");
			mAdapter.notifyDataSetChanged();
			getListView().setSelection(inState.getInt("scroll"));
		}
		mFilter.addTextChangedListener(onTextChanged);
		
		if (inState == null) new GetTags().execute();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable("mTags", mTags);
		outState.putString("filter", mFilter.getText().toString());
		outState.putInt("mPage", mPage);
		outState.putInt("scroll", getListView().getFirstVisiblePosition());
	}
	
	private TextWatcher onTextChanged = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			mHandler.removeCallbacks(applyNewFilter);
			mHandler.postDelayed(applyNewFilter, 1500);
		}
	};
	
	private Runnable applyNewFilter = new Runnable() {
		@Override
		public void run() {
			if (isRequestOngoing) return;
			mPage = 1;
			mTags.clear();
			mAdapter.notifyDataSetChanged();
			new GetTags().execute();
		}
	};
	
	private OnItemClickListener onClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Tag t = mTags.get(position);
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
	};
	
	private class GetTags extends AsyncTask<Void, Void, List<Tag>> {
		
		private Exception e;
		
		@Override
		protected void onPreExecute() {
			mAdapter.setLoading(true);
			isRequestOngoing = true;
		}
		
		@Override
		protected List<Tag> doInBackground(Void... params) {
			TagQuery query = new TagQuery();
			query.setPageSize(mPageSize).setPage(mPage);
			query.setFilter(mFilter.getText().toString());
			try {
				return mAPI.listTags(query);
			}
			catch (Exception e) {
				this.e = e;
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(List<Tag> result) {
			isRequestOngoing = false;
			if (e != null) {
				new AlertDialog.Builder(TagsActivity.this)
					.setTitle(R.string.title_error)
					.setMessage(R.string.tags_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get rep changes", e);
			}
			else {
				mTags.addAll(result);
				if (result.size() < mPageSize) {
					noMoreTags = true;
					mAdapter.setLoading(false);
				}
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}
	
	private OnScrollListener onScroll = new OnScrollListener() {
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// not used
		}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (isRequestOngoing == false && noMoreTags == false && totalItemCount > 0 && firstVisibleItem + visibleItemCount == totalItemCount) {
				mPage++;
				new GetTags().execute();
			}
		}
	};
	
}
