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
	
	public SitesArrayAdapter(Context context, List<Site> sites) {
		this.context = context;
		this.sites = sites;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return sites.size();
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
	public View getView(int position, View convertView, ViewGroup parent) {
		Site site = sites.get(position);
		View v;
		if (convertView == null)
			v = inflater.inflate(R.layout.item_pick_site, null);
		else v = convertView;
		((CheckedTextView) v).setText(site.getName());
		return v;
	}

}
