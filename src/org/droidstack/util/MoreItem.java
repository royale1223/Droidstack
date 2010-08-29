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
	public void bindView(View view, Context context) {
		// nothing to do
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		return View.inflate(context, R.layout.item_more, null);
	}
	
	@Override
	public void onClick() {
		mContext.startActivity(mIntent);
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_more;
	}

}
