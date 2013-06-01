package net.zekjur.davsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

public class NewMediaReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		MediaType mediaType = MediaType.PICTURE;
		
		Log.d("davsync", "received pic intent");
		if (!android.hardware.Camera.ACTION_NEW_PICTURE.equals(intent.getAction()) && !android.hardware.Camera.ACTION_NEW_VIDEO.equals(intent.getAction()))
			return;

		SharedPreferences preferences = context.getSharedPreferences("net.zekjur.davsync_preferences",
				Context.MODE_PRIVATE);

		if (android.hardware.Camera.ACTION_NEW_PICTURE.equals(intent.getAction())) {
			mediaType = MediaType.PICTURE; 
		} else if (android.hardware.Camera.ACTION_NEW_VIDEO.equals(intent.getAction())) {
			mediaType = MediaType.VIDEO;
		}
		
		if (mediaType == MediaType.PICTURE && !preferences.getBoolean("auto_sync_camera_pictures", true)) {
			Log.d("davsync", "automatic picture sync is disabled, ignoring");
			return;
		}
		
		if (mediaType == MediaType.VIDEO && !preferences.getBoolean("auto_sync_camera_videos", true)) {
			Log.d("davsync", "automatic video sync is disabled, ignoring");
			return;
		}
		

		boolean syncOnWifiOnly;
		if (mediaType == MediaType.PICTURE) {
			syncOnWifiOnly = preferences.getBoolean("auto_sync_pictures_on_wifi_only", true);
		} else {
			syncOnWifiOnly = preferences.getBoolean("auto_sync_videos_on_wifi_only", true);
		}

		Log.d("davsync", "New media was taken");
		Uri uri = intent.getData();
		Log.d("davsync", "media uri = " + uri);

		ConnectivityManager cs = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cs.getActiveNetworkInfo();

		// If we have WIFI connectivity, upload immediately
		boolean isWifi = info.isConnected() && (ConnectivityManager.TYPE_WIFI == info.getType());
		if (!syncOnWifiOnly || isWifi) {
			Log.d("davsync", "Trying to upload " + uri + " immediately (on WIFI)");
			Intent ulIntent = new Intent(context, UploadService.class);
			ulIntent.putExtra(Intent.EXTRA_STREAM, uri);
			context.startService(ulIntent);
		} else {
			Log.d("davsync", "Queueing " + uri + "for later (not on WIFI)");
			// otherwise, queue the image for later
			DavSyncOpenHelper helper = new DavSyncOpenHelper(context);
			helper.queueUri(uri, mediaType);
		}
	}

}
