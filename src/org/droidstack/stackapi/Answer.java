package org.droidstack.stackapi;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

public class Answer {
	
	public String title;
	public long id;
	public long questionID;
	public boolean accepted;
	public Date created;
	public int score;
	public boolean community;
	
	public Answer(JSONObject json) throws JSONException {
		title = json.getString("title");
		id = json.getLong("answer_id");
		questionID = json.getLong("question_id");
		accepted = json.getBoolean("accepted");
		created = new Date(json.getLong("creation_date"));
		score = json.getInt("score");
		community = json.getBoolean("community_owned");
	}
	
}
