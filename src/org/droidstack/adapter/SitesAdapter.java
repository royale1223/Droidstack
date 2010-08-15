package org.droidstack.adapter;

import java.io.File;

import org.droidstack.R;
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
	private Cursor sites;
	private File icons;
	
	private class Tag {
		public TextView label;
		public ImageView icon;
		
		public Tag(View v) {
			label = (TextView) v.findViewById(R.id.label);
			icon = (ImageView) v.findViewById(R.id.icon);
		}
	}
	
	public SitesAdapter(Context ctx, Cursor data, File iconsBase) {
		context = ctx;
		sites = data;
		icons = iconsBase;
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
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		sites.moveToPosition(position);
		String name = sites.getString(sites.getColumnIndex(SitesDatabase.KEY_NAME));
		String endpoint = sites.getString(sites.getColumnIndex(SitesDatabase.KEY_ENDPOINT));
		View v;
		Tag h;
		if (convertView == null) {
			v = View.inflate(context, R.layout.item_site, null);
			h = new Tag(v);
			v.setTag(h);
		}
		else {
			v = convertView;
			h = (Tag) convertView.getTag();
		}
		h.label.setText(name);
		h.icon.setImageDrawable(Drawable.createFromPath(new File(icons, Uri.parse(endpoint).getHost()).getAbsolutePath()));
		
		return v;
	}

}
