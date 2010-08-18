package org.droidstack.adapter;

import java.util.Date;
import java.util.List;

import net.sf.stackwrap4j.entities.Question;

import org.droidstack.R;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class QuestionsAdapter extends BaseAdapter {
	
	private Context context;
	private List<Question> questions;
	private Resources resources;
	private boolean loading;
	private LinearLayout.LayoutParams tagLayout;
	private OnClickListener tagClickListener;
	
	private class Tag {
		public TextView title;
		public TextView score;
		public TextView answers;
		public TextView answerLabel;
		public TextView views;
		public TextView bounty;
		public LinearLayout tags;
		
		public Tag(View v) {
			title = (TextView) v.findViewById(R.id.title);
			score = (TextView) v.findViewById(R.id.votesN);
			answers = (TextView) v.findViewById(R.id.answersN);
			answerLabel = (TextView) v.findViewById(R.id.answersL);
			views = (TextView) v.findViewById(R.id.viewsN);
			bounty = (TextView) v.findViewById(R.id.bounty);
			tags = (LinearLayout) v.findViewById(R.id.tags);
		}
	}
	
	public QuestionsAdapter(Context ctx, List<Question> data, OnClickListener onTagClicked) {
		context = ctx;
		questions = data;
		tagLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		tagLayout.setMargins(0, 0, 5, 0);
		resources = context.getResources();
		tagClickListener = onTagClicked;
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (loading) return questions.size()+1;
		else return questions.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return questions.get(position);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		try {
			return questions.get(position).getPostId();
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
		if (position == questions.size()) return false;
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == questions.size()) return View.inflate(context, R.layout.item_loading, null); 
		Question q = (Question) getItem(position);
		View v;
		TextView tagView;
		Tag h;
		
		if (convertView == null || convertView.getTag() == null) {
			v = View.inflate(context, R.layout.item_question, null);
			h = new Tag(v);
			v.setTag(h);
		}
		else {
			v = convertView;
			h = (Tag) convertView.getTag();
		}
		
		h.title.setText(q.getTitle());
		h.score.setText(String.valueOf(q.getScore()));
		h.answers.setText(String.valueOf(q.getAnswerCount()));
		h.views.setText(String.valueOf(q.getViewCount()));
		
		h.bounty.setVisibility(View.GONE);
		if (q.getBountyAmount() > 0 && new Date(q.getBountyClosesDate()).before(new Date())) {
			h.bounty.setText("+" + String.valueOf(q.getBountyAmount()));
			h.bounty.setVisibility(View.VISIBLE);
		}
		
		h.tags.removeAllViews();
		for (String tag: q.getTags()){
			tagView = (TextView) View.inflate(context, R.layout.tag, null);
			tagView.setText(tag);
			tagView.setOnClickListener(tagClickListener);
			h.tags.addView(tagView, tagLayout);
		}
		
		if (q.getAnswerCount() == 0) {
			h.answers.setBackgroundResource(R.color.no_answers_bg);
			h.answerLabel.setBackgroundResource(R.color.no_answers_bg);
			h.answers.setTextColor(resources.getColor(R.color.no_answers_text));
			h.answerLabel.setTextColor(resources.getColor(R.color.no_answers_text));
		}
		else {
			h.answers.setBackgroundResource(R.color.some_answers_bg);
			h.answerLabel.setBackgroundResource(R.color.some_answers_bg);
			if (q.getAcceptedAnswerId() > 0) {
				h.answers.setTextColor(resources.getColor(R.color.answer_accepted_text));
				h.answerLabel.setTextColor(resources.getColor(R.color.answer_accepted_text));
			}
			else {
				h.answers.setTextColor(resources.getColor(R.color.some_answers_text));
				h.answerLabel.setTextColor(resources.getColor(R.color.some_answers_text));
			}
		}
		
		return v;
	}

}
