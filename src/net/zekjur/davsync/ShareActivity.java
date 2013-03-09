package net.zekjur.davsync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;

public class ShareActivity extends Activity {

	/*
	 * Takes one or multiple images (see AndroidManifest.xml) and calls
	 * shareImageWithUri() on each one.
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);

		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		if (type == null)
			return;

		if (!intent.hasExtra(Intent.EXTRA_STREAM))
			return;

		if (Intent.ACTION_SEND.equals(action)) {
			Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
			shareImageWithUri(imageUri);
		} else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			ArrayList<Parcelable> list = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
			for (Parcelable p : list) {
				shareImageWithUri((Uri) p);
			}
		}
	}

	private void shareImageWithUri(Uri uri) {
		Log.d("davsync", "Sharing " + uri.toString());

		Intent ulIntent = new Intent(this, UploadService.class);
		ulIntent.putExtra(Intent.EXTRA_STREAM, uri);
		startService(ulIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.share, menu);
		return true;
	}

}
