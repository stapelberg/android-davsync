package net.zekjur.davsync;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by santi on 6/01/16.
 */
class CountingOutputStream extends OutputStream {

	private UploadListener listener;
	private long length;
    private long counter = 0l;
    private int lastPercent = 0;
    private OutputStream outputStream;

    public CountingOutputStream(OutputStream outputStream, UploadListener listener, long length) {
        this.outputStream = outputStream;
		this.listener = listener;
		this.length = length;
    }

    @Override
    public void write(int oneByte) throws IOException {
        this.outputStream.write(oneByte);
        counter++;
        if (listener != null) {
            int percent = (int) ((counter * 100) / length);
            // NB: We need to call this only when the percentage actually
            // changed, otherwise updating the notification will churn
            // through memory far too quickly.
            if (lastPercent != percent) {
                listener.onChange(percent);
                lastPercent = percent;
            }
        }
    }

	public interface UploadListener {
		public void onChange(int percent);
	}
}
