package net.zekjur.davsync;

import java.util.ArrayList;
import java.util.List;

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
		db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uri STRING NOT NULL, not_before DATETIME, uploading BOOLEAN DEFAULT 0);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if(oldVersion == 1 && newVersion == 2) {
			db.execSQL("ALTER TABLE sync_queue ADD COLUMN mediatype INTEGER DEFAULT 0;");
		}
	}

	/*
	 * Returns an ArrayList of Uris which are queued and not already being
	 * uploaded. Also marks all of them as being uploaded so that duplicate
	 * network change events don’t upload the same Uris a lot of times.
	 */
	public ArrayList<String> getQueuedUris(List<MediaType> mediaTypes) {
		ArrayList<String> result = new ArrayList<String>();
		SQLiteDatabase database = getWritableDatabase();
		database.beginTransaction();
		try {
			Cursor cursor = database.rawQuery("SELECT uri FROM sync_queue WHERE NOT uploading AND mediatype IN (" + listToString(mediaTypes) + ")", null);
			if (cursor.moveToFirst()) {
				do {
					result.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}

			if (cursor != null && !cursor.isClosed())
				cursor.close();

			database.execSQL("UPDATE sync_queue SET uploading = 1 AND mediatype IN (" + listToString(mediaTypes) + ")");

			database.setTransactionSuccessful();
		} finally {
			database.endTransaction();
		}
		return result;
	}

	public void queueUri(Uri uri, MediaType mediaType) {
		SQLiteDatabase database = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("uri", uri.toString());
		values.put("mediatype", mapMediaTypeToInt(mediaType));
		database.insertOrThrow("sync_queue", null, values);
	}

	public void removeUriFromQueue(String uri) {
		SQLiteDatabase database = getWritableDatabase();
		database.delete("sync_queue", "uri = ?", new String[] { uri });
	}
	
	private int mapMediaTypeToInt(MediaType mediaType) {
		switch(mediaType) {
		case PICTURE:
			return 0;
		case VIDEO:
			return 1;
		default:
			return 0;
		}
	}
	
	private String listToString(List<MediaType> mediaTypes) {
		StringBuilder builder = new StringBuilder();
		
		for (MediaType mediaType : mediaTypes) {
			builder.append(mapMediaTypeToInt(mediaType)).append(",");
		}
		if (builder.length() > 0) {
			builder.deleteCharAt(builder.length()-1);
		}
		
		return builder.toString();
	}
}
