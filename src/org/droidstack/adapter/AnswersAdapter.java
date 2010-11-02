package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.entities.Answer;

import org.droidstack.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AnswersAdapter extends BaseAdapter {
	
	private String title;
	private final Context context;
	private final LayoutInflater inflater;
	private final List<Answer> answers;
	private final Resources resources;
	private boolean loading;
	
	private class Tag {
		public TextView score;
		public TextView title;
		
		public Tag(View v) {
			score = (TextView) v.findViewById(R.id.score);
			title = (TextView) v.findViewById(R.id.title);
		}
	}
	
	public AnswersAdapter(Context context, List<Answer> answers) {
		this.context = context;
		this.answers = answers;
		resources = context.getResources();
		inflater = LayoutInflater.from(context);
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	public void setTitle(String title) {
		if (title.equals(this.title)) return;
		this.title = title;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		int count = answers.size();
		if (loading) count++;
		if (title != null) count++;
		return count;
	}
	
	@Override
	public int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public int getItemViewType(int position) {
		if (title != null) {
			if (position == 0) return IGNORE_ITEM_VIEW_TYPE;
			position--;
		}
		if (position == answers.size()) return IGNORE_ITEM_VIEW_TYPE;
		return 0;
	}
	
	@Override
	public Object getItem(int position) {
		if (title != null) position--;
		try {
			return answers.get(position);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		if (title != null) position--;
		try {
			return answers.get(position).getPostId();
		}
		catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		if (loading || title != null) return false;
		else return true;
	}

	@Override
	public boolean isEnabled(int position) {
		if (title != null) {
			if (position == 0) return false;
			position--;
		}
		if (position == answers.size()) return false;
		return true;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (title != null) {
			if (position == 0) {
				View v = inflater.inflate(R.layout.item_header, null);
				((TextView)v.findViewById(R.id.title)).setText(title);
				return v;
			}
			position--;
		}
		if (position == answers.size()) return inflater.inflate(R.layout.item_loading, null);
		Answer a = answers.get(position);
		View v;
		Tag t;
		
		if (convertView == null || convertView.getTag() == null) {
			v = inflater.inflate(R.layout.item_answer, null);
			t = new Tag(v);
			v.setTag(t);
		}
		else {
			v = convertView;
			t = (Tag) v.getTag();
		}
		
		t.score.setText(String.valueOf(a.getScore()));
		t.title.setText(a.getTitle());
		
		if (a.isAccepted()) {
			t.score.setBackgroundResource(R.color.score_max_bg);
			t.score.setTextColor(resources.getColor(R.color.score_max_text));
		}
		else if (a.getScore() == 0) {
			t.score.setBackgroundResource(R.color.score_neutral_bg);
			t.score.setTextColor(resources.getColor(R.color.score_neutral_text));
		}
		else if (a.getScore() > 0) {
			t.score.setBackgroundResource(R.color.score_high_bg);
			t.score.setTextColor(resources.getColor(R.color.score_high_text));
		}
		else {
			t.score.setBackgroundResource(R.color.score_low_bg);
			t.score.setTextColor(resources.getColor(R.color.score_low_text));
		}
		
		return v;
	}

}
