package org.droidstack.adapter;

import java.util.List;

import net.sf.stackwrap4j.entities.Answer;

import org.droidstack.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AnswersAdapter extends BaseAdapter {
	
	private Context context;
	private List<Answer> answers;
	private Resources resources;
	private boolean loading;
	
	private class Tag {
		public TextView score;
		public TextView title;
		
		public Tag(View v) {
			score = (TextView) v.findViewById(R.id.score);
			title = (TextView) v.findViewById(R.id.title);
		}
	}
	
	public AnswersAdapter(Context ctx, List<Answer> data) {
		context = ctx;
		answers = data;
		resources = context.getResources();
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (loading) return answers.size()+1;
		else return answers.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return answers.get(position);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		try {
			return answers.get(position).getPostId();
		}
		catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		if (loading) return false;
		else return true;
	}

	@Override
	public boolean isEnabled(int position) {
		if (position == answers.size()) return false;
		return true;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == answers.size()) return View.inflate(context, R.layout.item_loading, null);
		Answer a = (Answer) getItem(position);
		View v;
		Tag t;
		
		if (convertView == null || convertView.getTag() == null) {
			v = View.inflate(context, R.layout.item_answer, null);
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
