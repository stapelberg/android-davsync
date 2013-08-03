package net.zekjur.davsync;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.zekjur.davsync.CountingInputStreamEntity.UploadListener;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.app.Notification;
import android.app.Notification.Builder;
import android.util.Log;

public class UploadService extends IntentService {
	public UploadService() {
		super("UploadService");
	}

	/*
	 * Resolve a Uri like “content://media/external/images/media/9210” to an
	 * actual filename, like “IMG_20130304_181119.jpg”
	 */
	private String filenameFromUri(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
		if (cursor == null)
			return null;
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		Uri filePathUri = Uri.parse(cursor.getString(column_index));
		return filePathUri.getLastPathSegment().toString();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		Log.d("davsyncs", "Uploading " + uri.toString());

		SharedPreferences preferences = getSharedPreferences("net.zekjur.davsync_preferences", Context.MODE_PRIVATE);

		String webdavUrl = preferences.getString("webdav_url", null);
		String webdavUser = preferences.getString("webdav_user", null);
		String webdavPassword = preferences.getString("webdav_password", null);
		if (webdavUrl == null) {
			Log.d("davsyncs", "No WebDAV URL set up.");
			return;
		}

		ContentResolver cr = getContentResolver();

		String filename = this.filenameFromUri(uri);
		if (filename == null) {
			Log.d("davsyncs", "filenameFromUri returned null");
			return;
		}

		final Builder mBuilder = new Notification.Builder(this);
		mBuilder.setContentTitle("Uploading to WebDAV server");
		mBuilder.setContentText(filename);
		mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
		mBuilder.setOngoing(true);
		mBuilder.setProgress(100, 30, false);
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(uri.toString(), 0, mBuilder.build());

		HttpPut httpPut = new HttpPut(webdavUrl + filename);

		ParcelFileDescriptor fd;
		InputStream stream;
		try {
			fd = cr.openFileDescriptor(uri, "r");
			stream = cr.openInputStream(uri);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		CountingInputStreamEntity entity = new CountingInputStreamEntity(stream, fd.getStatSize());
		entity.setUploadListener(new UploadListener() {
			@Override
			public void onChange(int percent) {
				mBuilder.setProgress(100, percent, false);
				mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
			}
		});

		httpPut.setEntity(entity);

		DefaultHttpClient httpClient = new DefaultHttpClient();

		if (webdavUser != null && webdavPassword != null) {
			AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(webdavUser, webdavPassword);
			httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

			try {
				httpPut.addHeader(new BasicScheme().authenticate(credentials, httpPut));
			} catch (AuthenticationException e1) {
				e1.printStackTrace();
				return;
			}
		}

		try {
			HttpResponse response = httpClient.execute(httpPut);
			int status = response.getStatusLine().getStatusCode();
			// 201 means the file was created.
			// 200 and 204 mean it was stored but already existed.
			if (status == 201 || status == 200 || status == 204) {
				// The file was uploaded, so we remove the ongoing notification,
				// remove it from the queue and that’s it.
				mNotificationManager.cancel(uri.toString(), 0);
				DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
				helper.removeUriFromQueue(uri.toString());
				return;
			}
			Log.d("davsyncs", "" + response.getStatusLine());
			mBuilder.setContentText(filename + ": " + response.getStatusLine());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		} catch (IOException e) {
			e.printStackTrace();
			mBuilder.setContentText(filename + ": " + e.getLocalizedMessage());
		}

		// XXX: It would be good to provide an option to try again.
		// (or try it again automatically?)
		// XXX: possibly we should re-queue the images in the database
		mBuilder.setContentTitle("Error uploading to WebDAV server");
		mBuilder.setProgress(0, 0, false);
		mBuilder.setOngoing(false);
		mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
	}
}
