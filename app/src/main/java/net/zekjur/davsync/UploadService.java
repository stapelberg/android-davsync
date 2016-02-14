package net.zekjur.davsync;

import android.app.IntentService;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadService extends IntentService {

	private static class ProgressNotification {

		private static int NOTIFICATION_ID = 1;

		private Builder mBuilder;
		private NotificationManager mNotificationManager;

		public ProgressNotification(Context context, String filename) {

			mBuilder = new Notification.Builder(context);
			mNotificationManager = (NotificationManager) context
					.getSystemService(Context.NOTIFICATION_SERVICE);

			mBuilder.setContentTitle("Uploading to WebDAV server");
			mBuilder.setContentText(filename);
			mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
			mBuilder.setOngoing(true);
			mBuilder.setProgress(100, 0, false);
		}

		@SuppressWarnings("deprecation")
		public void show() {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
				mNotificationManager.notify(NOTIFICATION_ID, mBuilder.getNotification());
			}
			else {
				mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
			}
		}

		public void notifyError(String message) {
			mBuilder.setContentTitle("Error uploading to WebDAV server.");
			mBuilder.setContentText(message);
			mBuilder.setProgress(0, 0, false);
			mBuilder.setOngoing(false);
			mBuilder.setAutoCancel(true);

			show();
		}

		public void cancel() {
			mNotificationManager.cancel(NOTIFICATION_ID);
		}

		public void setProgress(int percent) {
			mBuilder.setProgress(100, percent, false);
			show();
		}
	}

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
		if (cursor == null || cursor.getCount() == 0)
			return null;
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		Uri filePathUri = Uri.parse(cursor.getString(column_index));
		cursor.close();
		return filePathUri.getLastPathSegment();
	}

	private void requeueingUri(Uri uri) {
		Log.d("davsync", "Queueing " + uri + "for later (Upload error)");
		DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
		helper.queueUri(uri);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
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

		final ProgressNotification progressNotification = new ProgressNotification(this, filename);
		progressNotification.show();

		try {

			if (!webdavUrl.endsWith("/")) {
				webdavUrl += "/";
			}

			URL url = new URL(webdavUrl + filename);
			HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

			httpURLConnection.setDoOutput(true);
			httpURLConnection.setRequestMethod("PUT");

			if (webdavUser != null && webdavPassword != null) {
				byte [] userAndPassword = (webdavUser + ":" + webdavPassword).getBytes();
				String credentials = Base64.encodeToString(userAndPassword, Base64.DEFAULT);
				httpURLConnection.setRequestProperty("Authorization", "Basic " + credentials);
			}

			CountingOutputStream.UploadListener listener =
					new CountingOutputStream.UploadListener() {

				@Override
				public void onChange(int percent) {
					progressNotification.setProgress(percent);
				}
			};

			ParcelFileDescriptor fd;
			InputStream stream;
			long fileSize;
			try {
				fd = cr.openFileDescriptor(uri, "r");
				assert fd != null; // to avoid Android Studio warning
				stream = cr.openInputStream(uri);
				assert stream != null; // to avoid Android Studio warning
				fileSize = fd.getStatSize();
			} catch (FileNotFoundException e1) {
				Log.d("davsyncs", "File not found", e1);
				return;
			}

			String strMaxFileSize = preferences.getString("auto_sync_max_filesize", null);
			int maxFileSize = strMaxFileSize != null ? Integer.parseInt(strMaxFileSize)
					: Integer.MAX_VALUE;
			if (fileSize >= maxFileSize * 1048576) {
				progressNotification.notifyError("File is too large");
				return;
			}

			httpURLConnection.setFixedLengthStreamingMode((int)fileSize);

			CountingOutputStream output = new CountingOutputStream(
					httpURLConnection.getOutputStream(), listener, fileSize);

			byte buffer [] = new byte [8192];
			int len;
			while ((len = stream.read(buffer)) != -1) {
				output.write(buffer, 0, len);
			}

			int responseCode = httpURLConnection.getResponseCode();
			// 201 means the file was created.
			// 200 and 204 mean it was stored but already existed.
			if (responseCode == HttpURLConnection.HTTP_CREATED
					|| responseCode == HttpURLConnection.HTTP_OK
					|| responseCode == HttpURLConnection.HTTP_NO_CONTENT) {

				progressNotification.cancel();
				return;
			}

			Log.d("davsyncs", "" + httpURLConnection.getResponseMessage());
			progressNotification.notifyError(filename + ": " + httpURLConnection.getResponseMessage());

		} catch (IOException e) {
			Log.d("davsyncs", "Error uploading to WebDAV server", e);
			progressNotification.notifyError(filename + ": " + e.getLocalizedMessage());
		}

		requeueingUri(uri);

	}
}
