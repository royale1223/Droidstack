package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class HeaderItem extends MultiItem {
	
	private String mTitle;
	private boolean mLoading = false;;
	
	private class Tag {
		public TextView title;
		public ProgressBar loading;
	}
	
	public HeaderItem(String title, boolean loading) throws NullPointerException {
		if (title == null) throw new NullPointerException("No title supplied");
		mTitle = title;
		mLoading = loading;
	}

	public HeaderItem(String title) throws NullPointerException {
		this(title, false);
	}
	
	public boolean isEnabled() { return false; }

	@Override
	public void bindView(View view, Context context) {
		Tag tag = (Tag) view.getTag();
		tag.title.setText(mTitle);
		if (mLoading) tag.loading.setVisibility(View.VISIBLE);
		else tag.loading.setVisibility(View.GONE);
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		View v = View.inflate(context, R.layout.item_header, null);
		Tag tag = new Tag();
		tag.title = (TextView) v.findViewById(R.id.title);
		tag.loading = (ProgressBar) v.findViewById(R.id.loading);
		v.setTag(tag);
		return v;
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_header;
	}

}
