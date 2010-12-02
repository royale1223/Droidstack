package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HeaderItem extends MultiItem {
	
	private final Context context;
	private final LayoutInflater inflater;
	private final String title;
	
	public HeaderItem(Context context, String title) throws NullPointerException {
		if (title == null) throw new NullPointerException("No title supplied");
		this.context = context;
		inflater = LayoutInflater.from(context);
		this.title = title;
	}
	
	public boolean isEnabled() { return false; }

	@Override
	public void bindView(View view, Context context) {
		((TextView) view).setText(title);
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		TextView tv = (TextView) inflater.inflate(R.layout.item_header, null);
		return tv;
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_header;
	}

}
