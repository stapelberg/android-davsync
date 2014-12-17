package net.zekjur.davsync;

import android.app.NotificationManager;
import android.app.Notification.Builder;

public class ProgressBarListener
{
	private String tag = "";
	private Builder mBuilder = null;
	private NotificationManager mNotificationManager = null;
	
	public ProgressBarListener()
	{
		super();
	}
	
	public ProgressBarListener(String tag, NotificationManager mNotificationManager, Builder mBuilder)
	{
		this();
		this.tag = tag;
		this.mNotificationManager = mNotificationManager;
		this.mBuilder = mBuilder;
	}
	
	public void updateTransferred(int percent)
	{
		this.mBuilder.setProgress(100, percent, false);
		this.mNotificationManager.notify(tag, 0, mBuilder.build());
		System.out.println("Transferred: " + percent + "%");
	}
}
