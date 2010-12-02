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
	
	private final Context context;
	private final LayoutInflater inflater;
	private final List<Answer> answers;
	private final Resources resources;
	
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
	
	@Override
	public int getCount() {
		return answers.size();
	}
	
	@Override
	public Object getItem(int position) {
		return answers.get(position);
	}

	@Override
	public long getItemId(int position) {
		return answers.get(position).getPostId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Answer a = answers.get(position);
		View v;
		Tag t;
		
		if (convertView == null) {
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
