package org.droidstack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

public class SitesDatabase {
	
	public static final String KEY_DOMAIN = BaseColumns._ID;
	public static final String KEY_NAME = "name";
	public static final String KEY_UID = "uid";
	public static final String KEY_ENDPOINT = "endpoint";
	
	private static final String DATABASE_NAME = "stackexchange";
	private static final String TABLE_NAME = "sites";
	private static final int VERSION = 4;
	
	private final SitesOpenHelper mOpenHelper;
	private final SQLiteDatabase mDatabase;
	
	public SitesDatabase(Context context) {
		mOpenHelper = new SitesOpenHelper(context);
		mDatabase = mOpenHelper.getWritableDatabase();
	}
	
	public long addSite(String domain, String name, long userID, String endpoint) {
		ContentValues values = new ContentValues();
		values.put(KEY_DOMAIN, domain);
		values.put(KEY_NAME, name);
		values.put(KEY_UID, userID);
		values.put(KEY_ENDPOINT, endpoint);
		
		long r = mDatabase.replace(TABLE_NAME, null, values);
		return r;
	}
	
	public int removeSite(String site) {
		return mDatabase.delete(TABLE_NAME, KEY_DOMAIN + " = ?", new String[] { site });
	}
	
	public Cursor getSites() {
		return query(null, null, null);
	}
	
	public long getUserID(String domain) {
		Cursor c = query(KEY_DOMAIN + " = ?", new String[] { domain }, new String[] { KEY_UID });
		if (c.getCount() == 0) {
			return 0;
		}
		c.moveToFirst();
		long userID = c.getLong(0);
		c.close();
		return userID;
	}
	
	public String getName(String domain) {
		Cursor c = query(KEY_DOMAIN + " = ?", new String[] { domain }, new String[] { KEY_NAME });
		if (c.getCount() == 0) {
			return null;
		}
		c.moveToFirst();
		String name = c.getString(0);
		c.close();
		return name;
	}
	
	public String getEndpoint(String domain) {
		Cursor c = query(KEY_DOMAIN + " = ?", new String[] { domain }, new String[] { KEY_ENDPOINT });
		if (c.getCount() == 0) {
			return null;
		}
		c.moveToFirst();
		String endpoint = c.getString(0);
		c.close();
		return endpoint;
	}
	
	public ContentValues getSite(String domain) {
		Cursor c = query(KEY_DOMAIN + " = ?",
			new String[] { domain },
			new String[] { KEY_DOMAIN, KEY_NAME, KEY_UID, KEY_ENDPOINT });
		if (c.getCount() == 0) return null;
		c.moveToFirst();
		ContentValues data = new ContentValues();
		data.put(KEY_DOMAIN, c.getString(0));
		data.put(KEY_NAME, c.getString(1));
		data.put(KEY_UID, c.getLong(2));
		data.put(KEY_ENDPOINT, c.getString(3));
		return data;
	}
	
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_NAME);
		Cursor cursor = builder.query(mDatabase, columns, selection, selectionArgs, null, null, null);
		return cursor;
	}
	
	private class SitesOpenHelper extends SQLiteOpenHelper {
		
		private static final String SITES_TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME + "(" +
			KEY_DOMAIN + " TEXT PRIMARY KEY, " +
			KEY_NAME + " TEXT, " +
			KEY_UID + " NUMERIC, " +
			KEY_ENDPOINT + " TEXT)";
		
		public SitesOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SITES_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
			onCreate(db);
		}
	}
	
	public void dispose() {
		
		mDatabase.close();
		mOpenHelper.close();
		
	}
	
}
