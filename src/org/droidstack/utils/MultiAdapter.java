package org.droidstack.utils;

import java.util.ArrayList;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;

public class MultiAdapter extends BaseAdapter implements OnItemClickListener {
	private static final int NO_SUCH_ITEM = -1;
	
	private Context context;
	
	private ArrayList<MultiItem> listItems;
	private SparseIntArray viewTypes;
	
	private int viewTypesCount = 0;
	
	public static abstract class MultiItem {
		public abstract int getLayoutResource();
		
		public boolean isEnabled() { return true; }
		
		public void onClick() {}
		
		public abstract View bindView(View view, Context context);
		public abstract View newView(Context context, ViewGroup parent);
	}
	
	public MultiAdapter(Context context) {
		this.context = context;
		
		listItems = new ArrayList<MultiAdapter.MultiItem>();
		viewTypes = new SparseIntArray();
	}
	
	public void addItem(MultiItem item) {
		int resource = item.getLayoutResource();
		
		if (resource != IGNORE_ITEM_VIEW_TYPE) {
			if (viewTypes.get(resource, NO_SUCH_ITEM) == NO_SUCH_ITEM) {
				// Resource not already added, add it
				viewTypes.append(resource, viewTypesCount++);
			}
		}
		
		listItems.add(item);
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
		return viewTypesCount != 0 ? viewTypesCount : 1;
	}

	@Override
	public boolean isEmpty() {
		return listItems.isEmpty();
	}

	@Override
	public boolean isEnabled(int position) {
		final MultiItem item = listItems.get(position);
		
		return item.isEnabled();
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
		
		v = item.bindView(v, context);
		
		return v;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final MultiItem item = listItems.get(position);
		
		item.onClick();
	}
}