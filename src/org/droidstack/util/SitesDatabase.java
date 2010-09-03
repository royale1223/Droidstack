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
	public static final String KEY_BOOKMARKED = "bookmarked";
	
	private static final String DATABASE_NAME = "stackexchange";
	private static final String TABLE_NAME = "sites";
	private static final int VERSION = 8;
	
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
		values.put(KEY_BOOKMARKED, 0);
		try {
			return mDatabase.insertOrThrow(TABLE_NAME, null, values);
		}
		catch (Exception e) {
			return -1;
		}
	}
	
	public int bookmarkSite(String endpoint) {
		ContentValues cv = new ContentValues(1);
		cv.put(KEY_BOOKMARKED, 1);
		return mDatabase.update(TABLE_NAME, cv, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public int removeBookmark(String endpoint) {
		ContentValues cv = new ContentValues(1);
		cv.put(KEY_BOOKMARKED, 0);
		return mDatabase.update(TABLE_NAME, cv, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public int removeSite(String endpoint) {
		return mDatabase.delete(TABLE_NAME, KEY_ENDPOINT + " = ?", new String[] { endpoint });
	}
	
	public Cursor getSites() {
		return query(null, null, null);
	}
	
	public Cursor getBookmarkedSites() {
		return query(KEY_BOOKMARKED + " = ?", new String[] { "1" }, null);
	}
	
	public Cursor getOtherSites() {
		return query(KEY_BOOKMARKED + " = ?", new String[] { "0" }, null);
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
			KEY_UNAME + " TEXT, " +
			KEY_BOOKMARKED + " INTEGER)";
		
		private static final String UPGRADE_7_TO_8 =
			"ALTER TABLE " + TABLE_NAME + " ADD COLUMN " +
			KEY_BOOKMARKED + " INTEGER";
		
		public SitesOpenHelper(Context context) {
			super(context, DATABASE_NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SITES_TABLE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 7 && newVersion == 8) {
				db.execSQL(UPGRADE_7_TO_8);
				ContentValues cv = new ContentValues(1);
				cv.put(KEY_BOOKMARKED, 1);
				db.update(TABLE_NAME, cv, null, null);
			}
			else {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
				onCreate(db);
			}
		}
	}
	
	public void dispose() {
		
		mDatabase.close();
		mOpenHelper.close();
		
	}
	
}
