package net.zekjur.davsync;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

public class NetworkReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("davsync", "network connectivity changed");

		if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
			return;

		ConnectivityManager cs = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo info = cs.getActiveNetworkInfo();
		if (info == null || !info.isConnected()) {
			Log.d("davsync", "_NOT_ connected anymore, not doing anything.");
			return;
		}

		SharedPreferences preferences = context.getSharedPreferences("net.zekjur.davsync_preferences",
				Context.MODE_PRIVATE);

		boolean syncPicturesOnWifiOnly = preferences.getBoolean("auto_sync_pictures_on_wifi_only", true);
		boolean syncVideosOnWifiOnly = preferences.getBoolean("auto_sync_videos_on_wifi_only", true);

		if ((syncPicturesOnWifiOnly || syncVideosOnWifiOnly) && !(ConnectivityManager.TYPE_WIFI == info.getType())) {
			Log.d("davsync", "Not on WIFI, not doing anything.");
			return;
		}

		Log.d("davsync", "Checking whether pictures/videos need to be synced");

		// XXX: It doesn’t really feel right to do this blockingly in a
		// BroadcastReceiver, but I was unable to find whether this is the right
		// way or whether there is a better one.
		
		ArrayList<MediaType> mediaTypes = new ArrayList<MediaType>();
		if (syncPicturesOnWifiOnly) {
			mediaTypes.add(MediaType.PICTURE);
		}
		if (syncVideosOnWifiOnly) {
			mediaTypes.add(MediaType.VIDEO);
		}

		DavSyncOpenHelper helper = new DavSyncOpenHelper(context);
		ArrayList<String> uris = helper.getQueuedUris(mediaTypes);
		for (String uri : uris) {
			Intent ulIntent = new Intent(context, UploadService.class);
			// evtl: mapintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			ulIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uri));
			context.startService(ulIntent);
		}
	}

}
