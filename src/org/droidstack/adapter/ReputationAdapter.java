package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.entities.Reputation;

import org.droidstack.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ReputationAdapter extends BaseAdapter {

	private final Context context;
	private final LayoutInflater inflater;
	private final List<Reputation> changes;
	
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
	
	public ReputationAdapter(Context context, List<Reputation> changes) {
		this.context = context;
		this.changes = changes;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return changes.size();
	}

	@Override
	public Object getItem(int position) {
		return changes.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Reputation r = changes.get(position);
		View v;
		Tag t;
		
		if (convertView == null || convertView.getTag() == null) {
			v = inflater.inflate(R.layout.item_rep, null);
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
