package org.droidstack.adapter;

import java.util.Date;
import java.util.List;

import net.sf.stackwrap4j.entities.Question;

import org.droidstack.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuestionsAdapter extends BaseAdapter {
	
	private final Context context;
	private final LayoutInflater inflater;
	private final List<Question> questions;
	private Resources resources;
	private final LinearLayout.LayoutParams tagLayout;
	private OnClickListener tagClickListener;
	
	private class Tag {
		public TextView title;
		public TextView votes;
		public TextView votesLabel;
		public TextView answers;
		public TextView answersLabel;
		public TextView views;
		public TextView viewsLabel;
		public TextView bounty;
		public LinearLayout tags;
		
		public Tag(View v) {
			title = (TextView) v.findViewById(R.id.title);
			votes = (TextView) v.findViewById(R.id.votesN);
			votesLabel = (TextView) v.findViewById(R.id.votesL);
			answers = (TextView) v.findViewById(R.id.answersN);
			answersLabel = (TextView) v.findViewById(R.id.answersL);
			views = (TextView) v.findViewById(R.id.viewsN);
			viewsLabel = (TextView) v.findViewById(R.id.viewsL);
			bounty = (TextView) v.findViewById(R.id.bounty);
			tags = (LinearLayout) v.findViewById(R.id.tags);
		}
	}
	
	public QuestionsAdapter(Context context, List<Question> questions, OnClickListener onTagClicked) {
		this.context = context;
		this.questions = questions;
		tagLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		tagLayout.setMargins(0, 0, 5, 0);
		resources = context.getResources();
		tagClickListener = onTagClicked;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return questions.size();
	}
	
	@Override
	public Object getItem(int position) {
		return questions.get(position);
	}

	@Override
	public long getItemId(int position) {
		return questions.get(position).getPostId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Question q = questions.get(position);
		View v;
		TextView tagView;
		Tag h;
		
		if (convertView == null) {
			v = inflater.inflate(R.layout.item_question, null);
			h = new Tag(v);
			v.setTag(h);
		}
		else {
			v = convertView;
			h = (Tag) convertView.getTag();
		}
		
		h.title.setText(q.getTitle());
		h.votes.setText(String.valueOf(q.getScore()));
		if (q.getScore() == 1) h.votesLabel.setText(R.string.vote);
		else h.votesLabel.setText(R.string.votes);
		h.answers.setText(String.valueOf(q.getAnswerCount()));
		if (q.getAnswerCount() == 1) h.answersLabel.setText(R.string.answer);
		else h.answersLabel.setText(R.string.answers);
		h.views.setText(String.valueOf(q.getViewCount()));
		if (q.getViewCount() == 1) h.viewsLabel.setText(R.string.view);
		else h.viewsLabel.setText(R.string.views);
		
		h.bounty.setVisibility(View.GONE);
		if (q.getBountyAmount() > 0 && new Date(q.getBountyClosesDate()).before(new Date())) {
			h.bounty.setText("+" + String.valueOf(q.getBountyAmount()));
			h.bounty.setVisibility(View.VISIBLE);
		}
		
		h.tags.removeAllViews();
		for (String tag: q.getTags()){
			tagView = (TextView) inflater.inflate(R.layout.tag, null);
			tagView.setText(tag);
			tagView.setOnClickListener(tagClickListener);
			h.tags.addView(tagView, tagLayout);
		}
		
		if (q.getAnswerCount() == 0) {
			h.answers.setBackgroundResource(R.color.no_answers_bg);
			h.answersLabel.setBackgroundResource(R.color.no_answers_bg);
			h.answers.setTextColor(resources.getColor(R.color.no_answers_text));
			h.answersLabel.setTextColor(resources.getColor(R.color.no_answers_text));
		}
		else {
			h.answers.setBackgroundResource(R.color.some_answers_bg);
			h.answersLabel.setBackgroundResource(R.color.some_answers_bg);
			if (q.getAcceptedAnswerId() > 0) {
				h.answers.setTextColor(resources.getColor(R.color.answer_accepted_text));
				h.answersLabel.setTextColor(resources.getColor(R.color.answer_accepted_text));
			}
			else {
				h.answers.setTextColor(resources.getColor(R.color.some_answers_text));
				h.answersLabel.setTextColor(resources.getColor(R.color.some_answers_text));
			}
		}
		
		return v;
	}

}
