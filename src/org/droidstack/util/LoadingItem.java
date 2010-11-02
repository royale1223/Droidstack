package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LoadingItem extends MultiItem {

	private final Context context;
	private final LayoutInflater inflater;
	
	public LoadingItem(Context context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public boolean isEnabled() {
		return false;
	}

	@Override
	public void bindView(View view, Context context) {
		// do nothing
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		return inflater.inflate(R.layout.item_loading, null);
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_loading;
	}

}
