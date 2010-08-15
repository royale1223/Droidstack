package org.droidstack.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

public class SitesDatabase {

	public static final String KEY_ENDPOINT = BaseColumns._ID;
	public static final String KEY_NAME = "name";
	public static final String KEY_UID = "uid";
	public static final String KEY_UNAME = "user_name";
	
	private static final String DATABASE_NAME = "stackexchange";
	private static final String TABLE_NAME = "sites";
	private static final int VERSION = 7;
	
	private final SitesOpenHelper mOpenHelper;
	private final SQLiteDatabase mDatabase;
	
	public SitesDatabase(Context context) {
		mOpenHelper = new SitesOpenHelper(context);
		mDatabase = mOpenHelper.getWritableDatabase();
	}
	
	public long addSite(String endpoint, String name, long userID, String uname) {
		ContentValues values = new ContentValues();
		values.put(KEY_ENDPOINT, endpoint);
		values.put(KEY_NAME, name);
		values.put(KEY_UID, userID);
		values.put(KEY_UNAME, uname);
		
		long r = mDatabase.replace(TABLE_NAME, null, values);
		return r;
	}
	
	public int removeSite(String endpoint) {
		return mDatabase.delete(TABLE_NAME, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public Cursor getSites() {
		return query(null, null, null);
	}
	
	public int getUserID(String endpoint) {
		Cursor c = query(KEY_ENDPOINT + " = ?", new String[] { endpoint }, new String[] { KEY_UID });
		if (c.getCount() == 0) {
			return 0;
		}
		c.moveToFirst();
		int userID = c.getInt(0);
		c.close();
		return userID;
	}
	
	public String getUserName(String endpoint) {
		Cursor c = query(KEY_ENDPOINT + " = ?", new String[] { endpoint }, new String[] { KEY_UNAME });
		if (c.getCount() == 0) {
			return null;
		}
		c.moveToFirst();
		String name = c.getString(0);
		c.close();
		return name;
	}
	
	public String getName(String endpoint) {
		Cursor c = query(KEY_ENDPOINT + " = ?", new String[] { endpoint }, new String[] { KEY_NAME });
		if (c.getCount() == 0) {
			return null;
		}
		c.moveToFirst();
		String name = c.getString(0);
		c.close();
		return name;
	}
	
	public int setUser(String endpoint, long userID, String userName) {
		ContentValues values = new ContentValues();
		values.put(KEY_UID, userID);
		values.put(KEY_UNAME, userName);
		return mDatabase.update(TABLE_NAME, values, KEY_ENDPOINT + " = ?", new String[] { endpoint });
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
			KEY_ENDPOINT + " TEXT PRIMARY KEY, " +
			KEY_NAME + " TEXT, " +
			KEY_UID + " NUMERIC, " +
			KEY_UNAME + " TEXT)";
		
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
