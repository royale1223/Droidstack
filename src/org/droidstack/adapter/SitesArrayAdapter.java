package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.stackauth.entities.Site;

import org.droidstack.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

public class SitesArrayAdapter extends BaseAdapter {

	private Context context;
	private LayoutInflater inflater;
	private List<Site> sites;
	private boolean loading;
	
	public SitesArrayAdapter(Context context, List<Site> sites) {
		this.context = context;
		this.sites = sites;
		inflater = LayoutInflater.from(context);
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		int count = sites.size();
		if (loading) count++;
		return count;
	}

	@Override
	public Object getItem(int position) {
		return sites.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (position == sites.size()) return IGNORE_ITEM_VIEW_TYPE;
		return 1;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == sites.size()) return inflater.inflate(R.layout.item_loading, null);
		Site site = sites.get(position);
		View v;
		if (convertView == null)
			v = inflater.inflate(R.layout.item_pick_site, null);
		else v = convertView;
		((CheckedTextView) v).setText(site.getName());
		return v;
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
	}

}
