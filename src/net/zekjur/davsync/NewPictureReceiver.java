package net.zekjur.davsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

public class NewPictureReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("davsync", "received pic intent");
		if (!android.hardware.Camera.ACTION_NEW_PICTURE.equals(intent.getAction()))
			return;

		SharedPreferences preferences = context.getSharedPreferences("net.zekjur.davsync_preferences",
				Context.MODE_PRIVATE);

		if (!preferences.getBoolean("auto_sync_camera_pictures", true)) {
			Log.d("davsync", "automatic camera sync is disabled, ignoring");
			return;
		}

		boolean syncOnWifiOnly = preferences.getBoolean("auto_sync_on_wifi_only", true);

		Log.d("davsync", "New picture was taken");
		Uri uri = intent.getData();
		Log.d("davsync", "picture uri = " + uri);

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
			helper.queueUri(uri);
		}
	}

}
