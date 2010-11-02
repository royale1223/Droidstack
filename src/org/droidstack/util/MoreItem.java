package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MoreItem extends MultiItem {
	
	private final Intent intent;
	private final Context context;
	private final LayoutInflater inflater;
	
	public MoreItem(Intent intent, Context context) {
		this.intent = intent;
		this.context = context;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context) {
		// nothing to do
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		return inflater.inflate(R.layout.item_more, null);
	}
	
	@Override
	public void onClick() {
		context.startActivity(intent);
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_more;
	}

}
