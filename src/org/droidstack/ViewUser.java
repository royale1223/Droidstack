package org.droidstack;

import java.net.URL;
import java.util.List;

import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Reputation;
import net.sf.stackwrap4j.entities.User;
import net.sf.stackwrap4j.http.HttpClient;
import net.sf.stackwrap4j.query.ReputationQuery;

import org.droidstack.utils.Const;
import org.droidstack.utils.MultiAdapter;
import org.droidstack.utils.MultiAdapter.MultiItem;
import org.droidstack.utils.MultiHeader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewUser extends ListActivity {
	
	private static final int ITEMS = 3; 
	
	private String mEndpoint;
	private int mUserID;
	private Drawable mAvatarDrawable;
	private StackWrapper mAPI;
	private MultiAdapter mAdapter;
	
	private User mUser;
	private List<Reputation> mRepChanges;
	
	private ImageView mAvatar;
	private TextView mName;
	private TextView mRep;
	
	private Context mContext;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.user);
		
		HttpClient.setTimeout(Const.NET_TIMEOUT);
		
		mContext = (Context) this;
		
		// get launch parameters
		Uri data = getIntent().getData();
		try {
			mEndpoint = data.getQueryParameter("endpoint").toString();
			mUserID = Integer.parseInt(data.getQueryParameter("uid"));
		}
		catch (Exception e) {
			Log.e(Const.TAG, "User: error parsing launch parameters", e);
			finish();
			return;
		}
		
		mAdapter = new MultiAdapter(mContext);
		setListAdapter(mAdapter);
		
		mAvatar = (ImageView) findViewById(R.id.avatar);
		mName = (TextView) findViewById(R.id.name);
		mRep = (TextView) findViewById(R.id.rep);
		
		mAPI = new StackWrapper(mEndpoint, Const.APIKEY);
		new FetchUserDataTask().execute();
	}
	
	private class FetchUserDataTask extends AsyncTask<Void, Void, Void> {

		private Exception mException;
		private ProgressDialog progressDialog;
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(mContext, "", getString(R.string.loading), true, false);
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				// user info
				mUser = mAPI.getUserById(mUserID);
				URL avatarURL = new URL("http://www.gravatar.com/avatar/" + mUser.getEmailHash() + "?s=64&d=identicon&r=PG");
				
				// avatar
				mAvatarDrawable = Drawable.createFromStream(avatarURL.openStream(), null);
				
				// rep changes
				ReputationQuery repQuery = new ReputationQuery();
				repQuery.setPageSize(ITEMS).setIds(mUserID);
				mRepChanges = mAPI.getReputationByUserId(repQuery);
			}
			catch (Exception e) {
				mException = e;
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			if (mException != null) {
				new AlertDialog.Builder(mContext)
					.setTitle(R.string.title_error)
					.setMessage(R.string.user_fetch_error)
					.setCancelable(false)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					}).create().show();
				Log.e(Const.TAG, "Failed to get user data", mException);
			}
			else {
				updateView();
			}
		}
		
	}
	
	private void updateView() {
		mAvatar.setImageDrawable(mAvatarDrawable);
		mName.setText(mUser.getDisplayName());
		String rep = Const.longFormatRep(mUser.getReputation());
		mRep.setText(rep);
		if (mRepChanges.size() > 0) {
			mAdapter.addItem(new MultiHeader("Reputation Changes"));
			for (Reputation r: mRepChanges) {
				mAdapter.addItem(new RepItem(r));
			}
		}
		mAdapter.notifyDataSetChanged();
	}
	
	private class RepItem extends MultiItem {
		
		private Reputation data;
		
		private class Tag {
			TextView rep_pos;
			TextView rep_neg;
			TextView title;
		}
		
		private void prepareView(Tag tag) {
			if (data.getPositiveRep() > 0) {
				tag.rep_pos.setVisibility(View.VISIBLE);
				tag.rep_pos.setText("+" + data.getPositiveRep());
			}
			else {
				tag.rep_pos.setVisibility(View.GONE);
			}
			
			if (data.getNegativeRep() > 0) {
				tag.rep_neg.setVisibility(View.VISIBLE);
				tag.rep_neg.setText("-" + data.getNegativeRep());
			}
			else {
				tag.rep_neg.setVisibility(View.GONE);
			}
			
			tag.title.setText(data.getTitle());
		}
		
		public RepItem(Reputation rep) {
			data = rep;
		}
		
		@Override
		public int getLayoutResource() {
			return R.layout.multi_rep;
		}

		@Override
		public View bindView(View view, Context context) {
			try {
				Tag tag = (Tag) view.getTag(getLayoutResource());
				if (tag == null) throw new NullPointerException();
				prepareView(tag);
				return view;
			}
			catch (Exception e) {
				return newView(context, null);
			}
		}

		@Override
		public View newView(Context context, ViewGroup parent) {
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflater.inflate(R.layout.multi_rep, null);
			Tag tag = new Tag();
			tag.title = (TextView) v.findViewById(R.id.title);
			tag.rep_pos = (TextView) v.findViewById(R.id.rep_pos);
			tag.rep_neg = (TextView) v.findViewById(R.id.rep_neg);
			v.setTag(getLayoutResource(), tag);
			prepareView(tag);
			return v;
		}
		
	}
	
}
