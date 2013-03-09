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
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
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
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		Uri filePathUri = Uri.parse(cursor.getString(column_index));
		return filePathUri.getLastPathSegment().toString();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		final Uri uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
		Log.d("davsyncs", "Uploading " + uri.toString());

		ContentResolver cr = getContentResolver();

		String filename = this.filenameFromUri(uri);

		final Builder mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle("Uploading to WebDAV server");
		mBuilder.setContentText(filename);
		mBuilder.setSmallIcon(android.R.drawable.ic_menu_upload);
		mBuilder.setOngoing(true);
		mBuilder.setProgress(100, 30, false);
		final NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(uri.toString(), 0, mBuilder.build());

		HttpPut httpPut = new HttpPut("http://dav.zekjur.net/d/" + filename);

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
			public void onChange(int percent) {
				mBuilder.setProgress(100, percent, false);
				mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
			}
		});

		httpPut.setEntity(entity);

		DefaultHttpClient httpClient = new DefaultHttpClient();
		AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("michael", "hh0bFP0S");
		httpClient.getCredentialsProvider().setCredentials(authScope, credentials);

		try {
			httpPut.addHeader(new BasicScheme().authenticate(credentials, httpPut));
		} catch (AuthenticationException e1) {
			e1.printStackTrace();
			return;
		}

		try {
			HttpResponse response = httpClient.execute(httpPut);
			if (response.getStatusLine().getStatusCode() == 201) {
				// The file was uploaded, so we remove the ongoing notification
				// and that’s it.
				mNotificationManager.cancel(uri.toString(), 0);
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
		mBuilder.setProgress(0, 0, false);
		mBuilder.setOngoing(false);
		mNotificationManager.notify(uri.toString(), 0, mBuilder.build());
	}
}
