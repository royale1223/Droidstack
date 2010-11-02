package org.droidstack.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class MultiAdapter extends BaseAdapter implements OnItemClickListener {
	private static final int NO_SUCH_ITEM = -1;
	// this hack is needed because the current implementation of ListView
	// only calls this method ONCE, when setAdapter() is called.
	// Calling notifyDataSetChanged() does not reset the cached return value
	// of this method that ListView holds
	private static final int VIEW_TYPE_COUNT = 10;
	
	private final Context context;
	
	private final ArrayList<MultiItem> listItems;
	private final SparseIntArray viewTypes;
	
	private int viewTypeCount;
	
	public static abstract class MultiItem {
		public abstract int getLayoutResource();
		
		public boolean isEnabled() { return true; }
		
		public void onClick() {}
		
		public abstract void bindView(View view, Context context);
		public abstract View newView(Context context, ViewGroup parent);
	}
	
	public MultiAdapter(Context context, int viewTypeCount) {
		this.context = context;
		
		listItems = new ArrayList<MultiAdapter.MultiItem>();
		viewTypes = new SparseIntArray();
		
		this.viewTypeCount = viewTypeCount;
	}
	
	public MultiAdapter(Context context) {
		this(context, VIEW_TYPE_COUNT);
	}
	
	public void addItem(MultiItem item) {
		int resource = item.getLayoutResource();
		
		if (resource != IGNORE_ITEM_VIEW_TYPE) {
			if (viewTypes.get(resource, NO_SUCH_ITEM) == NO_SUCH_ITEM) {
				// Resource not already added, add it
				viewTypes.append(resource, viewTypes.size());
			}
		}
		
		listItems.add(item);
	}
	
	public void clear() {
		listItems.clear();
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public int getItemViewType(int position) {
		final MultiItem item = listItems.get(position);
		
		int resource = item.getLayoutResource();
		
		return resource != IGNORE_ITEM_VIEW_TYPE ? viewTypes.get(resource) : IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public int getViewTypeCount() {
		return viewTypeCount;
	}

	@Override
	public boolean isEmpty() {
		return listItems.isEmpty();
	}

	@Override
	public boolean isEnabled(int position) {
		return listItems.get(position).isEnabled();
	}
	
	@Override
	public int getCount() {
		return listItems.size();
	}

	@Override
	public Object getItem(int position) {
		return listItems.get(position);
	}

	@Override
	public long getItemId(int position) {
		// Position is the same as id in our case
		return position;
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {
		final MultiItem item = listItems.get(position);
		
		if (v == null) {
			v = item.newView(context, parent);
		}
		
		item.bindView(v, context);
		
		return v;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		listItems.get(position).onClick();
	}
}