package net.zekjur.davsync;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

//Handler that receives messages from the thread
class ServiceHandler extends Handler {
	private final WeakReference<UploadService> mService;

	ServiceHandler(Looper looper, UploadService service) {
		super(looper);
		mService = new WeakReference<UploadService>(service);
	}

	@Override
	public void handleMessage(Message msg) {
		Log.d("davsyncs", "handleMessage");

		HttpPut httpPut = new HttpPut("http://dav.zekjur.net/d/it-works.txt");

		StringEntity entity = null;
		try {
			entity = new StringEntity("foobar :-)");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		httpPut.setEntity(entity);

		DefaultHttpClient httpClient = new DefaultHttpClient();
		AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("michael", "hh0bFP0S");
		httpClient.getCredentialsProvider().setCredentials(authScope, credentials);
		try {
			HttpResponse response = httpClient.execute(httpPut);
			Log.d("davsyncs", "" + response.getStatusLine());
			// D/davsyncs( 3898): HTTP/1.1 201 Created
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Normally we would do some work here, like download a file.
		// For our sample, we just sleep for 5 seconds.
		// Sardine s = SardineFactory.begin("michael", "hh0bFP0S");
		// try {
		// // TODO: unauthorized is:
		// // W/System.err( 2362): de.aflx.sardine.impl.SardineException:
		// Unexpected response (401 Unauthorized)
		//
		// List<DavResource> list = s.list("http://dav.zekjur.net/");
		// Log.d("davsync", list.toString());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// Stop the service using the startId, so that we don't stop
		// the service in the middle of handling another job
		mService.get().stopSelf(msg.arg1);
	}
}

public class UploadService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	@Override
	public IBinder onBind(Intent intent) {
		// We don’t provide binding, so return null
		return null;
	}

	@Override
	public void onCreate() {
		// Start up the thread running the service. Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block. We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("UploadThread", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper, this);
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "Uploaddav Service done", Toast.LENGTH_SHORT).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "upload service starting", Toast.LENGTH_SHORT).show();

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the
		// job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_NOT_STICKY;
	}
}
