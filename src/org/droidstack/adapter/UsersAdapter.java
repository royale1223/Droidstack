package org.droidstack.adapter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.query.UserQuery;
import net.sf.stackwrap4j.utils.StackUtils;

import org.droidstack.R;
import org.droidstack.util.Const;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class UsersAdapter extends BaseAdapter implements Filterable {
	
	private class UsersFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			UserQuery query = new UserQuery();
			query.setPageSize(pageSize);
			query.setFilter(constraint.toString());
			FilterResults results = new FilterResults();
			try {
				List<User> users = api.listUsers(query);
				results.values = users;
				results.count = users.size();
			}
			catch (Exception e) {
				Log.e(Const.TAG, "Users filter exception", e);
				return null;
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence constraint,
				FilterResults results) {
			if (results == null) return;
			users.clear();
			users.addAll((List<User>) results.values);
			notifyDataSetChanged();
			new GetAvatars().execute();
		}
		
	}
	
	private class Tag {
		public ImageView avatar;
		public TextView name;
		public TextView rep;
		public Tag(View v) {
			avatar = (ImageView) v.findViewById(R.id.avatar);
			name = (TextView) v.findViewById(R.id.name);
			rep = (TextView) v.findViewById(R.id.rep);
		}
	}
	
	private class GetAvatarsProgress {
		public final String hash;
		public final Bitmap avatar;
		public GetAvatarsProgress(String hash, Bitmap avatar) {
			this.hash = hash;
			this.avatar = avatar;
		}
	}
	
	private GetAvatars getAvatarsInstance;
	
	private class GetAvatars extends AsyncTask<Void, GetAvatarsProgress, Void> {
		
		public GetAvatars() {
			if (getAvatarsInstance != null) {
				getAvatarsInstance.cancel(true);
			}
			getAvatarsInstance = this;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			if (Thread.interrupted()) return null;
			for (User user: users) {
				String hash = user.getEmailHash();
				int size = 64;
				DisplayMetrics metrics = new DisplayMetrics();
				((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
				size *= metrics.density;
				try {
					URL avatarURL = new URL("http://www.gravatar.com/avatar/" + hash + "?s=" + size + "&d=identicon&r=PG");
					Bitmap avatar = BitmapFactory.decodeStream(avatarURL.openStream());
					publishProgress(new GetAvatarsProgress(hash, avatar));
				}
				catch (Exception e) {
					Log.e(Const.TAG, "Error fetching avatar", e);
				}
			}
			return null;
		}
		
		@Override
		protected void onProgressUpdate(GetAvatarsProgress... values) {
			avatars.put(values[0].hash, values[0].avatar);
			notifyDataSetChanged();
		}
		
		@Override
		protected void onPostExecute(Void result) {
			getAvatarsInstance = null;
		}
		
	}
	
	private final Context context;
	private final LayoutInflater inflater;
	private final List<User> users = new ArrayList<User>();
	private final StackWrapper api;
	private final int pageSize;
	private final UsersFilter filter;
	private final HashMap<String, Bitmap> avatars = new HashMap<String, Bitmap>();
	
	public UsersAdapter(Context context, String endpoint) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		api = new StackWrapper(endpoint, Const.APIKEY);
		pageSize = Const.getPageSize(context);
		filter = new UsersFilter();
	}
	
	@Override
	public Filter getFilter() {
		return filter;
	}
	
	@Override
	public int getCount() {
		return users.size();
	}

	@Override
	public Object getItem(int position) {
		return users.get(position).getDisplayName();
	}
	
	public User getUser(int position) {
		return users.get(position);
	}

	@Override
	public long getItemId(int position) {
		return users.get(position).getId();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		User user = users.get(position);
		View v = convertView;
		Tag t;
		if (v == null) {
			v = inflater.inflate(R.layout.item_user, null);
			t = new Tag(v);
			v.setTag(t);
		}
		else {
			t = (Tag) v.getTag();
		}
		
		t.name.setText(user.getDisplayName());
		t.rep.setText(StackUtils.formatRep(user.getReputation()));
		
		if (avatars.containsKey(user.getEmailHash())) {
			t.avatar.setImageBitmap(avatars.get(user.getEmailHash()));
		}
		else {
			t.avatar.setImageResource(R.drawable.noavatar);
		}
		
		return v;
	}

}
