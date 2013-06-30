package net.zekjur.davsync;

import java.util.ArrayList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PendingUploadsActivity extends Activity {
	
	public final static String LOCAL_BROADCAST_ACTION = "media_uploaded";
	public final static String LOCAL_BROADCAST_MESSAGE = "uri";

	private PendingUploadsAdapter adapter;
	
	private class Item {
		public String uri;
		public boolean checked;
		
		public Item(String u, boolean c) {
			uri = u;
			checked = c;
		}
	}
	
	private ArrayList<Item> pendingUploads = new ArrayList<Item>();
	private BroadcastReceiver receiver = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pending_uploads);

		getActionBar().setTitle(R.string.title_activity_pending_uploads);
		
		adapter = new PendingUploadsAdapter();
		
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setEmptyView(findViewById(R.id.empty));
		listView.setAdapter(adapter);
	}
	

	@Override
	protected void onResume() {
		super.onResume();

		DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
		ArrayList<String> uris = new ArrayList<String>();
				
		for (String uri : helper.getQueuedUris()) {
			long id = -1;
			Cursor cursor = getContentResolver().query(Uri.parse(uri), new String[] {MediaStore.Images.Media._ID}, null, null, null);
			if (cursor != null) {
				int id_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
				if(cursor.moveToFirst()) {
					id = cursor.getLong(id_index);
				}
				cursor.close();
			}
			if(id == -1) {
				Log.d("davsync", "remove uri " + uri);
				helper.removeUriFromQueue(uri);
				uris.remove(uri);
			} else {
				uris.add(uri);
			}
		}
		
		for (String uri : uris) {
			pendingUploads.add(new Item(uri, true));
		}
		adapter.notifyDataSetChanged();
		
		receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                String uri = bundle.getString(LOCAL_BROADCAST_MESSAGE);
                
                for (int i = 0; i < pendingUploads.size(); i++) {
					if(pendingUploads.get(i).uri.equals(uri)) {
						pendingUploads.remove(i);
						adapter.notifyDataSetChanged();
						break;
					}
				}
            }
        };
        
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(LOCAL_BROADCAST_ACTION));
	}




	@Override
	protected void onPause() {
		super.onPause();
		if(receiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
			receiver = null;
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.pending_uploads, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_upload:
			DavSyncOpenHelper helper = new DavSyncOpenHelper(this);
			for (Item i : pendingUploads) {
				if(i.checked == true) {
					helper.setUploading(i.uri, 1);
				}
			}
			Intent ulIntent = new Intent(this, UploadService.class);
			startService(ulIntent);
			return true;
		case R.id.action_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public class PendingUploadsAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			return pendingUploads.size();
		}

		@Override
		public Object getItem(int position) {
			return pendingUploads.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final View view;
			if (convertView == null) {
				view = getLayoutInflater().inflate(R.layout.pending_upload_item, parent, false);
			} else {
				view = convertView;
			}
			
			String title = "";
			long id = -1;
			
			String[] projection = { MediaStore.Images.Media.TITLE, MediaStore.Images.Media._ID };
			Cursor cursor = getContentResolver().query(Uri.parse(pendingUploads.get(position).uri), projection, null, null, null);
			if (cursor != null) {
				int title_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE);
				int id_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
				if(cursor.moveToFirst()) {
					title = cursor.getString(title_index);
					id = cursor.getLong(id_index);
				}
				cursor.close();
			}
			
			TextView filename = (TextView) view.findViewById(R.id.filename);
			filename.setText(title);

			ImageView image = (ImageView) view.findViewById(R.id.thumbnail);
			
			String mediaType = pendingUploads.get(position).uri.split("/")[4];
			
			Bitmap bitmap = null;
			if (id != -1) {
				if (mediaType.equals("images")) {
					bitmap = MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
				} else {
					bitmap = MediaStore.Video.Thumbnails.getThumbnail(getContentResolver(), id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
				}
			}

			if(image.getTag() != null && image.getTag().equals(Boolean.TRUE)){ 
				Drawable d = image.getDrawable();
				if(d instanceof BitmapDrawable) {
					Bitmap oldBmp = ((BitmapDrawable) d).getBitmap();
					oldBmp.recycle();
				}
			}
			
			if(bitmap != null) {
				image.setImageBitmap(bitmap);
				image.setTag(Boolean.TRUE);
			} else {
				image.setImageResource(R.drawable.ic_launcher);
				image.setTag(Boolean.FALSE);
			}

			final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
			checkBox.setChecked(pendingUploads.get(position).checked);
			checkBox.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					pendingUploads.get(position).checked = checkBox.isChecked(); 
				}
			});
			
			return view;
		}
	}
}
