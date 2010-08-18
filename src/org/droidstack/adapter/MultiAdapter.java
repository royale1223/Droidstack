package org.droidstack.adapter;

import java.util.ArrayList;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class MultiAdapter extends BaseAdapter implements OnItemClickListener {
	
	private Context context;
	
	private ArrayList<MultiItem> listItems;
	
	public static abstract class MultiItem {
		
		public boolean isEnabled() { return true; }
		
		public void onClick() {}
		
		public abstract View bindView(View view, Context context);
		public abstract View newView(Context context, ViewGroup parent);
	}
	
	public MultiAdapter(Context context) {
		this.context = context;
		
		listItems = new ArrayList<MultiAdapter.MultiItem>();
	}
	
	public void addItem(MultiItem item) {
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
		
		v = item.bindView(v, context);
		
		return v;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		listItems.get(position).onClick();
	}
}