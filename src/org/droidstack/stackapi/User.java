package org.droidstack.stackapi;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
	
	public final String name;
	public final int rep;
	
	public User(JSONObject json) throws JSONException {
		name = json.getString("display_name");
		rep = json.getInt("reputation");
	}
	
}
