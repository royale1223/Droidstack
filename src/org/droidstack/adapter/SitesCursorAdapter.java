package org.droidstack.adapter;

import java.io.File;
import java.util.HashMap;

import net.sf.stackwrap4j.utils.StackUtils;

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
import android.widget.LinearLayout;
import android.widget.TextView;

public class SitesCursorAdapter extends BaseAdapter {
	
	private Context context;
	private LayoutInflater inflater;
	private Cursor sites;
	private final HashMap<String, Drawable> icons =  new HashMap<String, Drawable>();
	private boolean loading;
	
	private class Tag {
		public final TextView label;
		public final LinearLayout user;
		public final TextView userName;
		public final TextView userRep;
		public final ImageView icon;
		public final ImageView chat;
		
		public Tag(View v) {
			label = (TextView) v.findViewById(R.id.label);
			user = (LinearLayout) v.findViewById(R.id.user_layout);
			userName = (TextView) v.findViewById(R.id.user_name);
			userRep = (TextView) v.findViewById(R.id.user_rep);
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
		// do not rebuild cache if we already have all icons
		if (icons.size() == sites.getCount()) return;
		icons.clear();
		sites.moveToFirst();
		while (!sites.isAfterLast()) {
			String endpoint = sites.getString(sites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			if (icons.containsKey(endpoint)) continue;
			String host = Uri.parse(endpoint).getHost();
			File icon = new File(Const.getIconsDir(), host);
			if (icon.exists()) icons.put(endpoint, Drawable.createFromPath(icon.getAbsolutePath()));
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
		String endpoint = SitesDatabase.getEndpoint(site);
		String name = SitesDatabase.getName(site);
		String user = SitesDatabase.getUserName(site);
		int reputation = SitesDatabase.getReputation(site);
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
			h.userName.setText(user);
			if (reputation > 0)
				h.userRep.setText(StackUtils.formatRep(reputation));
			else
				h.userRep.setText(String.valueOf(reputation));
			h.user.setVisibility(View.VISIBLE);
		}
		else h.user.setVisibility(View.GONE);
		if (icons.containsKey(endpoint)) h.icon.setImageDrawable(icons.get(endpoint));
		else h.icon.setImageResource(R.drawable.missing_icon);
		h.chat.setTag(position);
		return v;
	}

}
