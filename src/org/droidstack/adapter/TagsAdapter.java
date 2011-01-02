package org.droidstack.adapter;

import java.util.ArrayList;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Tag;
import net.sf.stackwrap4j.query.TagQuery;

import org.droidstack.R;
import org.droidstack.util.Const;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class TagsAdapter extends BaseAdapter implements Filterable {
	
	private class TagFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			try {
				FilterResults results = new FilterResults();
				TagQuery query = new TagQuery();
				query.setFilter(constraint.toString()).setPageSize(pageSize);
				List<Tag> tags = api.listTags(query);
				results.count = tags.size();
				results.values = tags;
				return results;
			}
			catch (Exception e) {
				Log.e(Const.TAG, "TagFilter.performFiltering error while fetching", e);
				return null;
			}
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (results == null) return;
			tags.clear();
			tags.addAll((List<Tag>) results.values);
			notifyDataSetChanged();
		}
	}
	
	private static class ViewTag {
		public TextView title;
		public TextView count;
		public ViewTag(View v) {
			title = (TextView) v.findViewById(R.id.title);
			count = (TextView) v.findViewById(R.id.count);
		}
	}
	
	private final Context context;
	private final LayoutInflater inflater;
	private final String endpoint;
	private final StackWrapper api;
	private final int pageSize;
	private final List<Tag> tags = new ArrayList<Tag>();
	private final TagFilter filter = new TagFilter();
	
	public TagsAdapter(Context context, String endpoint) {
		this.context = context;
		this.endpoint = endpoint;
		api = new StackWrapper(endpoint, Const.APIKEY);
		pageSize = Const.getPageSize(context);
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return tags.size();
	}

	@Override
	public Object getItem(int position) {
		return tags.get(position).getName();
	}
	
	public Tag getTag(int position) {
		return tags.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Tag tag = tags.get(position);
		View v = convertView;
		ViewTag t;
		if (v == null || v.getTag() == null) {
			v = inflater.inflate(R.layout.item_tag, null);
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

	@Override
	public Filter getFilter() {
		return filter;
	}

}
