package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.entities.Tag;

import org.droidstack.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TagsAdapter extends BaseAdapter {
	
	private static class ViewTag {
		public TextView title;
		public TextView count;
		public ViewTag(View v) {
			title = (TextView) v.findViewById(R.id.title);
			count = (TextView) v.findViewById(R.id.count);
		}
	}
	
	private Context context;
	private List<Tag> tags;
	private boolean loading;
	
	public TagsAdapter(Context context, List<Tag> tags) {
		this.context = context;
		this.tags = tags;
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (loading) return tags.size()+1;
		else return tags.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return tags.get(position);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		if (loading) return false;
		else return true;
	}

	@Override
	public boolean isEnabled(int position) {
		if (position == tags.size()) return false;
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == tags.size()) return View.inflate(context, R.layout.item_loading, null);
		Tag tag = tags.get(position);
		View v = convertView;
		ViewTag t;
		if (v == null || v.getTag() == null) {
			v = View.inflate(context, R.layout.item_tag, null);
			t = new ViewTag(v);
			v.setTag(t);
		}
		else {
			t = (ViewTag) v.getTag();
		}
		
		t.title.setText(tag.getName());
		t.count.setText("×" + tag.getCount());
		
		return v;
	}

}
