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
	public View bindView(View view, Context context) {
		if (view.getTag(R.layout.item_loading) != null) return view;
		return newView(context, null);
	}

	@Override
	public View newView(Context context, ViewGroup parent) {
		View v = View.inflate(context, R.layout.item_loading, null);
		v.setTag(R.layout.item_loading, true);
		return v;
	}

}
