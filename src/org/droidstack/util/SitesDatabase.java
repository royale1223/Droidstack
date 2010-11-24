package org.droidstack.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

public class SitesDatabase {

	public static final String KEY_ENDPOINT = BaseColumns._ID;
	public static final String KEY_NAME = "name";
	public static final String KEY_UID = "uid";
	public static final String KEY_REPUTATION = "reputation";
	public static final String KEY_UNAME = "user_name";
	// not used anymore, kept for backward-compatibility
	// should be removed in next version!
	public static final String KEY_BOOKMARKED = "bookmarked";
	
	private static final String DATABASE_NAME = "stackexchange";
	private static final String TABLE_NAME = "sites";
	private static final int VERSION = 11;
	
	private final SitesOpenHelper mOpenHelper;
	private final SQLiteDatabase mDatabase;
	
	public static String getEndpoint(Cursor c) {
		return c.getString(c.getColumnIndex(KEY_ENDPOINT));
	}
	public static String getName(Cursor c) {
		return c.getString(c.getColumnIndex(KEY_NAME));
	}
	public static int getReputation(Cursor c) {
		return c.getInt(c.getColumnIndex(KEY_REPUTATION));
	}
	public static String getUserName(Cursor c) {
		return c.getString(c.getColumnIndex(KEY_UNAME));
	}
	public static int getUserID(Cursor c) {
		return c.getInt(c.getColumnIndex(KEY_UID));
	}
	
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
		try {
			return mDatabase.insertOrThrow(TABLE_NAME, null, values);
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	public boolean exists(String endpoint) {
		Cursor c = mDatabase.query(TABLE_NAME, new String[] { "1" },
				KEY_ENDPOINT + " = ?", new String[] { endpoint }, null, null, null);
		boolean result = c.getCount() > 0;
		c.close();
		return result;
	}
	
	public int removeSite(String endpoint) {
		return mDatabase.delete(TABLE_NAME, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public Cursor getSites() {
		return query(null, null, null);
	}
	
	public int setUser(String endpoint, long userID, int reputation, String userName) {
		ContentValues values = new ContentValues(3);
		values.put(KEY_UID, userID);
		values.put(KEY_UNAME, userName);
		values.put(KEY_REPUTATION, reputation);
		return mDatabase.update(TABLE_NAME, values, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public int setReputation(String endpoint, int reputation) {
		ContentValues values = new ContentValues(1);
		values.put(KEY_REPUTATION, reputation);
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
			KEY_REPUTATION + " INTEGER, " +
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
			switch(oldVersion) {
			case 8:
				db.delete(TABLE_NAME, KEY_BOOKMARKED + " = ?", new String[] { "0" });
			case 9:
				db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_REPUTATION + " INTEGER");
				break;
			case 10:
				try {
					db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + KEY_REPUTATION + " INTEGER");
				}
				catch (Exception e) {
					// this is a bugfix
					Log.e(Const.TAG, "bugfix!");
				}
				break;
			default:
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
				onCreate(db);
				break;
			}
		}
	}
	
	public void close() {
		mOpenHelper.close();
		mDatabase.close();
	}
	
	@Override
	protected void finalize() throws Throwable {
		close();
	}
	
}
