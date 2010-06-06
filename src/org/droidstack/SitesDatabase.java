package org.droidstack;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

public class SitesDatabase {
	
	public static final String KEY_SITE = BaseColumns._ID;
	public static final String KEY_UID = "uid";
	
	private static final String DATABASE_NAME = "stackexchange";
	private static final String TABLE_NAME = "sites";
	private static final int VERSION = 3;
	
	private final SitesOpenHelper mOpenHelper;
	private final SQLiteDatabase mDatabase;
	
	public SitesDatabase(Context context) {
		mOpenHelper = new SitesOpenHelper(context);
		mDatabase = mOpenHelper.getWritableDatabase();
	}
	
	public long addSite(String site, long userID) {
		ContentValues values = new ContentValues();
		values.put(KEY_SITE, site);
		values.put(KEY_UID, userID);
		
		long r = mDatabase.replace(TABLE_NAME, null, values);
		return r;
	}
	
	public int removeSite(String site) {
		return mDatabase.delete(TABLE_NAME, KEY_SITE + " = ?", new String[] { site });
	}
	
	public Cursor getSites() {
		return query(null, null, null);
	}
	
	public long getUserIdForSite(String site) {
		Cursor c = query(KEY_SITE + " = ?", new String[] { site }, new String[] { KEY_UID });
		if (c.getCount() == 0) {
			return 0;
		}
		c.moveToFirst();
		long userID = c.getLong(0);
		c.close();
		return userID;
	}
	
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(TABLE_NAME);
		Cursor cursor = builder.query(mDatabase, columns, selection, selectionArgs, null, null, null);
		return cursor;
	}
	
	private class SitesOpenHelper extends SQLiteOpenHelper {
		
		private static final String SITES_TABLE_CREATE =
			"CREATE TABLE " + TABLE_NAME + "(" + KEY_SITE + " TEXT PRIMARY KEY, " + KEY_UID + " NUMERIC)";
		
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
