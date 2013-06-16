package net.zekjur.davsync;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class DavSyncOpenHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "davsync";
	private static final int DATABASE_VERSION = 2;

	public DavSyncOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uri STRING NOT NULL, not_before DATETIME, uploading BOOLEAN DEFAULT 1);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion == 1 && newVersion == 2) {
			ArrayList<String> uris = getQueuedUris(db);
			db.execSQL("DROP TABLE sync_queue;");
			onCreate(db);
			for (String uri : uris) {
				queueUri(db, Uri.parse(uri));
			}
		}
	}

	public ArrayList<String> getQueuedUris() {
		return getQueuedUris(null);
	}
	
	/*
	 * Returns an ArrayList of Uris which are queued and not already being
	 * uploaded.
	 */
	public ArrayList<String> getQueuedUris(SQLiteDatabase db) {
		ArrayList<String> result = new ArrayList<String>();
		SQLiteDatabase database = db;
		if (db == null) {
			database = getReadableDatabase();
		}
		database.beginTransaction();
		try {
			Cursor cursor = database.rawQuery("SELECT uri FROM sync_queue", null);
			if (cursor.moveToFirst()) {
				do {
					result.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}

			if (cursor != null && !cursor.isClosed())
				cursor.close();

			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
			
			if(db == null) {
				database.close();
			}
		}
		return result;
	}
	
	public String getNextUri() {
		String result = null;
		SQLiteDatabase database = getReadableDatabase();
		database.beginTransaction();
		try {
			Cursor cursor = database.rawQuery("SELECT uri FROM sync_queue WHERE uploading=1 LIMIT 1", null);
			if (cursor.moveToFirst()) {
				result = cursor.getString(0);
			}

			if (cursor != null && !cursor.isClosed())
				cursor.close();

			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
			database.close();
		}
		return result;
	}
	
	public void setUploading(String uri, int uploadingValue) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("uploading", uploadingValue);
		database.update("sync_queue", values, "uri=?", new String[]{uri});
		database.close();
	}

	public void queueUri(Uri uri) {
		queueUri(null, uri);
	}
	
	public void queueUri(SQLiteDatabase db , Uri uri) {
		SQLiteDatabase database = db;
		if (db == null) {
			database = getWritableDatabase();
		}
		ContentValues values = new ContentValues();
		values.put("uri", uri.toString());
		database.insertOrThrow("sync_queue", null, values);
		if (db == null) {
			database.close();
		}
	}

	public void removeUriFromQueue(String uri) {
		SQLiteDatabase database = getWritableDatabase();
		database.delete("sync_queue", "uri = ?", new String[] { uri });
		database.close();
	}
}
