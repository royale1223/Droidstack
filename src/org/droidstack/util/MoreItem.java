package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

public class MoreItem extends MultiItem {
	
	private Intent mIntent;
	private Context mContext;
	
	public MoreItem(Intent i, Context ctx) {
		mIntent = i;
		mContext = ctx;
	}

	@Override
	public View bindView(View view, Context context) {
		try {
			Boolean b = (Boolean) view.getTag(R.layout.item_more);
			if (b == null || b.booleanValue() == false) throw new NullPointerException();
			return view;
		}
		catch (Exception e) {
			return newView(context, null);
		}
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		View v = View.inflate(context, R.layout.item_more, null);
		v.setTag(R.layout.item_more, true);
		return v;
	}
	
	@Override
	public void onClick() {
		mContext.startActivity(mIntent);
	}

}
