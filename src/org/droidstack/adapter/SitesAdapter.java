package org.droidstack.adapter;

import java.io.File;

import org.droidstack.R;
import org.droidstack.util.Const;
import org.droidstack.util.SitesDatabase;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class SitesAdapter extends BaseAdapter {
	
	private Context context;
	private Cursor bookmarked;
	private Cursor others;
	private File icons;
	private boolean loading;
	
	private class Tag {
		public TextView label;
		public ImageView icon;
		
		public Tag(View v) {
			label = (TextView) v.findViewById(R.id.label);
			icon = (ImageView) v.findViewById(R.id.icon);
		}
	}
	
	public SitesAdapter(Context ctx, Cursor bookmarked, Cursor others) {
		context = ctx;
		this.bookmarked = bookmarked;
		this.others = others;
		icons = Const.getIconsDir();
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		int count = bookmarked.getCount() + others.getCount() + 1;
		if (bookmarked.getCount() > 0) count++;
		return count;
	}

	@Override
	public Object getItem(int position) {
		int b = bookmarked.getCount();
		if (b > 0) {
			if (position == 0) return R.string.bookmarked;
			position--;
			if (position < b) {
				bookmarked.moveToPosition(position);
				return bookmarked;
			}
			position -= b;
		}
		if (position == 0) return R.string.all_sites;
		position--;
		others.moveToPosition(position);
		return others;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof Integer) return IGNORE_ITEM_VIEW_TYPE;
		else return 1;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		try {
			int res = (Integer) getItem(position);
			return false;
		}
		catch (Exception e) {
			return true;
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Object item = getItem(position);
		if (item instanceof Integer) {
			View v = View.inflate(context, R.layout.item_header, null);
			((TextView)v.findViewById(R.id.title)).setText((Integer)item);
			if (loading && ((Integer)item) == R.string.all_sites) {
				v.findViewById(R.id.loading).setVisibility(View.VISIBLE);
			}
			return v;
		}
		else {
			Cursor site = (Cursor) item;
			String name = site.getString(site.getColumnIndex(SitesDatabase.KEY_NAME));
			String endpoint = site.getString(site.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
			View v;
			Tag h;
			if (convertView == null || convertView.getTag() == null) {
				v = View.inflate(context, R.layout.item_site, null);
				h = new Tag(v);
				v.setTag(h);
			}
			else {
				v = convertView;
				h = (Tag) convertView.getTag();
			}
			h.label.setText(name);
			File icon = new File(icons, Uri.parse(endpoint).getHost());
			if (icon.exists()) {
				h.icon.setImageDrawable(Drawable.createFromPath(icon.getAbsolutePath()));
			}
			else {
				h.icon.setImageResource(R.drawable.icon);
			}
			return v;
		}
	}

}
