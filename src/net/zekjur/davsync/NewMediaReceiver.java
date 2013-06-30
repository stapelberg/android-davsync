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
		Log.d("davsync", "received pic or video intent");

		boolean isNewPic = android.hardware.Camera.ACTION_NEW_PICTURE.equals(intent.getAction());
		boolean isNewVid = android.hardware.Camera.ACTION_NEW_VIDEO.equals(intent.getAction());

		if (!isNewPic && !isNewVid) return;

		SharedPreferences preferences = context.getSharedPreferences("net.zekjur.davsync_preferences",
				Context.MODE_PRIVATE);

		if (isNewPic && !preferences.getBoolean("auto_sync_camera_pictures", true)) {
			Log.d("davsync", "automatic camera picture sync is disabled, ignoring");
			return;
		}

		if (isNewVid && !preferences.getBoolean("auto_sync_camera_videos", true)) {
			Log.d("davsync", "automatic camera video sync is disabled, ignoring");
			return;
		}

		boolean syncOnWifiOnly = preferences.getBoolean("auto_sync_on_wifi_only", true);

		Log.d("davsync", "New picture was taken");
		Uri uri = intent.getData();
		Log.d("davsync", "picture uri = " + uri);

		DavSyncOpenHelper helper = new DavSyncOpenHelper(context);

		ConnectivityManager cs = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cs.getActiveNetworkInfo();
		if (info == null) {
			Log.d("davsync", "Queueing " + uri + "for later (no active network info)");
			// otherwise, queue the image for later
			helper.queueUri(uri);
			return;
		}

		// If we have WIFI connectivity, upload immediately
		boolean isWifi = info.isConnected() && (ConnectivityManager.TYPE_WIFI == info.getType());
		if (!syncOnWifiOnly || isWifi) {
			Log.d("davsync", "Trying to upload " + uri + " immediately (on WIFI)");
			helper.queueUri(uri);
			Intent ulIntent = new Intent(context, UploadService.class);
			context.startService(ulIntent);
		} else {
			Log.d("davsync", "Queueing " + uri + "for later (not on WIFI)");
			// otherwise, queue the image for later
			helper.queueUri(uri);
		}
	}

}
