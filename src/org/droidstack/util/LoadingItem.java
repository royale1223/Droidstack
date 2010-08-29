package org.droidstack.util;

import org.droidstack.R;
import org.droidstack.adapter.MultiAdapter.MultiItem;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class LoadingItem extends MultiItem {

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
		return View.inflate(context, R.layout.item_loading, null);
	}

	@Override
	public int getLayoutResource() {
		return R.layout.item_loading;
	}

}
