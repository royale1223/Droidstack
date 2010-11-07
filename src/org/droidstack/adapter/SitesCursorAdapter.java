package org.droidstack.adapter;

import java.io.File;
import java.util.ArrayList;

import org.droidstack.R;
import org.droidstack.util.Const;
import org.droidstack.util.SitesDatabase;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SitesCursorAdapter extends BaseAdapter {
	
	private Context context;
	private LayoutInflater inflater;
	private Cursor sites;
	private final ArrayList<Drawable> icons = new ArrayList<Drawable>();
	private boolean loading;
	
	private class Tag {
		public final TextView label;
		public final TextView user;
		public final ImageView icon;
		public final ImageView chat;
		
		public Tag(View v) {
			label = (TextView) v.findViewById(R.id.label);
			user = (TextView) v.findViewById(R.id.user);
			icon = (ImageView) v.findViewById(R.id.icon);
			chat = (ImageView) v.findViewById(R.id.chat);
		}
	}
	
	public SitesCursorAdapter(Context context, Cursor sites) {
		this.context = context;
		this.sites = sites;
		inflater = LayoutInflater.from(context);
		buildIconCache();
	}
	
	private void buildIconCache() {
		icons.clear();
		sites.moveToFirst();
		while (!sites.isAfterLast()) {
			String endpoint = sites.getString(sites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			String host = Uri.parse(endpoint).getHost();
			File icon = new File(Const.getIconsDir(), host);
			if (icon.exists())
				icons.add(Drawable.createFromPath(icon.getAbsolutePath()));
			else
				icons.add(context.getResources().getDrawable(R.drawable.missing_icon));
			sites.moveToNext();
		}
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return sites.getCount();
	}

	@Override
	public Object getItem(int position) {
		sites.moveToPosition(position);
		return sites;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public void notifyDataSetChanged() {
		buildIconCache();
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Cursor site = (Cursor) getItem(position);
		String name = SitesDatabase.getName(site);
		String user = SitesDatabase.getUserName(site);
		View v;
		Tag h;
		if (convertView == null || convertView.getTag() == null) {
			v = inflater.inflate(R.layout.item_site, null);
			h = new Tag(v);
			v.setTag(h);
		}
		else {
			v = convertView;
			h = (Tag) convertView.getTag();
		}
		h.label.setText(name);
		if (user != null && user.length() > 0) {
			h.user.setText(user);
			h.user.setVisibility(View.VISIBLE);
		}
		else h.user.setVisibility(View.GONE);
		h.icon.setImageDrawable(icons.get(position));
		h.chat.setTag(position);
		return v;
	}

}
