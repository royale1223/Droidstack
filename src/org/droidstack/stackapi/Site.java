package org.droidstack.stackapi;

import org.json.JSONException;
import org.json.JSONObject;

public class Site {
	
	public final String name;
	public final String logo_url;
	public final String api_endpoint;
	public final String site_url;
	public final String description;
	public final String icon_url;
	
	public Site(JSONObject json) throws JSONException {
		name = json.getString("name");
		logo_url = json.getString("logo_url");
		api_endpoint= json.getString("api_endpoint");
		site_url = json.getString("site_url");
		description = json.getString("description");
		icon_url = json.getString("icon_url");
	}
	
}
