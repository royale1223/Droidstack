package org.droidstack.adapter;

import java.net.URL;
import java.util.HashMap;
import java.util.List;

import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.utils.StackUtils;

import org.droidstack.R;
import org.droidstack.util.Const;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class UsersAdapter extends BaseAdapter {
	
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
	
	private class GetAvatar extends AsyncTask<String, Void, Bitmap> {
		
		private Exception exception;
		private String hash;

		@Override
		protected Bitmap doInBackground(String... params) {
			hash = params[0];
			int size = 64;
			DisplayMetrics metrics = new DisplayMetrics();
			((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
			size *= metrics.density;
			try {
				URL avatarURL = new URL("http://www.gravatar.com/avatar/" + hash + "?s=" + size + "&d=identicon&r=PG");
				return BitmapFactory.decodeStream(avatarURL.openStream());
			}
			catch (Exception e) {
				exception = e;
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			if (exception != null) {
				Log.e(Const.TAG, "Failed to retrieve avatar", exception);
			}
			else {
				avatars.put(hash, result);
				notifyDataSetChanged();
			}
			
		}
		
	}
	
	private Context context;
	private List<User> users;
	private boolean loading;
	private final HashMap<String, Bitmap> avatars = new HashMap<String, Bitmap>();
	
	public UsersAdapter(Context context, List<User> users) {
		this.context = context;
		this.users = users;
	}
	
	public void setLoading(boolean isLoading) {
		if (loading == isLoading) return;
		loading = isLoading;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if (loading) return users.size()+1;
		else return users.size();
	}

	@Override
	public Object getItem(int position) {
		try {
			return users.get(position);
		}
		catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		try {
			return users.get(position).getId();
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
		if (position == users.size()) return false;
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (position == users.size()) return View.inflate(context, R.layout.item_loading, null);
		User user = users.get(position);
		View v = convertView;
		Tag t;
		if (v == null || v.getTag() == null) {
			v = View.inflate(context, R.layout.item_user, null);
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
			new GetAvatar().execute(user.getEmailHash());
			t.avatar.setImageResource(R.drawable.noavatar);
		}
		
		return v;
	}

}
