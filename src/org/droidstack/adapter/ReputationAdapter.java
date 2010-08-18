package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.entities.Reputation;

import org.droidstack.R;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ReputationAdapter extends BaseAdapter {

	private Context context;
	private List<Reputation> changes;
	private boolean loading;
	
	private class Tag {
		TextView rep_pos;
		TextView rep_neg;
		TextView title;
		public Tag(View v) {
			rep_pos = (TextView) v.findViewById(R.id.rep_pos);
			rep_neg = (TextView) v.findViewById(R.id.rep_neg);
			title = (TextView) v.findViewById(R.id.title);
		}
	}
	
	public ReputationAdapter(Context ctx, List<Reputation> data) {
		context = ctx;
		changes = data;
	}

	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (loading) return changes.size()+1;
		else return changes.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return changes.get(position);
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
		if (position == changes.size()) return false;
		return true;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == changes.size()) return View.inflate(context, R.layout.item_loading, null);
		Reputation r = changes.get(position);
		View v;
		Tag t;
		
		if (convertView == null || convertView.getTag() == null) {
			v = View.inflate(context, R.layout.item_rep, null);
			t = new Tag(v);
			v.setTag(t);
		}
		else {
			v = convertView;
			t = (Tag) v.getTag();
		}
		
		t.title.setText(r.getTitle());
		t.rep_pos.setText("+" + r.getPositiveRep());
		t.rep_neg.setText("-" + r.getNegativeRep());
		
		if (r.getPositiveRep() == 0) t.rep_pos.setVisibility(View.GONE);
		else t.rep_pos.setVisibility(View.VISIBLE);
		
		if (r.getNegativeRep() == 0) t.rep_neg.setVisibility(View.GONE);
		else t.rep_neg.setVisibility(View.VISIBLE);
		
		return v;
	}

}
