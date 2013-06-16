package net.zekjur.davsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

		boolean syncOnWifiOnly = preferences.getBoolean("auto_sync_on_wifi_only", true);

		if (syncOnWifiOnly && !(ConnectivityManager.TYPE_WIFI == info.getType())) {
			Log.d("davsync", "Not on WIFI, not doing anything.");
			return;
		}

		Log.d("davsync", "Checking whether pictures need to be synced");

		Intent ulIntent = new Intent(context, UploadService.class);
		context.startService(ulIntent);
	}

}
